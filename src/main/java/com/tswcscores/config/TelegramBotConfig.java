package com.tswcscores.config;

import com.tswcscores.bot.WcPredictBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;

@Slf4j
@Configuration
public class TelegramBotConfig {

    private final WcPredictBot bot;
    private TelegramBotsLongPollingApplication botsApplication;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.admin-chat-id:0}")
    private long adminChatId;

    public TelegramBotConfig(WcPredictBot bot) {
        this.bot = bot;
    }

    @PostConstruct
    public void registerBot() {
        try {
            log.info("Registering Telegram bot: @{}", bot.getBotUsername());
            
            // Удаляем вебхук, если он был установлен ранее — это предотвращает конфликт с long polling
            try {
                bot.getTelegramClient().execute(new DeleteWebhook());
                log.info("Webhook deleted successfully");
            } catch (Exception e) {
                log.warn("Failed to delete webhook (non-critical): {}", e.getMessage());
            }
            
            // В 10.x регистрация через TelegramBotsLongPollingApplication
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, bot);
            log.info("✅ Telegram bot registered successfully, long polling started");
            bot.registerCommands();
            notifyAdmin("✅ <b>TS WC Scores " + bot.BOT_VERSION + "</b> запущен и готов к работе.");
        } catch (Exception e) {
            log.error("❌ Failed to register Telegram bot: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start without Telegram bot", e);
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Bot shutting down...");
        notifyAdmin("⚠️ <b>TS WC Scores</b> остановлен.");
        try {
            if (botsApplication != null) botsApplication.close();
        } catch (Exception e) {
            log.warn("Error closing bot application: {}", e.getMessage());
        }
    }

    private void notifyAdmin(String text) {
        if (adminChatId == 0) return;
        bot.sendNotification(adminChatId, text);
    }
}
