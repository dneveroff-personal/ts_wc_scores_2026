package com.tswcscores.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Свой HTTP-клиент к Telegram Bot API.
 * Никаких сторонних telegram-библиотек — только OkHttp + Jackson.
 */
@Slf4j
@Component
public class TelegramBotClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .readTimeout(35, TimeUnit.SECONDS)   // > 25s polling timeout
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public TelegramBotClient(@Value("${telegram.bot.token:}") String token) {
        this.baseUrl = "https://api.telegram.org/bot" + token;
        log.info("TelegramBotClient initialized with baseUrl: {}", baseUrl);
    }

    // For tests
    TelegramBotClient(String token, String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Fallback for Spring when @Value cannot be resolved
    public TelegramBotClient() {
        this.baseUrl = "";
    }

    // --- Отправка сообщений ---

    public void sendMessage(long chatId, String text) {
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML"
        ));
    }

    public void sendMessageWithInlineKeyboard(long chatId, String text, List<List<Map<String, String>>> keyboard) {
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML",
                "reply_markup", Map.of("inline_keyboard", keyboard)
        ));
    }

    public void sendMessageWithReplyKeyboard(long chatId, String text, List<List<String>> buttonRows) {
        List<List<Map<String, String>>> keyboard = buttonRows.stream()
                .map(row -> row.stream()
                        .map(label -> Map.of("text", label))
                        .toList())
                .toList();
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML",
                "reply_markup", Map.of(
                        "keyboard", keyboard,
                        "resize_keyboard", true,
                        "is_persistent", true
                )
        ));
    }

    public void sendMessageWithForceReplyAndPlaceholder(long chatId, String text, String placeholder) {
        post("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML",
                "reply_markup", Map.of(
                        "force_reply", true,
                        "selective", false,
                        "input_field_placeholder", placeholder
                )
        ));
    }

    public void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        post("answerCallbackQuery", Map.of(
                "callback_query_id", callbackQueryId,
                "text", text,
                "show_alert", showAlert
        ));
    }

    public void setMyCommands(List<Map<String, String>> commands) {
        post("setMyCommands", Map.of("commands", commands));
    }

    // --- Long Polling ---

    public List<TelegramUpdate> getUpdates(long offset, int timeout) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.warn("getUpdates skipped: baseUrl is empty (telegram.bot.token not configured)");
            return List.of();
        }
        try {
            String url = baseUrl + "/getUpdates?offset=" + offset
                    + "&timeout=" + timeout + "&limit=100";
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = http.newCall(req).execute()) {
                String body = resp.body().string();
                var node = mapper.readTree(body);
                if (!node.path("ok").asBoolean()) {
                    log.warn("getUpdates not ok: {}", body);
                    return List.of();
                }
                return mapper.readerForListOf(TelegramUpdate.class)
                        .readValue(node.path("result"));
            }
        } catch (Exception e) {
            log.error("getUpdates error: {}", e.getMessage());
            return List.of();
        }
    }

    // --- Приватный метод отправки ---

    private void post(String method, Object payload) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            log.warn("Telegram {} skipped: baseUrl is empty (telegram.bot.token not configured)", method);
            return;
        }
        try {
            String json = mapper.writeValueAsString(payload);
            Request req = new Request.Builder()
                    .url(baseUrl + "/" + method)
                    .post(RequestBody.create(json, JSON))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("Telegram {} failed: {} {}", method, resp.code(), resp.body().string());
                }
            }
        } catch (Exception e) {
            log.error("Telegram {} error: {}", method, e.getMessage());
        }
    }
}
