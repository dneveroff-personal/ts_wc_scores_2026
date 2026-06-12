package com.tswcscores.telegram;

import com.tswcscores.bot.WcPredictBot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Long polling — непрерывно запрашивает апдейты у Telegram и передаёт в WcPredictBot.
 * Без сторонних библиотек, без рекламы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdatePoller {

    private final TelegramBotClient telegramClient;
    private final WcPredictBot bot;

    private final AtomicLong offset = new AtomicLong(0);
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "telegram-poller");
                t.setDaemon(true);
                return t;
            });
    private volatile boolean running = false;

    @PostConstruct
    public void start() {
        running = true;
        // Небольшая задержка чтобы Spring-контекст успел полностью подняться
        executor.schedule(this::poll, 2, TimeUnit.SECONDS);
        log.info("✅ Telegram long polling started");
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
        log.info("Telegram poller stopped");
    }

    private void poll() {
        while (running) {
            try {
                // timeout=25 — Telegram держит соединение до 25 сек если нет апдейтов
                List<TelegramUpdate> updates = telegramClient.getUpdates(offset.get(), 25);
                for (TelegramUpdate update : updates) {
                    try {
                        log.debug("Update #{}: hasMsg={} hasCallback={} text={}",
                            update.getUpdateId(),
                            update.hasMessage(),
                            update.hasCallbackQuery(),
                            update.getMessage() != null ? update.getMessage().getText() : "null");
                        bot.handleUpdate(update);
                    } catch (Exception e) {
                        log.error("Error handling update {}: {}", update.getUpdateId(), e.getMessage(), e);
                    }
                    // Сдвигаем offset чтобы не получать один апдейт дважды
                    offset.set(update.getUpdateId() + 1);
                }
            } catch (Exception e) {
                log.error("Polling error: {}", e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
    }
}
