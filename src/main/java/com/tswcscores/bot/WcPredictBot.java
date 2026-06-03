package com.tswcscores.bot;

import com.tswcscores.bot.handler.BotMessageBuilder;
import com.tswcscores.bot.keyboard.InlineKeyboardFactory;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.exception.DeadlinePassedException;
import com.tswcscores.exception.MatchNotFoundException;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.service.impl.PredictionService;
import com.tswcscores.service.impl.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${telegram.bot.username}")
    private String botUsername;

    public WcPredictBot(
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            PredictionService predictionService,
            MatchRepository matchRepository) {
        super(botToken);
        this.userService = userService;
        this.predictionService = predictionService;
        this.matchRepository = matchRepository;
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

        switch (command) {
            case "/start", "/register" -> handleRegister(chatId, tgUser);
            case "/matches" -> handleMatches(chatId, tgUser.getId());
            case "/predict" -> handlePredict(chatId, tgUser.getId(), text);
            case "/mypredictions" -> handleMyPredictions(chatId, tgUser.getId());
            case "/leaderboard" -> handleLeaderboard(chatId);
            case "/help" -> sendText(chatId, BotMessageBuilder.predictHelp());
            default -> {
                // В личке отвечаем, в группе молчим на незнакомые команды
                if (chatId.equals(tgUser.getId().longValue())) {
                    sendText(chatId, "Не понял команду. Используй /help");
                }
            }
        }
    }

    private void handleRegister(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        boolean exists = userService.findByTelegramId(tgUser.getId().longValue()).isPresent();
        User user = userService.registerOrGet(
                tgUser.getId().longValue(),
                tgUser.getUserName(),
                tgUser.getFirstName(),
                tgUser.getLastName()
        );
        String response = exists
                ? BotMessageBuilder.alreadyRegistered()
                : BotMessageBuilder.welcome(user);
        sendText(chatId, response);
    }

    private void handleMatches(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            sendText(chatId, BotMessageBuilder.notRegistered());
            return;
        }
        List<Match> matches = matchRepository.findUpcoming(
                LocalDateTime.now(), LocalDateTime.now().plusHours(24));

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(BotMessageBuilder.matchList(matches))
                .parseMode("HTML")
                .replyMarkup(matches.isEmpty() ? null : InlineKeyboardFactory.matchListKeyboard(matches))
                .build();
        execute(message);
    }

    private void handlePredict(Long chatId, Long telegramId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            sendText(chatId, BotMessageBuilder.notRegistered());
            return;
        }

        String[] parts = text.split("\\s+");
        if (parts.length != 4) {
            sendText(chatId, BotMessageBuilder.predictHelp());
            return;
        }

        try {
            long matchId = Long.parseLong(parts[1]);
            int homeScore = Integer.parseInt(parts[2]);
            int awayScore = Integer.parseInt(parts[3]);

            if (homeScore < 0 || awayScore < 0 || homeScore > 20 || awayScore > 20) {
                sendText(chatId, "❌ Некорректный счёт. Введи нормальные числа 😅");
                return;
            }

            Prediction prediction = predictionService.savePrediction(
                    userOpt.get(), matchId, homeScore, awayScore);
            sendText(chatId, BotMessageBuilder.predictionSaved(prediction));

        } catch (NumberFormatException e) {
            sendText(chatId, BotMessageBuilder.predictHelp());
        } catch (DeadlinePassedException e) {
            sendText(chatId, "⛔ " + e.getMessage());
        } catch (MatchNotFoundException e) {
            sendText(chatId, "❓ Матч не найден. Проверь ID в /matches");
        }
    }

    private void handleMyPredictions(Long chatId, Long telegramId) {
        Optional<User> userOpt = userService.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            sendText(chatId, BotMessageBuilder.notRegistered());
            return;
        }
        List<Prediction> predictions = predictionService.getUserPredictions(userOpt.get());
        sendText(chatId, BotMessageBuilder.myPredictions(predictions));
    }

    private void handleLeaderboard(Long chatId) {
        List<User> leaders = userService.getLeaderboard();
        sendText(chatId, BotMessageBuilder.leaderboard(leaders));
    }

    public void sendNotification(Long telegramId, String htmlText) {
        sendText(telegramId, htmlText);
    }

    private void sendText(Long chatId, String text) {
        execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("HTML")
                .build());
    }

    private void execute(SendMessage msg) {
        try {
            execute((org.telegram.telegrambots.meta.api.methods.BotApiMethod<?>) msg);
        } catch (TelegramApiException e) {
            log.error("TelegramApiException: {}", e.getMessage());
        }
    }
}
