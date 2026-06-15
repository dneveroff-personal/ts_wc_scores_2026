package com.tswcscores.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendMessageWithForceReplyAndPlaceholderPostsExpectedPayload() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        server.start();
        try {
            TelegramBotClient client = new TelegramBotClient("test-token", server.url("/bot-test-token").toString());

            client.sendMessageWithForceReplyAndPlaceholder(123L, "\u2800", "/predict 42 ");

            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath()).isEqualTo("/bot-test-token/sendMessage");

            JsonNode payload = mapper.readTree(request.getBody().readUtf8());
            assertThat(payload.path("chat_id").asLong()).isEqualTo(123L);
            assertThat(payload.path("text").asText()).isEqualTo("\u2800");
            assertThat(payload.path("parse_mode").asText()).isEqualTo("HTML");

            JsonNode replyMarkup = payload.path("reply_markup");
            assertThat(replyMarkup.path("force_reply").asBoolean()).isTrue();
            assertThat(replyMarkup.path("selective").asBoolean()).isFalse();
            assertThat(replyMarkup.path("input_field_placeholder").asText()).isEqualTo("/predict 42 ");
        } finally {
            server.shutdown();
        }
    }

    @Test
    void sendMessageWithInlineKeyboardPostsExpectedPayload() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        server.start();
        try {
            TelegramBotClient client = new TelegramBotClient("test-token", server.url("/bot-test-token").toString());

            client.sendMessageWithInlineKeyboard(123L, "text", java.util.List.of());

            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath()).isEqualTo("/bot-test-token/sendMessage");

            JsonNode payload = mapper.readTree(request.getBody().readUtf8());
            assertThat(payload.path("chat_id").asLong()).isEqualTo(123L);
            assertThat(payload.path("text").asText()).isEqualTo("text");
            assertThat(payload.path("parse_mode").asText()).isEqualTo("HTML");
            assertThat(payload.path("reply_markup").path("inline_keyboard").isArray()).isTrue();
        } finally {
            server.shutdown();
        }
    }
}
