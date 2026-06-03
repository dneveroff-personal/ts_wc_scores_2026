package com.tswcscores.config;

import com.tswcscores.bot.WcPredictBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class TelegramBotConfig {

    private final WcPredictBot bot;

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
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start without Telegram bot", e);
        }
    }
}
