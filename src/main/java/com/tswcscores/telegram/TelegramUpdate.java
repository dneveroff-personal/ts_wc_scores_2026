package com.tswcscores.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Минимальное представление Telegram Update */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdate {

    @JsonProperty("update_id")
    private Long updateId;

    @JsonProperty("message")
    private TelegramMessage message;

    @JsonProperty("callback_query")
    private TelegramCallbackQuery callbackQuery;

    // Inline query — приходит когда пользователь нажимает switchInlineQueryCurrentChat кнопку.
    // Мы его не обрабатываем, но парсим чтобы не падало с ошибкой десериализации.
    @JsonProperty("inline_query")
    private Object inlineQuery;

    public boolean hasMessage() {
        return message != null 
            && message.getText() != null 
            && !message.getText().isBlank();
    }

    public boolean hasCallbackQuery() {
        return callbackQuery != null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramMessage {
        @JsonProperty("message_id") private Long messageId;
        @JsonProperty("from")       private TelegramUser from;
        @JsonProperty("chat")       private TelegramChat chat;
        @JsonProperty("text")       private String text;

        public Long getChatId() { return chat != null ? chat.getId() : null; }
        public boolean isGroup()  { return getChatId() != null && getChatId() < 0; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramCallbackQuery {
        @JsonProperty("id")      private String id;
        @JsonProperty("from")    private TelegramUser from;
        @JsonProperty("message") private TelegramMessage message;
        @JsonProperty("data")    private String data;

        public Long getChatId() { return message != null ? message.getChatId() : null; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramUser {
        @JsonProperty("id")         private Long id;
        @JsonProperty("username")   private String username;
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name")  private String lastName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramChat {
        @JsonProperty("id")    private Long id;
        @JsonProperty("type")  private String type;
        @JsonProperty("title") private String title;
    }
}
