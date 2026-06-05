package com.tswcscores.config;

import com.tswcscores.bot.WcPredictBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class TelegramBotConfig {

    private final WcPredictBot bot;

    // ID чата/пользователя куда слать системные уведомления (твой личный Telegram ID)
    @Value("${telegram.admin-chat-id:0}")
    private long adminChatId;

    public TelegramBotConfig(WcPredictBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void registerBot() {
        try {
            log.info("Registering Telegram bot: @{}", bot.getBotUsername());
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);
            log.info("✅ Telegram bot registered successfully, long polling started");
            notifyAdmin("✅ <b>TS WC Scores</b> запущен и готов к работе.");
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start without Telegram bot", e);
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Bot shutting down...");
        notifyAdmin("⚠️ <b>TS WC Scores</b> остановлен.");
    }

    private void notifyAdmin(String text) {
        if (adminChatId == 0) return;
        bot.sendNotification(adminChatId, text);
    }
}
