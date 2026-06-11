package com.tswcscores.config;

import com.tswcscores.bot.WcPredictBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.List;

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
            registerCommands();
            notifyAdmin("✅ <b>TS WC Scores</b> запущен и готов к работе.");
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot start without Telegram bot", e);
        }
    }

    private void registerCommands() {
        try {
            bot.execute(SetMyCommands.builder()
                    .commands(List.of(
                            new BotCommand("register",      "Зарегистрироваться в игре"),
                            new BotCommand("matches",       "Матчи ближайших 24 часов"),
                            new BotCommand("predict",       "Сделать прогноз на матч"),
                            new BotCommand("mypredictions", "Мои прогнозы и очки"),
                            new BotCommand("leaderboard",   "Таблица лидеров группы"),
                            new BotCommand("sync",          "Синхронизация матчей с API"),
                            new BotCommand("calcscore",     "Подсчёт очков"),
                            new BotCommand("help",          "Справка по командам")
                    ))
                    .scope(new BotCommandScopeDefault())
                    .build());
            log.info("✅ Bot commands registered");
        } catch (TelegramApiException e) {
            log.warn("Failed to register bot commands: {}", e.getMessage());
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
