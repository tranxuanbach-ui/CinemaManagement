package com.example.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void shouldPreferExplicitSpringPropertyOverDotenv() throws Exception {
        Files.writeString(tempDir.resolve(".env"), "GEMINI_API_KEY=dotenv-key\n");

        ChatbotGeminiClient client = new ChatbotGeminiClient(
                "explicit-spring-key",
                "gemini-2.5-pro",
                null,
                new ObjectMapper(),
                List.of(tempDir),
                false
        );

        assertTrue(client.isConfigured());
        assertEquals("explicit-spring-key", client.resolveApiKey());
        assertEquals("gemini-2.5-pro", client.resolveModel());
    }

    @Test
    void shouldSupportQuotedAndExportedEntriesInDotenvFile() throws Exception {
        Files.writeString(tempDir.resolve(".env"), """
                export GEMINI_API_KEY="quoted-api-key-123"
                GEMINI_MODEL='gemini-2.5-flash'
                """);

        ChatbotGeminiClient client = new ChatbotGeminiClient(
                "",
                ChatbotGeminiClient.DEFAULT_MODEL,
                null,
                new ObjectMapper(),
                List.of(tempDir),
                false
        );

        assertTrue(client.isConfigured());
        assertEquals("quoted-api-key-123", client.resolveApiKey());
        assertEquals("gemini-2.5-flash", client.resolveModel());
    }
}
