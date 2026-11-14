package com.example.main.service;

import com.example.main.dto.ChatbotRequest;
import com.example.main.entity.Cinema;
import com.example.main.entity.Genre;
import com.example.main.entity.Movie;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tích hợp tư vấn AI bằng mô hình Google Gemini (mặc định gemini-flash-lite-latest).
 * Cung cấp ngữ cảnh danh sách phim và rạp đang chiếu để trợ lý AI tư vấn thuần đối thoại,
 * tập trung gợi ý phim và hướng dẫn đặt vé thân thiện.
 */
@Component
@Slf4j
public class ChatbotGeminiClient {

    public static final String DEFAULT_MODEL = "gemini-flash-lite-latest";
    private static final String ENV_API_KEY = "GEMINI_API_KEY";
    private static final String ENV_MODEL = "GEMINI_MODEL";

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
                            .connectTimeout(Duration.ofSeconds(5))
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
        if (resolved != null && !resolved.isBlank()) {
            String m = resolved.trim();
            // Chuẩn hóa nếu người dùng ghi biến thể của 3.8 flash lite
            if (m.equalsIgnoreCase("3.8 flash lite")
                    || m.equalsIgnoreCase("3.8-flash-lite")
                    || m.equalsIgnoreCase("gemini-3.8-flash-lite")
                    || m.equalsIgnoreCase("gemini-3.8-flashlite")) {
                return "gemini-flash-lite-latest";
            }
            if (m.equalsIgnoreCase("3.8 flash") || m.equalsIgnoreCase("3.8-flash")) {
                return "gemini-3.8-flash";
            }
            return m;
        }
        return DEFAULT_MODEL;
    }

    private HttpResponse<String> sendGenerateContent(String apiKey, String model, Map<String, Object> requestBody) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        return getOrCreateHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public Optional<String> generateChatResponse(
            String userMessage,
            List<ChatbotRequest.ChatbotHistoryItemDto> history,
            List<Movie> availableMovies,
            List<Cinema> availableCinemas
    ) {
        return generateChatResponse(userMessage, history, availableMovies, availableCinemas, null);
    }

    public Optional<String> generateChatResponse(
            String userMessage,
            List<ChatbotRequest.ChatbotHistoryItemDto> history,
            List<Movie> availableMovies,
            List<Cinema> availableCinemas,
            String userLocation
    ) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured");
            return Optional.empty();
        }
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        try {
            String systemInstruction = buildSystemInstruction(availableMovies, availableCinemas, userLocation);
            List<Map<String, Object>> contents = buildContentsPayload(userMessage, history);

            Map<String, Object> requestBody = Map.of(
                    "contents", contents,
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemInstruction))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 1024
                    )
            );

            String model = resolveModel();
            HttpResponse<String> response = sendGenerateContent(apiKey, model, requestBody);

            // Tự động dự phòng sang Flash Lite nếu model chỉ định bị lỗi 503 (Server Unavailable), 404 (Not Found), 429 (Rate Limit)
            if ((response.statusCode() == 503 || response.statusCode() == 404 || response.statusCode() == 429)
                    && !DEFAULT_MODEL.equals(model)) {
                log.warn("Gemini model '{}' returned {}, retrying with fallback model '{}'",
                        model, response.statusCode(), DEFAULT_MODEL);
                response = sendGenerateContent(apiKey, DEFAULT_MODEL, requestBody);
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API call returned error status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(textNode.asText().trim());
        } catch (Exception ex) {
            log.warn("Gemini chat request failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    List<Map<String, Object>> buildContentsPayload(String userMessage, List<ChatbotRequest.ChatbotHistoryItemDto> history) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            for (ChatbotRequest.ChatbotHistoryItemDto item : history) {
                if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                String role = "user";
                if ("bot".equalsIgnoreCase(item.getRole())
                        || "model".equalsIgnoreCase(item.getRole())
                        || "assistant".equalsIgnoreCase(item.getRole())) {
                    role = "model";
                }

                // Gemini API quy định: turn đầu tiên phải là "user"
                if (contents.isEmpty() && !"user".equals(role)) {
                    continue;
                }

                // Không được 2 role liên tiếp giống nhau
                if (!contents.isEmpty() && contents.get(contents.size() - 1).get("role").equals(role)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(contents.size() - 1).get("parts");
                    parts.add(Map.of("text", item.getContent().trim()));
                    continue;
                }

                List<Map<String, Object>> parts = new ArrayList<>();
                parts.add(Map.of("text", item.getContent().trim()));

                Map<String, Object> contentItem = new HashMap<>();
                contentItem.put("role", role);
                contentItem.put("parts", parts);
                contents.add(contentItem);
            }
        }

        // Đảm bảo tin nhắn người dùng hiện tại là turn cuối cùng
        String cleanUserMsg = userMessage.trim();
        if (contents.isEmpty() || !"user".equals(contents.get(contents.size() - 1).get("role"))) {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", cleanUserMsg));

            Map<String, Object> contentItem = new HashMap<>();
            contentItem.put("role", "user");
            contentItem.put("parts", parts);
            contents.add(contentItem);
        } else {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(contents.size() - 1).get("parts");
            boolean alreadyPresent = parts.stream().anyMatch(p -> cleanUserMsg.equals(p.get("text")));
            if (!alreadyPresent) {
                parts.add(Map.of("text", cleanUserMsg));
            }
        }

        return contents;
    }

    String buildSystemInstruction(List<Movie> movies, List<Cinema> cinemas) {
        return buildSystemInstruction(movies, cinemas, null);
    }

    String buildSystemInstruction(List<Movie> movies, List<Cinema> cinemas, String userLocation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là Trợ lý AI Tư Vấn Phim chuyên nghiệp, vui tính và am hiểu điện ảnh của hệ thống rạp chiếu phim.\n");
        sb.append("Nhiệm vụ hàng đầu của bạn là trò chuyện thuần, cởi mở, lắng nghe sở thích, tâm trạng, thể loại người dùng muốn xem và tư vấn nhiệt tình các bộ phim đang chiếu rạp thật hấp dẫn.\n\n");

        sb.append("DANH SÁCH PHIM ĐANG CHIẾU TẠI RẠP HIỆN TẠI:\n");
        if (movies != null && !movies.isEmpty()) {
            for (Movie m : movies) {
                sb.append("- Phim: \"").append(m.getTitle()).append("\"");
                if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                    String genres = m.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));
                    sb.append(" | Thể loại: ").append(genres);
                }
                Double rating = m.getRating() != null ? m.getRating() : m.getDefaultRating();
                if (rating != null && rating > 0) {
                    sb.append(" | Đánh giá: ").append(String.format(Locale.US, "%.1f", rating)).append("/10");
                }
                if (m.getRuntimeMinutes() != null && m.getRuntimeMinutes() > 0) {
                    sb.append(" | Thời lượng: ").append(m.getRuntimeMinutes()).append(" phút");
                }
                if (m.getDescription() != null && !m.getDescription().isBlank()) {
                    String desc = m.getDescription().trim();
                    if (desc.length() > 120) {
                        desc = desc.substring(0, 117) + "...";
                    }
                    sb.append(" | Nội dung: ").append(desc);
                }
                sb.append("\n");
            }
        } else {
            sb.append("(Hiện chưa có thông tin danh sách phim)\n");
        }

        sb.append("\nDANH SÁCH CÁC CỤM RẠP TRONG HỆ THỐNG:\n");
        if (cinemas != null && !cinemas.isEmpty()) {
            for (Cinema c : cinemas) {
                String sysName = (c.getSystem() != null) ? c.getSystem().getName() : "Rạp";
                sb.append("- [").append(sysName).append("] ").append(c.getName()).append("\n");
            }
        } else {
            sb.append("(Hiện chưa có thông tin danh sách rạp)\n");
        }

        sb.append("\nTHÔNG TIN ĐỊNH VỊ HIỆN TẠI TỪ THIẾT BỊ/WEB CỦA KHÁCH:\n");
        if (userLocation != null && !userLocation.isBlank()) {
            sb.append("Vị trí ước tính từ định vị của web: \"").append(userLocation.trim()).append("\"\n");
        } else {
            sb.append("Chưa có dữ liệu GPS hoặc không xác định được vị trí tương đối từ web.\n");
        }

        sb.append("\nNGUYÊN TẮC PHỤC VỤ VÀ GIAO TIẾP QUAN TRỌNG:\n");
        sb.append("1. Giao tiếp tự nhiên, thân thiện bằng tiếng Việt, xưng \"mình\" và gọi khách là \"bạn\". Dùng emoji sinh động (🎬🍿✨).\n");
        sb.append("2. TẬP TRUNG TƯ VẤN PHIM: Trò chuyện và gợi ý phim theo đúng cảm xúc, nhu cầu, thể loại của khách. TUYỆT ĐỐI KHÔNG bắt người dùng phải chọn rạp hay chọn khu vực ngay từ đầu.\n");
        sb.append("3. XỬ LÝ ĐỊA ĐIỂM/RẠP CHIẾU (CHỈ KHI KHÁCH ĐÃ CHỌN ĐƯỢC PHIM HOẶC CHỦ ĐỘNG HỎI RẠP/SUẤT CHIẾU):\n");
        sb.append("   - NẾU ĐÃ CÓ VỊ TRÍ ĐỊNH VỊ TỪ WEB Ở TRÊN: Trước khi hỏi khách, hãy dùng ngay vị trí định vị này để gợi ý các cụm rạp gần nhất trong danh sách rạp (VD: \"Theo định vị của bạn ở khu vực [Địa điểm], mình thấy có các cụm rạp gần bạn nhất là: ... Nếu bạn đang ở khu vực khác, cứ nhắn lại cho mình nhé!\").\n");
        sb.append("   - NẾU ĐỊNH VỊ BỊ SAI VÀ KHÁCH SỬA LẠI ĐỊA CHỈ: Nếu khách nói định vị sai hoặc cung cấp địa chỉ khác trong câu chat (VD: \"Không, mình ở Đống Đa\", \"Mình ở Quận 7\"), BẠN PHẢI LẬP TỨC ƯU TIÊN SỬ DỤNG ĐỊA CHỈ DO KHÁCH CUNG CẤP để gợi ý rạp phù hợp nhất!\n");
        sb.append("   - NẾU KHÔNG CÓ DỮ LIỆU GPS / KHÔNG XÁC ĐỊNH ĐƯỢC VỊ TRÍ: Lúc này bạn mới hỏi khách đang ở khu vực, quận hoặc thành phố nào để gợi ý rạp gần nhất. Cho khách tự nhập tự nhiên, không bắt ép chọn ô.\n");
        sb.append("4. TÊN PHIM: Luôn in đậm tên phim chính xác như trong danh sách (VD: **Doraemon: Nobita và Bản Giao Hưởng Địa Cầu**).\n");
        sb.append("5. GIỮ ĐÚNG VAI TRÒ: Nếu khách hỏi những câu không liên quan đến phim ảnh (như viết code, giải toán, bài tập...), hãy từ chối khéo léo, hóm hỉnh và dẫn dắt người dùng quay trở lại với phim ảnh.\n");

        return sb.toString();
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
}
