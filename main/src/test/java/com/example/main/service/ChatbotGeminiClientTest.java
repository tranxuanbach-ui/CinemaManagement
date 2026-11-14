package com.example.main.service;

import com.example.main.dto.ChatbotRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChatbotGeminiClientTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadApiKeyAndModelFromDotenvFileInParentOrSiblingDirectory() throws Exception {
        Path missingDir = tempDir.resolve("missing");
        Path rootDir = tempDir.resolve("workspace-root");
        Files.createDirectories(rootDir);
        Files.writeString(rootDir.resolve(".env"), """
                # Comment
                GEMINI_API_KEY=test-dotenv-secret-key
                GEMINI_MODEL=gemini-2.5-flash
                """);

        ChatbotGeminiClient client = new ChatbotGeminiClient(
                "",
                ChatbotGeminiClient.DEFAULT_MODEL,
                null,
                new ObjectMapper(),
                List.of(missingDir, rootDir),
                false
        );

        assertTrue(client.isConfigured());
        assertEquals("test-dotenv-secret-key", client.resolveApiKey());
        assertEquals("gemini-2.5-flash", client.resolveModel());
    }

    @Test
    void shouldStayUnconfiguredWhenDotenvKeyIsBlank() throws Exception {
        Files.writeString(tempDir.resolve(".env"), "GEMINI_API_KEY=\n");

        ChatbotGeminiClient client = new ChatbotGeminiClient(
                "",
                ChatbotGeminiClient.DEFAULT_MODEL,
                null,
                new ObjectMapper(),
                List.of(tempDir),
                false
        );

        assertFalse(client.isConfigured());
        assertNull(client.resolveApiKey());
        assertEquals(ChatbotGeminiClient.DEFAULT_MODEL, client.resolveModel());
    }

    @Test
    void shouldNormalizeFlashLiteAliasesToDefaultModel() {
        ChatbotGeminiClient client1 = new ChatbotGeminiClient("", "3.8 flash lite", new ObjectMapper());
        assertEquals("gemini-flash-lite-latest", client1.resolveModel());

        ChatbotGeminiClient client2 = new ChatbotGeminiClient("", "gemini-3.8-flash-lite", new ObjectMapper());
        assertEquals("gemini-flash-lite-latest", client2.resolveModel());
    }

    @Test
    void shouldBuildContentsPayloadWithAlternatingRolesAndEnsureUserMessageLast() {
        ChatbotGeminiClient client = new ChatbotGeminiClient("", ChatbotGeminiClient.DEFAULT_MODEL, new ObjectMapper());

        List<ChatbotRequest.ChatbotHistoryItemDto> history = List.of(
                ChatbotRequest.ChatbotHistoryItemDto.builder().role("bot").content("Xin chào bạn").build(),
                ChatbotRequest.ChatbotHistoryItemDto.builder().role("user").content("Tư vấn giúp mình phim").build(),
                ChatbotRequest.ChatbotHistoryItemDto.builder().role("bot").content("Bạn thích thể loại gì?").build()
        );

        List<Map<String, Object>> contents = client.buildContentsPayload("Mình thích phim hài", history);

        assertNotNull(contents);
        assertFalse(contents.isEmpty());

        // Turn đầu tiên phải là "user"
        assertEquals("user", contents.get(0).get("role"));

        // Turn cuối cùng phải là "user" với nội dung tin nhắn hiện tại
        Map<String, Object> lastTurn = contents.get(contents.size() - 1);
        assertEquals("user", lastTurn.get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) lastTurn.get("parts");
        assertEquals("Mình thích phim hài", parts.get(0).get("text"));
    }

    @Test
    void shouldReturnEmptyWhenApiKeyIsNotConfigured() {
        ChatbotGeminiClient client = new ChatbotGeminiClient("", ChatbotGeminiClient.DEFAULT_MODEL, new ObjectMapper());
        Optional<String> res = client.generateChatResponse("Xin chào", List.of(), List.of(), List.of());
        assertTrue(res.isEmpty());
    }
}
