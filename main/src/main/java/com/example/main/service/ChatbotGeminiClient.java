package com.example.main.service;

import com.example.main.dto.ChatbotStateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tích hợp tùy chọn với Google Gemini API thông qua biến môi trường hoặc file .env (GEMINI_API_KEY).
 * Nếu không có key hoặc lỗi mạng, hệ thống tự động fallback sang bộ NLP tiếng Việt cục bộ.
 */
@Component
@Slf4j
public class ChatbotGeminiClient {

    public static final String DEFAULT_MODEL = "gemini-flash-latest";
    private static final String ENV_API_KEY = "GEMINI_API_KEY";
    private static final String ENV_MODEL = "GEMINI_MODEL";

    public record GeminiSlots(
            String location,
            String genre,
            Boolean genreSkipped,
            String timeExpression,
            Boolean timeSkipped,
            Boolean resetRequested
    ) {
    }

    private final String configuredApiKey;
    private final String configuredModel;
    private final Environment springEnvironment;
    private final ObjectMapper objectMapper;
    private final List<Path> dotenvSearchDirectories;
    private final boolean allowSystemEnvLookup;
    private volatile HttpClient httpClient;

    @Autowired
    public ChatbotGeminiClient(
            @Value("${app.ai.gemini.api-key:}") String configuredApiKey,
            @Value("${app.ai.gemini.model:" + DEFAULT_MODEL + "}") String configuredModel,
            Environment springEnvironment,
            ObjectMapper objectMapper) {
        this(
                configuredApiKey,
                configuredModel,
                springEnvironment,
                objectMapper,
                defaultDotenvDirectories(),
                true
        );
    }

    /**
     * Constructor dùng cho unit test cô lập (không tự đọc file .env thật trên máy).
     */
    public ChatbotGeminiClient(String configuredApiKey, String configuredModel, ObjectMapper objectMapper) {
        this(configuredApiKey, configuredModel, null, objectMapper, List.of(), false);
    }

    /**
     * Constructor dùng để kiểm thử đọc trực tiếp các đường dẫn file .env chỉ định.
     */
    ChatbotGeminiClient(
            String configuredApiKey,
            String configuredModel,
            Environment springEnvironment,
            ObjectMapper objectMapper,
            List<Path> dotenvSearchDirectories,
            boolean allowSystemEnvLookup) {
        this.configuredApiKey = configuredApiKey;
        this.configuredModel = configuredModel;
        this.springEnvironment = springEnvironment;
        this.objectMapper = objectMapper;
        this.dotenvSearchDirectories = dotenvSearchDirectories != null ? dotenvSearchDirectories : List.of();
        this.allowSystemEnvLookup = allowSystemEnvLookup;
    }

    private static List<Path> defaultDotenvDirectories() {
        return List.of(
                Path.of("."),
                Path.of(".."),
                Path.of("main")
        );
    }

    private HttpClient getOrCreateHttpClient() {
        HttpClient client = this.httpClient;
        if (client == null) {
            synchronized (this) {
                if (this.httpClient == null) {
                    this.httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(4))
                            .build();
                }
                client = this.httpClient;
            }
        }
        return client;
    }

    public boolean isConfigured() {
        return resolveApiKey() != null;
    }

    public String resolveModel() {
        String resolved = resolveConfigValue(configuredModel, ENV_MODEL, null);
        return (resolved != null && !resolved.isBlank()) ? resolved : DEFAULT_MODEL;
    }

    public Optional<GeminiSlots> extractSlots(String userMessage, ChatbotStateDto state, List<String> availableGenres) {
        String apiKey = resolveApiKey();
        if (apiKey == null || userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        try {
            String prompt = buildExtractionPrompt(userMessage, state, availableGenres);
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.0,
                            "responseMimeType", "application/json"
                    )
            );

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + resolveModel() + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = getOrCreateHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned status {}, falling back to local NLP", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                return Optional.empty();
            }

            JsonNode parsed = objectMapper.readTree(textNode.asText());
            return Optional.of(new GeminiSlots(
                    readNullableText(parsed, "location"),
                    readNullableText(parsed, "genre"),
                    readNullableBool(parsed, "genre_skipped"),
                    readNullableText(parsed, "time_expression"),
                    readNullableBool(parsed, "time_skipped"),
                    readNullableBool(parsed, "reset_requested")
            ));
        } catch (Exception ex) {
            log.warn("Gemini extraction failed, falling back to local NLP: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    String resolveApiKey() {
        return resolveConfigValue(configuredApiKey, ENV_API_KEY, null);
    }

    private String resolveConfigValue(String springPropertyValue, String envName, String fallbackValue) {
        if (springPropertyValue != null && !springPropertyValue.isBlank()
                && !(ENV_MODEL.equals(envName) && DEFAULT_MODEL.equals(springPropertyValue.trim()))) {
            return springPropertyValue.trim();
        }

        if (springEnvironment != null) {
            String fromSpringEnv = springEnvironment.getProperty(envName);
            if (fromSpringEnv != null && !fromSpringEnv.isBlank()) {
                return fromSpringEnv.trim();
            }
        }

        if (allowSystemEnvLookup) {
            String fromSystemEnv = System.getenv(envName);
            if (fromSystemEnv != null && !fromSystemEnv.isBlank()) {
                return fromSystemEnv.trim();
            }
        }

        String fromDotenv = readFromDotenvFiles(envName);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            return fromDotenv.trim();
        }

        return (springPropertyValue != null && !springPropertyValue.isBlank())
                ? springPropertyValue.trim()
                : fallbackValue;
    }

    private String readFromDotenvFiles(String key) {
        for (Path dir : dotenvSearchDirectories) {
            if (dir == null) {
                continue;
            }
            Path envFile = dir.toAbsolutePath().normalize().resolve(".env");
            if (!Files.isRegularFile(envFile)) {
                continue;
            }
            try {
                String val = parseKeyFromDotenvFile(envFile, key);
                if (val != null && !val.isBlank()) {
                    return val;
                }
            } catch (Exception ex) {
                log.debug("Unable to read {} from {}: {}", key, envFile, ex.getMessage());
            }
        }
        return null;
    }

    private String parseKeyFromDotenvFile(Path envFile, String targetKey) throws Exception {
        List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int eqIdx = line.indexOf('=');
            if (eqIdx <= 0) {
                continue;
            }
            String key = line.substring(0, eqIdx).trim();
            if (!targetKey.equals(key)) {
                continue;
            }
            String rawVal = line.substring(eqIdx + 1).trim();
            return stripOptionalQuotesAndInlineComment(rawVal);
        }
        return null;
    }

    private String stripOptionalQuotesAndInlineComment(String rawVal) {
        if (rawVal == null || rawVal.isEmpty()) {
            return "";
        }
        if ((rawVal.startsWith("\"") && rawVal.endsWith("\"") && rawVal.length() >= 2)
                || (rawVal.startsWith("'") && rawVal.endsWith("'") && rawVal.length() >= 2)) {
            return rawVal.substring(1, rawVal.length() - 1).trim();
        }
        int commentIdx = rawVal.indexOf(" #");
        if (commentIdx >= 0) {
            rawVal = rawVal.substring(0, commentIdx).trim();
        }
        return rawVal.trim();
    }

    private String buildExtractionPrompt(String userMessage, ChatbotStateDto state, List<String> availableGenres) {
        String step = state != null && state.getPendingStep() != null ? state.getPendingStep().name() : "LOCATION";
        String genresText = (availableGenres != null && !availableGenres.isEmpty())
                ? String.join(", ", availableGenres)
                : "Hành Động, Kinh Dị, Hài Hước, Tâm Lý, Hoạt Hình, Phiêu Lưu, Giả Tưởng, Khoa Học Viễn Tưởng, Gia Đình, Giật Gân, Bí Ẩn, Hình Sự, Âm Nhạc, Lịch Sử, Tiểu Sử, Fantasy";

        return """
                Trích xuất ý định từ câu tiếng Việt của khách đặt vé xem phim. Trả về JSON thuần theo schema:
                {
                  "location": string|null,
                  "genre": string|null,
                  "genre_skipped": boolean,
                  "time_expression": string|null,
                  "time_skipped": boolean,
                  "reset_requested": boolean
                }
                Ngữ cảnh bước đang hỏi: %s
                Danh sách thể loại hợp lệ trong DB: %s
                Quy tắc:
                - Nếu khách nói "không cần", "sao cũng được", "gì cũng được", hãy đánh dấu genre_skipped=true nếu đang hỏi GENRE hoặc về thể loại; time_skipped=true nếu đang hỏi TIME hoặc về giờ chiếu.
                - Không suy diễn câu hỏi "có phim nào không?" thành skipped.
                - "genre" phải khớp một trong các thể loại hợp lệ nếu có.
                - "time_expression" giữ nguyên cụm mô tả giờ/buổi/ngày (VD: "buổi tối", "sau 19h", "hôm nay", "cuối tuần").
                Câu của khách: "%s"
                """.formatted(step, genresText, userMessage.replace("\"", "'"));
    }

    private String readNullableText(JsonNode node, String field) {
        JsonNode val = node.path(field);
        if (val.isMissingNode() || val.isNull()) {
            return null;
        }
        String text = val.asText("").trim();
        return (text.isEmpty() || "null".equalsIgnoreCase(text)) ? null : text;
    }

    private Boolean readNullableBool(JsonNode node, String field) {
        JsonNode val = node.path(field);
        return (!val.isMissingNode() && val.isBoolean()) ? val.asBoolean() : Boolean.FALSE;
    }
}
