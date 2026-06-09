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
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class WcPredictBot extends TelegramLongPollingBot {

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
        super(botToken);
        this.userService = userService;
        this.predictionService = predictionService;
        this.matchRepository = matchRepository;
        this.footballDataService = footballDataService;
        this.scoringService = scoringService;
        this.groupService = groupService;
        this.BOT_VERSION  = buildProperties != null ? buildProperties.getVersion() : "dev";
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
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

        // Авторегистрация группы при любом сообщении боту из группы
        if (isGroup && msg.getChat() != null) {
            groupService.registerGroup(chatId, msg.getChat().getTitle());
            // Привязываем пользователя к группе при первом взаимодействии
            // (нужно для корректного начисления групповых очков)
            userService.findByTelegramId(tgUser.getId().longValue())
                    .ifPresent(u -> groupService.ensureUserInGroup(u, chatId));
        }

        // Telegram в группе вставляет "@botname" — может быть как суффикс ("/cmd@bot"),
        // так и префикс ("@bot /cmd счёт") при использовании switchInlineQueryCurrentChat.
        // Нормализуем оба варианта.
        if (text.startsWith("@")) {
            // "@bot /predict 1 2 1" → "/predict 1 2 1"
            int spaceIdx = text.indexOf(" ");
            text = spaceIdx >= 0 ? text.substring(spaceIdx).trim() : "";
        } else if (text.contains("@")) {
            // "/predict@bot 1 2 1" → "/predict 1 2 1"
            String[] tokens = text.split("\\s+");
            tokens[0] = tokens[0].substring(0, tokens[0].indexOf("@"));
            text = String.join(" ", tokens);
        }
        if (text.isEmpty()) return;

        String command = text.split("\\s+")[0].toLowerCase();
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
            // В группе: показываем toast-подсказку и дублируем в личку
            answerCallback(callback.getId(),
                    "Напиши в личку боту: /predict " + matchIdStr + " 2 1", false);
            // Шлём в личку подробную подсказку
            sendText(telegramId,
                    "⚽ Введи счёт — просто скопируй и замени цифры:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        } else {
            // В личке: toast + в том же чате
            answerCallback(callback.getId(), "Введи счёт 👇", false);
            sendText(chatId,
                    "⚽ Введи счёт — скопируй и замени <b>2 1</b> на свой прогноз:\n\n" +
                    "<code>/predict " + matchIdStr + " 2 1</code>");
        }
    }

    private void answerCallback(String callbackId, String text, boolean alert) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(alert)
                    .build());
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

        sendText(chatId, exists ? BotMessageBuilder.alreadyRegistered() : BotMessageBuilder.welcome(user));
    }

    private void handleMatches(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) { sendText(chatId, BotMessageBuilder.notRegistered()); return; }

        List<Match> matches = matchRepository.findUpcoming(
                LocalDateTime.now(), LocalDateTime.now().plusHours(predictionsWindow));
        List<Prediction> userPredictions = predictionService.getUserPredictions(userOpt.get());

        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(BotMessageBuilder.matchList(matches))
                    .parseMode("HTML")
                    .replyMarkup(matches.isEmpty() ? null :
                            InlineKeyboardFactory.matchListKeyboard(matches, userPredictions))
                    .build());
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

            // ensureUserInGroup вызывается в onUpdateReceived для всех команд из группы
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
            // Глобальный рейтинг
            sendText(chatId, BotMessageBuilder.leaderboard(userService.getLeaderboard()));
        } else {
            // Рейтинг группы
            var entries = groupService.getGroupLeaderboard(chatId);
            String title = null;
            var group = entries.isEmpty() ? null :
                    entries.get(0).getChatGroup();
            if (group != null) title = group.getTitle();
            sendText(chatId, BotMessageBuilder.groupLeaderboard(title, entries));
            // Подсказка про глобальный
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

    public void sendNotification(Long telegramId, String htmlText) {
        sendText(telegramId, htmlText);
    }

    private void sendText(Long chatId, String text) {
        try {
            execute((org.telegram.telegrambots.meta.api.methods.BotApiMethod<?>)
                    SendMessage.builder()
                            .chatId(chatId.toString())
                            .text(text)
                            .parseMode("HTML")
                            .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}: {}", chatId, e.getMessage());
        }
    }
}
