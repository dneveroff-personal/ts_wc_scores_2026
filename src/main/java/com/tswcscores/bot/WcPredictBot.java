package com.tswcscores.bot;

import com.tswcscores.bot.handler.BotMessageBuilder;
import com.tswcscores.bot.keyboard.InlineKeyboardFactory;
import com.tswcscores.entity.*;
import com.tswcscores.entity.User;
import com.tswcscores.exception.DeadlinePassedException;
import com.tswcscores.exception.MatchNotFoundException;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.service.ScoringService;
import com.tswcscores.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class WcPredictBot implements LongPollingSingleThreadUpdateConsumer {

    // В 10.x отправка идёт через TelegramClient, а не через super.execute()
    private final TelegramClient telegramClient;

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
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            PredictionService predictionService,
            MatchRepository matchRepository,
            FootballDataService footballDataService,
            ScoringService scoringService,
            GroupService groupService,
            @Autowired(required = false) BuildProperties buildProperties) {
        // OkHttpTelegramClient — стандартная реализация TelegramClient в 10.x
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.userService = userService;
        this.predictionService = predictionService;
        this.matchRepository = matchRepository;
        this.footballDataService = footballDataService;
        this.scoringService = scoringService;
        this.groupService = groupService;
        this.BOT_VERSION = buildProperties != null ? buildProperties.getVersion() : "dev";
    }

    public TelegramClient getTelegramClient() {
        return telegramClient;
    }

    public String getBotUsername() {
        return botUsername;
    }

    // --- Точка входа для всех апдейтов ---
    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        String text = msg.getText().trim();
        Long chatId = msg.getChatId();
        org.telegram.telegrambots.meta.api.objects.User tgUser = msg.getFrom();
        boolean isGroup = chatId < 0;

        // Авторегистрация группы + привязка пользователя
        if (isGroup && msg.getChat() != null) {
            groupService.registerGroup(chatId, msg.getChat().getTitle());
            userService.findByTelegramId(tgUser.getId().longValue())
                    .ifPresent(u -> groupService.ensureUserInGroup(u, chatId));
        }

        // Нормализуем команду: убираем @botname в обоих форматах
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
        // Для ReplyKeyboard кнопок: приводим весь текст к нижнему регистру
        if (text.contains(" ")) {
            command = text.toLowerCase();
        }
        log.debug("Command '{}' from user {} in chat {}", command, tgUser.getId(), chatId);

        switch (command) {
            case "/start", "/register" -> handleRegister(chatId, tgUser);
            case "/matches"            -> handleMatches(chatId, tgUser.getId());
            case "/predict"            -> handlePredict(chatId, tgUser.getId(), text);
            case "/mypredictions"      -> handleMyPredictions(chatId, tgUser.getId());
            case "/leaderboard"        -> handleLeaderboard(chatId, text);
            case "/help"               -> sendText(chatId, BotMessageBuilder.help(BOT_VERSION));
            case "/sync"               -> handleSync(chatId);
            case "/calcscore"          -> handleCalcScore(chatId);
            // Текстовые кнопки ReplyKeyboard
            case "⚽ matches"         -> handleMatches(chatId, tgUser.getId());
            case "📋 my predictions" -> handleMyPredictions(chatId, tgUser.getId());
            case "🏆 leaderboard"       -> handleLeaderboard(chatId, text);
            case "❓ help"        -> sendText(chatId, BotMessageBuilder.help(BOT_VERSION));
            default -> {
                if (!isGroup) sendText(chatId, "Не понял команду. Используй /help");
            }
        }
    }

    // --- Callback от inline-кнопок ---
    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        Long chatId = callback.getMessage().getChatId();
        Long telegramId = callback.getFrom().getId();
        boolean isGroup = chatId < 0;

        if (!data.startsWith(InlineKeyboardFactory.PREDICT_PREFIX)) return;

        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            answerCallback(callback.getId(), "Сначала зарегистрируйся: /register", true);
            return;
        }

        String matchIdStr = data.substring(InlineKeyboardFactory.PREDICT_PREFIX.length());

        if (isGroup) {
            answerCallback(callback.getId(), "Напиши в личку боту: /predict " + matchIdStr + " 2 1", false);
            sendText(telegramId,
                    "⚽ Введи счёт — скопируй и замени цифры:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        } else {
            answerCallback(callback.getId(), "Введи счёт 👇", false);
            sendText(chatId,
                    "⚽ Введи счёт — замени <b>2 1</b> на свой прогноз:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        }
    }

    private void answerCallback(String callbackId, String text, boolean alert) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
            answer.setText(text);
            answer.setShowAlert(alert);
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback: {}", e.getMessage());
        }
    }

    // --- Команды ---

    private void handleRegister(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        boolean exists = userService.findByTelegramId(tgUser.getId().longValue()).isPresent();
        User user = userService.registerOrGet(
                tgUser.getId().longValue(), tgUser.getUserName(),
                tgUser.getFirstName(), tgUser.getLastName());

        // Если регистрируется из группы — привязываем к ней
        if (chatId < 0) {
            groupService.registerGroup(chatId, null); // уже должна быть, просто ensure
        }

        String replyText = exists ? BotMessageBuilder.alreadyRegistered() : BotMessageBuilder.welcome(user);
        sendTextWithKeyboard(chatId, replyText, InlineKeyboardFactory.mainMenuKeyboard());
    }

    private void handleMatches(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { sendText(chatId, BotMessageBuilder.notRegistered()); return; }

        List<Match> matches = matchRepository.findUpcoming(
                LocalDateTime.now(), LocalDateTime.now().plusHours(predictionsWindow));
        List<Prediction> userPredictions = predictionService.getUserPredictions(userOpt.get());

        try {
            SendMessage msg = new SendMessage(chatId.toString(), BotMessageBuilder.matchList(matches));
            msg.setParseMode("HTML");
            if (!matches.isEmpty()) {
                msg.setReplyMarkup(InlineKeyboardFactory.matchListKeyboard(matches, userPredictions));
            }
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send matches: {}", e.getMessage());
        }
    }

    private void handlePredict(Long chatId, Long telegramId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { sendText(chatId, BotMessageBuilder.notRegistered()); return; }

        String[] parts = text.split("\\s+");
        if (parts.length != 4) {
            sendText(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>\nПример: <code>/predict 42 2 1</code>");
            return;
        }
        try {
            long matchId  = Long.parseLong(parts[1]);
            int homeScore = Integer.parseInt(parts[2]);
            int awayScore = Integer.parseInt(parts[3]);
            if (homeScore < 0 || awayScore < 0 || homeScore > 20 || awayScore > 20) {
                sendText(chatId, "❌ Некорректный счёт 😅"); return;
            }
            Prediction p = predictionService.savePrediction(userOpt.get(), matchId, homeScore, awayScore);
            sendText(chatId, BotMessageBuilder.predictionSaved(p));
        } catch (NumberFormatException e) {
            sendText(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>");
        } catch (DeadlinePassedException e) {
            sendText(chatId, "⛔ " + e.getMessage());
        } catch (MatchNotFoundException e) {
            sendText(chatId, "❓ Матч не найден. Проверь ID в /matches");
        }
    }

    private void handleMyPredictions(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { sendText(chatId, BotMessageBuilder.notRegistered()); return; }
        sendText(chatId, BotMessageBuilder.myPredictions(predictionService.getUserPredictions(userOpt.get())));
    }

    private void handleLeaderboard(Long chatId, String fullText) {
        boolean global = fullText.toLowerCase().contains("global");
        boolean isGroup = chatId < 0;

        if (global || !isGroup) {
            sendText(chatId, BotMessageBuilder.leaderboard(userService.getLeaderboard()));
        } else {
            var entries = groupService.getGroupLeaderboard(chatId);
            String title = entries.isEmpty() ? null : entries.get(0).getChatGroup().getTitle();
            sendText(chatId, BotMessageBuilder.groupLeaderboard(title, entries));
            sendText(chatId, "Для общего рейтинга: /leaderboard global");
        }
    }

    private void handleSync(Long chatId) {
        sendText(chatId, BotMessageBuilder.syncStarted());
        try {
            footballDataService.syncMatches();
            sendText(chatId, BotMessageBuilder.syncDone());
        } catch (Exception e) {
            log.error("Manual sync failed", e);
            sendText(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleCalcScore(Long chatId) {
        sendText(chatId, BotMessageBuilder.calcScoreStarted());
        try {
            scoringService.processFinishedMatches();
            sendText(chatId, BotMessageBuilder.calcScoreDone());
        } catch (Exception e) {
            log.error("Manual score calc failed", e);
            sendText(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    // --- Утилиты ---
    public void sendNotification(Long telegramId, String htmlText) {
        sendText(telegramId, htmlText);
    }

    public void registerCommands() {
        try {
            List<BotCommand> commands = List.of(
                new BotCommand("register",      "Зарегистрироваться в игре"),
                new BotCommand("matches",       "Матчи ближайших 24 часов"),
                new BotCommand("predict",       "Сделать прогноз на матч"),
                new BotCommand("mypredictions", "Мои прогнозы и очки"),
                new BotCommand("leaderboard",   "Таблица лидеров группы"),
                new BotCommand("sync",          "Синхронизация матчей с API"),
                new BotCommand("calcscore",     "Подсчёт очков"),
                new BotCommand("help",          "Справка по командам")
            );
            SetMyCommands setCommands = new SetMyCommands(commands);
            setCommands.setScope(new BotCommandScopeDefault());
            telegramClient.execute(setCommands);
        } catch (TelegramApiException e) {
            log.warn("Failed to register bot commands: {}", e.getMessage());
        }
    }

    private void sendTextWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.setParseMode("HTML");
            msg.setReplyMarkup(keyboard);
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send message with keyboard to chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.setParseMode("HTML");
            telegramClient.execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}: {}", chatId, e.getMessage());
        }
    }
}
