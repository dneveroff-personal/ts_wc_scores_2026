package com.tswcscores.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;

@Disabled("Requires test constructor - refactor needed")
class TelegramBotClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

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

    @Test
    void sendMessagePostsExpectedPayload() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        server.start();
        try {
            TelegramBotClient client = new TelegramBotClient("test-token", server.url("/bot-test-token").toString());

            client.sendMessage(123L, "Hello");

            RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath()).isEqualTo("/bot-test-token/sendMessage");

            JsonNode payload = mapper.readTree(request.getBody().readUtf8());
            assertThat(payload.path("chat_id").asLong()).isEqualTo(123L);
            assertThat(payload.path("text").asText()).isEqualTo("Hello");
            assertThat(payload.path("parse_mode").asText()).isEqualTo("HTML");
        } finally {
            server.shutdown();
        }
    }
}
