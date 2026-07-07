package com.tswcscores.config;

import com.tswcscores.bot.WcPredictBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

  private final WcPredictBot bot;

  @Value("${telegram.admin-chat-id:0}")
  private long adminChatId;

  @PostConstruct
  public void init() {
    log.info("Registering bot commands for @{}", bot.getBotUsername());
    bot.registerBotCommands();
    notifyAdmin("✅ <b>TS WC Scores " + bot.BOT_VERSION + "</b> запущен.");
  }

  private void notifyAdmin(String text) {
    if (adminChatId == 0) return;
    bot.sendNotification(adminChatId, text);
  }
}
