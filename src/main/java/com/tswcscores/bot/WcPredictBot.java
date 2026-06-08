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
import com.tswcscores.service.impl.FootballDataService;
import com.tswcscores.service.impl.PredictionService;
import com.tswcscores.service.impl.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
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

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${predictions.window.hours}")
    private int predictionsWindow;

    public final String BOT_VERSION;

    public WcPredictBot(
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            PredictionService predictionService,
            MatchRepository matchRepository,
            FootballDataService footballDataService,
            ScoringService scoringService,
            @Autowired(required = false) BuildProperties buildProperties) {
        super(botToken);
        this.userService = userService;
        this.predictionService = predictionService;
        this.matchRepository = matchRepository;
        this.footballDataService = footballDataService;
        this.scoringService = scoringService;
        this.BOT_VERSION  = buildProperties != null ? buildProperties.getVersion() : "dev";
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        String text = msg.getText().trim();
        Long chatId = msg.getChatId();
        org.telegram.telegrambots.meta.api.objects.User tgUser = msg.getFrom();

        // Убираем @botname из команды если пришло из группы
        if (text.contains("@")) {
            text = text.substring(0, text.indexOf("@"));
        }

        String command = text.split("\\s+")[0].toLowerCase();
        log.debug("Command '{}' from user {} in chat {}", command, tgUser.getId(), chatId);

        switch (command) {
            case "/start", "/register" -> handleRegister(chatId, tgUser);
            case "/matches"            -> handleMatches(chatId, tgUser.getId());
            case "/predict"            -> handlePredict(chatId, tgUser.getId(), text);
            case "/mypredictions"      -> handleMyPredictions(chatId, tgUser.getId());
            case "/leaderboard"        -> handleLeaderboard(chatId);
            case "/help"               -> sendText(chatId, BotMessageBuilder.help(BOT_VERSION));
            case "/sync"               -> handleSync(chatId, tgUser.getId());
            case "/calcscore"          -> handleCalcScore(chatId, tgUser.getId());
            default -> {
                if (chatId.equals(tgUser.getId().longValue())) {
                    sendText(chatId, "Не понял команду. Используй /help");
                }
            }
        }
    }

    // --- Команды ---
    private void handleRegister(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        boolean exists = userService.findByTelegramId(tgUser.getId().longValue()).isPresent();
        User user = userService.registerOrGet(
                tgUser.getId().longValue(),
                tgUser.getUserName(),
                tgUser.getFirstName(),
                tgUser.getLastName()
        );
        sendText(chatId, exists ? BotMessageBuilder.alreadyRegistered() : BotMessageBuilder.welcome(user));
    }

    private void handleMatches(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            sendText(chatId, BotMessageBuilder.notRegistered());
            return;
        }
        List<Match> matches = matchRepository.findUpcoming(
                LocalDateTime.now(), LocalDateTime.now().plusHours(predictionsWindow));

        // Загружаем прогнозы пользователя чтобы отметить их на кнопках
        List<Prediction> userPredictions = predictionService.getUserPredictions(userOpt.get());

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(BotMessageBuilder.matchList(matches))
                .parseMode("HTML")
                .replyMarkup(matches.isEmpty() ? null : InlineKeyboardFactory.matchListKeyboard(matches, userPredictions))
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send matches to chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void handlePredict(Long chatId, Long telegramId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            sendText(chatId, BotMessageBuilder.notRegistered());
            return;
        }
        String[] parts = text.split("\\s+");
        if (parts.length != 4) {
            sendText(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>\nПример: <code>/predict 42 2 1</code>");
            return;
        }
        try {
            long matchId   = Long.parseLong(parts[1]);
            int homeScore  = Integer.parseInt(parts[2]);
            int awayScore  = Integer.parseInt(parts[3]);

            if (homeScore < 0 || awayScore < 0 || homeScore > 20 || awayScore > 20) {
                sendText(chatId, "❌ Некорректный счёт 😅");
                return;
            }
            Prediction prediction = predictionService.savePrediction(userOpt.get(), matchId, homeScore, awayScore);
            sendText(chatId, BotMessageBuilder.predictionSaved(prediction));

        } catch (NumberFormatException e) {
            sendText(chatId, "Формат: <code>/predict {id} {гол1} {гол2}</code>\nПример: <code>/predict 42 2 1</code>");
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

    private void handleLeaderboard(Long chatId) {
        sendText(chatId, BotMessageBuilder.leaderboard(userService.getLeaderboard()));
    }

    private void handleSync(Long chatId, Long telegramId) {
        sendText(chatId, BotMessageBuilder.syncStarted());
        try {
            footballDataService.syncMatches();
            sendText(chatId, BotMessageBuilder.syncDone());
        } catch (Exception e) {
            log.error("Manual sync failed", e);
            sendText(chatId, "❌ Ошибка синхронизации: " + e.getMessage());
        }
    }

    private void handleCalcScore(Long chatId, Long telegramId) {
        sendText(chatId, BotMessageBuilder.calcScoreStarted());
        try {
            scoringService.processFinishedMatches();
            sendText(chatId, BotMessageBuilder.calcScoreDone());
        } catch (Exception e) {
            log.error("Manual score calc failed", e);
            sendText(chatId, "❌ Ошибка подсчёта: " + e.getMessage());
        }
    }

    // --- Утилиты ---
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
