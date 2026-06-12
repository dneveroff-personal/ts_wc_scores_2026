package com.tswcscores.bot;

import com.tswcscores.bot.handler.BotMessageBuilder;
import com.tswcscores.bot.keyboard.InlineKeyboardFactory;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.exception.DeadlinePassedException;
import com.tswcscores.exception.MatchNotFoundException;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.service.ScoringService;
import com.tswcscores.service.impl.*;
import com.tswcscores.telegram.TelegramBotClient;
import com.tswcscores.telegram.TelegramUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class WcPredictBot {

    private final TelegramBotClient telegram;
    private final UserService userService;
    private final PredictionService predictionService;
    private final MatchRepository matchRepository;
    private final FootballDataService footballDataService;
    private final ScoringService scoringService;
    private final GroupService groupService;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${predictions.window.hours:24}")
    private int predictionsWindow;

    public final String BOT_VERSION;

    public WcPredictBot(
            TelegramBotClient telegram,
            UserService userService,
            PredictionService predictionService,
            MatchRepository matchRepository,
            FootballDataService footballDataService,
            ScoringService scoringService,
            GroupService groupService,
            @Autowired(required = false) BuildProperties buildProperties) {
        this.telegram = telegram;
        this.userService = userService;
        this.predictionService = predictionService;
        this.matchRepository = matchRepository;
        this.footballDataService = footballDataService;
        this.scoringService = scoringService;
        this.groupService = groupService;
        this.BOT_VERSION = buildProperties != null ? buildProperties.getVersion() : "dev";
    }

    public String getBotUsername() { return botUsername; }

    // --- Точка входа ---

    public void handleUpdate(TelegramUpdate update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage()) return;

        TelegramUpdate.TelegramMessage msg = update.getMessage();
        String text = msg.getText().trim();
        Long chatId = msg.getChatId();
        TelegramUpdate.TelegramUser tgUser = msg.getFrom();
        boolean isGroup = msg.isGroup();

        if (isGroup && msg.getChat() != null) {
            groupService.registerGroup(chatId, msg.getChat().getTitle());
            userService.findByTelegramId(tgUser.getId())
                    .ifPresent(u -> groupService.ensureUserInGroup(u, chatId));
        }

        // Нормализуем: убираем @botname
        if (text.startsWith("@")) {
            int spaceIdx = text.indexOf(" ");
            text = spaceIdx >= 0 ? text.substring(spaceIdx).trim() : "";
        } else if (text.contains("@")) {
            String[] tokens = text.split("\\s+");
            tokens[0] = tokens[0].substring(0, tokens[0].indexOf("@"));
            text = String.join(" ", tokens);
        }
        if (text.isEmpty()) return;

        String command = text.split("\\s+")[0].toLowerCase();
        log.debug("Command '{}' from {} in chat {}", command, tgUser.getId(), chatId);

        switch (command) {
            case "/start", "/register" -> handleRegister(chatId, tgUser);
            case "/matches"            -> handleMatches(chatId, tgUser.getId());
            case "/predict"            -> handlePredict(chatId, tgUser.getId(), text);
            case "/mypredictions"      -> handleMyPredictions(chatId, tgUser.getId());
            case "/leaderboard"        -> handleLeaderboard(chatId, text);
            case "/help"               -> telegram.sendMessage(chatId, BotMessageBuilder.help(BOT_VERSION));
            case "/sync"               -> handleSync(chatId);
            case "/calcscore"          -> handleCalcScore(chatId);
            // Кнопки ReplyKeyboard
            case "⚽ матчи"         -> handleMatches(chatId, tgUser.getId());
            case "📋 мои прогнозы" -> handleMyPredictions(chatId, tgUser.getId());
            case "🏆 рейтинг"       -> handleLeaderboard(chatId, text);
            case "❓ помощь"        -> telegram.sendMessage(chatId, BotMessageBuilder.help(BOT_VERSION));
            default -> { if (!isGroup) telegram.sendMessage(chatId, "Не понял команду. Используй /help"); }
        }
    }

    // --- Callback ---

    private void handleCallback(TelegramUpdate.TelegramCallbackQuery callback) {
        String data = callback.getData();
        Long chatId = callback.getChatId();
        Long telegramId = callback.getFrom().getId();
        boolean isGroup = chatId != null && chatId < 0;

        if (data == null || !data.startsWith(InlineKeyboardFactory.PREDICT_PREFIX)) return;

        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            telegram.answerCallbackQuery(callback.getId(), "Сначала зарегистрируйся: /register", true);
            return;
        }

        String matchIdStr = data.substring(InlineKeyboardFactory.PREDICT_PREFIX.length());
        if (isGroup) {
            telegram.answerCallbackQuery(callback.getId(),
                    "Напиши боту в личку: /predict " + matchIdStr + " 2 1", false);
            telegram.sendMessage(telegramId,
                    "⚽ Скопируй и замени цифры на свой прогноз:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        } else {
            telegram.answerCallbackQuery(callback.getId(), "Введи счёт 👇", false);
            telegram.sendMessage(chatId,
                    "⚽ Замени <b>2 1</b> на свой прогноз:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        }
    }

    // --- Команды ---

    private void handleRegister(Long chatId, TelegramUpdate.TelegramUser tgUser) {
        boolean exists = userService.findByTelegramId(tgUser.getId()).isPresent();
        User user = userService.registerOrGet(
                tgUser.getId(), tgUser.getUsername(), tgUser.getFirstName(), tgUser.getLastName());
        String text = exists ? BotMessageBuilder.alreadyRegistered() : BotMessageBuilder.welcome(user);
        telegram.sendMessageWithReplyKeyboard(chatId, text, InlineKeyboardFactory.mainMenuButtons());
    }

    private void handleMatches(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { telegram.sendMessage(chatId, BotMessageBuilder.notRegistered()); return; }

        List<Match> matches = matchRepository.findUpcoming(
                LocalDateTime.now(), LocalDateTime.now().plusHours(predictionsWindow));
        List<Prediction> predictions = predictionService.getUserPredictions(userOpt.get());

        if (matches.isEmpty()) {
            telegram.sendMessage(chatId, BotMessageBuilder.matchList(matches));
        } else {
            telegram.sendMessageWithInlineKeyboard(chatId,
                    BotMessageBuilder.matchList(matches),
                    InlineKeyboardFactory.matchListKeyboard(matches, predictions));
        }
    }

    private void handlePredict(Long chatId, Long telegramId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { telegram.sendMessage(chatId, BotMessageBuilder.notRegistered()); return; }

        String[] parts = text.split("\\s+");
        if (parts.length != 4) {
            telegram.sendMessage(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>\nПример: <code>/predict 42 2 1</code>");
            return;
        }
        try {
            long matchId  = Long.parseLong(parts[1]);
            int homeScore = Integer.parseInt(parts[2]);
            int awayScore = Integer.parseInt(parts[3]);
            if (homeScore < 0 || awayScore < 0 || homeScore > 20 || awayScore > 20) {
                telegram.sendMessage(chatId, "❌ Некорректный счёт 😅"); return;
            }
            Prediction p = predictionService.savePrediction(userOpt.get(), matchId, homeScore, awayScore);
            telegram.sendMessage(chatId, BotMessageBuilder.predictionSaved(p));
        } catch (NumberFormatException e) {
            telegram.sendMessage(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>");
        } catch (DeadlinePassedException e) {
            telegram.sendMessage(chatId, "⛔ " + e.getMessage());
        } catch (MatchNotFoundException e) {
            telegram.sendMessage(chatId, "❓ Матч не найден. Проверь ID в /matches");
        }
    }

    private void handleMyPredictions(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { telegram.sendMessage(chatId, BotMessageBuilder.notRegistered()); return; }
        telegram.sendMessage(chatId,
                BotMessageBuilder.myPredictions(predictionService.getUserPredictions(userOpt.get())));
    }

    private void handleLeaderboard(Long chatId, String fullText) {
        boolean global = fullText.toLowerCase().contains("global");
        boolean isGroup = chatId < 0;
        if (global || !isGroup) {
            telegram.sendMessage(chatId, BotMessageBuilder.leaderboard(userService.getLeaderboard()));
        } else {
            var entries = groupService.getGroupLeaderboard(chatId);
            String title = entries.isEmpty() ? null : entries.get(0).getChatGroup().getTitle();
            telegram.sendMessage(chatId, BotMessageBuilder.groupLeaderboard(title, entries));
        }
    }

    private void handleSync(Long chatId) {
        telegram.sendMessage(chatId, BotMessageBuilder.syncStarted());
        try { footballDataService.syncMatches(); telegram.sendMessage(chatId, BotMessageBuilder.syncDone()); }
        catch (Exception e) { telegram.sendMessage(chatId, "❌ Ошибка: " + e.getMessage()); }
    }

    private void handleCalcScore(Long chatId) {
        telegram.sendMessage(chatId, BotMessageBuilder.calcScoreStarted());
        try { scoringService.processFinishedMatches(); telegram.sendMessage(chatId, BotMessageBuilder.calcScoreDone()); }
        catch (Exception e) { telegram.sendMessage(chatId, "❌ Ошибка: " + e.getMessage()); }
    }

    public void sendNotification(Long telegramId, String htmlText) {
        telegram.sendMessage(telegramId, htmlText);
    }

    public void registerBotCommands() {
        telegram.setMyCommands(List.of(
                Map.of("command", "register",      "description", "Зарегистрироваться в игре"),
                Map.of("command", "matches",       "description", "Матчи ближайших 24 часов"),
                Map.of("command", "predict",       "description", "Сделать прогноз на матч"),
                Map.of("command", "mypredictions", "description", "Мои прогнозы и очки"),
                Map.of("command", "leaderboard",   "description", "Таблица лидеров группы"),
                Map.of("command", "sync",          "description", "Синхронизация матчей с API"),
                Map.of("command", "calcscore",     "description", "Подсчёт очков"),
                Map.of("command", "help",          "description", "Справка по командам")
        ));
    }
}
