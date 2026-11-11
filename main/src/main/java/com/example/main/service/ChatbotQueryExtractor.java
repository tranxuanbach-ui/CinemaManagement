package com.example.main.service;

import com.example.main.dto.ChatbotStateDto;
import com.example.main.entity.Cinema;
import com.example.main.entity.Genre;
import com.example.main.enums.ChatbotStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatbotQueryExtractor {

    private final ChatbotLocationMatcher locationMatcher;
    private final ChatbotGeminiClient geminiClient;

    public record TimeExtraction(
            String timeSlot,
            String timeLabel,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate targetDate,
            boolean weekendOnly
    ) {
        public boolean hasAnyTimeConstraint() {
            return timeSlot != null || startTime != null || endTime != null || targetDate != null || weekendOnly;
        }
    }

    public record ExtractedSlots(
            boolean resetRequested,
            String location,
            boolean locationExplicitChange,
            String genre,
            boolean genreChangeRequested,
            boolean skipPreference,
            boolean explicitHighRating,
            boolean explicitBusyIdealTime,
            TimeExtraction time,
            boolean timeChangeRequested,
            boolean usedGemini
    ) {
    }

    private static final Pattern RANGE_TIME_PATTERN = Pattern.compile(
            "(?:tu|khoang)?\\s*(\\d{1,2})(?:h|:| gio )(\\d{0,2})?\\s*(?:den|-|toi)\\s*(\\d{1,2})(?:h|:| gio )(\\d{0,2})?"
    );
    private static final Pattern AFTER_TIME_PATTERN = Pattern.compile(
            "(?:sau|tu)\\s+(\\d{1,2})(?:h|:| gio )(\\d{0,2})?"
    );
    private static final Pattern BEFORE_TIME_PATTERN = Pattern.compile(
            "(?:truoc|den)\\s+(\\d{1,2})(?:h|:| gio )(\\d{0,2})?"
    );
    private static final Pattern SINGLE_TIME_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})\\s*(?:h|:|gio)\\s*(\\d{1,2})?\\s*(sang|trua|chieu|toi|dem)?\\b"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b"
    );

    private static final Map<String, String> GENRE_SYNONYMS = buildGenreSynonyms();

    private static final List<String> SKIP_PHRASES = List.of(
            "khong can", "ko can", "k can", "khong can chon", "khong can dau",
            "sao cung duoc", "gi cung duoc", "the nao cung duoc", "nao cung duoc",
            "luc nao cung duoc", "gio nao cung duoc", "khi nao cung duoc",
            "tuy ban", "tuy ai", "tuy he thong", "bat ky", "bo qua", "skip"
    );

    private static final Set<String> EXACT_NEGATIVE_REPLIES = Set.of(
            "khong", "khong a", "khong dau", "khong nhe", "ko", "k", "hok", "hong", "tuy"
    );

    private static final List<String> RESET_PHRASES = List.of(
            "lam moi", "bat dau lai", "reset", "tim lai", "tu van lai", "chon lai tu dau", "xoa bo loc"
    );

    public ExtractedSlots extract(
            String message,
            ChatbotStateDto currentState,
            List<Cinema> allCinemas,
            List<Genre> allGenres,
            LocalDate today
    ) {
        String normalized = ChatbotTextNormalizer.normalize(message);
        String normTimeText = ChatbotTextNormalizer.normalizeKeepTimePunctuation(message);

        boolean resetRequested = containsAnyPhrase(normalized, RESET_PHRASES);
        boolean skipPreference = containsSkipIntent(normalized, currentState);
        boolean explicitHighRating = containsAnyPhrase(normalized, List.of(
                "rating cao", "diem cao", "phim hay nhat", "top phim", "danh gia cao", "hay nhat"
        ));
        boolean explicitBusyIdealTime = containsAnyPhrase(normalized, List.of(
                "nguoi ban ron", "ban ron", "khung gio ly tuong", "gio ly tuong", "gio dep nhat"
        ));

        String detectedLocation = locationMatcher.extractLocationOrCinemaFromText(message, allCinemas);
        boolean locationExplicitChange = containsAnyPhrase(normalized, List.of(
                "doi dia diem", "doi rap", "cho khac", "khu vuc khac", "doi khu vuc"
        ));
        boolean genreChangeRequested = containsAnyPhrase(normalized, List.of(
                "doi the loai", "the loai khac", "chon lai the loai", "doi phim"
        ));
        boolean timeChangeRequested = containsAnyPhrase(normalized, List.of(
                "doi khung gio", "doi gio", "gio khac", "khung gio khac", "doi thoi gian", "thoi gian khac"
        ));

        String detectedGenre = matchGenre(message, normalized, allGenres);
        ChatbotStep pendingStep = currentState != null ? currentState.getPendingStep() : ChatbotStep.LOCATION;
        TimeExtraction timeExtraction = extractTime(message, normTimeText, normalized, detectedLocation, pendingStep, today);
        boolean usedGemini = false;

        // Thử nâng cấp trích xuất bằng Gemini nếu người dùng cấu hình API key và câu phức tạp / chưa bắt đủ
        if (!resetRequested && geminiClient.isConfigured()) {
            List<String> genreNames = allGenres != null
                    ? allGenres.stream().map(Genre::getName).toList()
                    : List.of();
            Optional<ChatbotGeminiClient.GeminiSlots> geminiOpt =
                    geminiClient.extractSlots(message, currentState, genreNames);
            if (geminiOpt.isPresent()) {
                usedGemini = true;
                ChatbotGeminiClient.GeminiSlots gSlots = geminiOpt.get();
                if (Boolean.TRUE.equals(gSlots.resetRequested())) {
                    resetRequested = true;
                }
                if (detectedLocation == null && gSlots.location() != null) {
                    String mappedLoc = locationMatcher.extractLocationOrCinemaFromText(gSlots.location(), allCinemas);
                    detectedLocation = mappedLoc != null ? mappedLoc : gSlots.location();
                }
                if (detectedGenre == null && gSlots.genre() != null) {
                    detectedGenre = matchGenre(gSlots.genre(), ChatbotTextNormalizer.normalize(gSlots.genre()), allGenres);
                }
                ChatbotStep pending = currentState != null ? currentState.getPendingStep() : ChatbotStep.LOCATION;
                if (Boolean.TRUE.equals(gSlots.genreSkipped()) && (pending == ChatbotStep.GENRE || detectedGenre == null)) {
                    skipPreference = true;
                }
                if (Boolean.TRUE.equals(gSlots.timeSkipped()) && (pending == ChatbotStep.TIME || !timeExtraction.hasAnyTimeConstraint())) {
                    skipPreference = true;
                }
                if (!timeExtraction.hasAnyTimeConstraint() && gSlots.timeExpression() != null) {
                    String gTimeNorm = ChatbotTextNormalizer.normalizeKeepTimePunctuation(gSlots.timeExpression());
                    TimeExtraction fromGemini = extractTime(
                            gSlots.timeExpression(),
                            gTimeNorm,
                            ChatbotTextNormalizer.normalize(gSlots.timeExpression()),
                            detectedLocation,
                            pending,
                            today
                    );
                    if (fromGemini.hasAnyTimeConstraint()) {
                        timeExtraction = fromGemini;
                    }
                }
            }
        }

        // Trong ngữ cảnh vừa hỏi GENRE mà người dùng đòi "rating cao" / "hay nhất" -> hiểu là bỏ qua chọn thể loại cụ thể để lấy top rating
        if (currentState != null && currentState.getPendingStep() == ChatbotStep.GENRE
                && detectedGenre == null && explicitHighRating) {
            skipPreference = true;
        }
        // Trong ngữ cảnh vừa hỏi TIME mà người dùng nói "giờ lý tưởng cho người bận rộn" -> hiểu là skip chọn giờ cố định để AI đề xuất giờ bận rộn
        if (explicitBusyIdealTime && !timeExtraction.hasAnyTimeConstraint()) {
            skipPreference = true;
        }

        return new ExtractedSlots(
                resetRequested,
                detectedLocation,
                locationExplicitChange,
                detectedGenre,
                genreChangeRequested,
                skipPreference,
                explicitHighRating,
                explicitBusyIdealTime,
                timeExtraction,
                timeChangeRequested,
                usedGemini
        );
    }

    private boolean containsSkipIntent(String normalized, ChatbotStateDto state) {
        if (normalized.isBlank()) {
            return false;
        }
        if (containsAnyPhrase(normalized, SKIP_PHRASES)) {
            return true;
        }
        // Chỉ coi câu trả lời cụt "không" / "ko" là skip khi AI đang chờ trả lời thể loại hoặc giờ chiếu
        ChatbotStep pending = state != null ? state.getPendingStep() : ChatbotStep.LOCATION;
        if ((pending == ChatbotStep.GENRE || pending == ChatbotStep.TIME)
                && EXACT_NEGATIVE_REPLIES.contains(normalized)) {
            return true;
        }
        return false;
    }

    public String matchGenre(String rawMessage, String normalizedText, List<Genre> allGenres) {
        if (normalizedText == null || normalizedText.isBlank() || allGenres == null || allGenres.isEmpty()) {
            return null;
        }
        String rawLower = rawMessage != null ? rawMessage.toLowerCase(Locale.forLanguageTag("vi")) : "";

        // Khớp trực tiếp tên thể loại chuẩn trong DB (ưu tiên tên dài hơn trước để "Khoa Học Viễn Tưởng" thắng "Giả Tưởng")
        List<Genre> sortedGenres = allGenres.stream()
                .filter(g -> g != null && g.getName() != null)
                .sorted(Comparator.comparingInt((Genre g) -> ChatbotTextNormalizer.normalize(g.getName()).length()).reversed())
                .toList();

        for (Genre g : sortedGenres) {
            String normGenre = ChatbotTextNormalizer.normalize(g.getName());
            if (ChatbotTextNormalizer.containsWholePhrase(normalizedText, normGenre)) {
                return g.getName();
            }
        }

        // Phân biệt có dấu "hài" (thể loại) vs "hai" (số 2 / Hai Bà Trưng)
        if (containsRawToken(rawLower, "hài") || "hai".equals(normalizedText) || "hai kich".equals(normalizedText)) {
            return findGenreNameByNormalized(sortedGenres, "hai huoc");
        }
        // Phân biệt "phim ma" / standalone "ma" không nhầm "mà"
        if ("ma".equals(normalizedText)) {
            return findGenreNameByNormalized(sortedGenres, "kinh di");
        }

        // Khớp theo từ khóa đồng nghĩa phổ biến của người dùng
        String bestAliasTarget = null;
        int bestAliasLen = -1;
        for (Map.Entry<String, String> entry : GENRE_SYNONYMS.entrySet()) {
            String alias = entry.getKey();
            if (ChatbotTextNormalizer.containsWholePhrase(normalizedText, alias) && alias.length() > bestAliasLen) {
                bestAliasLen = alias.length();
                bestAliasTarget = entry.getValue();
            }
        }

        if (bestAliasTarget != null) {
            return findGenreNameByNormalized(sortedGenres, bestAliasTarget);
        }

        return null;
    }

    private String findGenreNameByNormalized(List<Genre> genres, String normalizedTarget) {
        for (Genre g : genres) {
            if (ChatbotTextNormalizer.normalize(g.getName()).equals(normalizedTarget)) {
                return g.getName();
            }
        }
        return null;
    }

    public TimeExtraction extractTime(
            String rawMessage,
            String normTimeText,
            String normalizedWords,
            String detectedLocation,
            ChatbotStep pendingStep,
            LocalDate today
    ) {
        String rawLower = rawMessage != null ? rawMessage.toLowerCase(Locale.forLanguageTag("vi")) : "";
        LocalTime startTime = null;
        LocalTime endTime = null;
        String timeSlot = null;
        String timeLabel = null;
        LocalDate targetDate = null;
        boolean weekendOnly = false;

        // 1. Phân tích ngày chiếu (tránh nhầm "Hoàng Mai" là "mai")
        String cleanedForDate = normalizedWords;
        if (detectedLocation != null && ChatbotTextNormalizer.normalize(detectedLocation).contains("mai")) {
            cleanedForDate = cleanedForDate.replace("hoang mai", "");
        }

        if (containsAnyPhrase(cleanedForDate, List.of("hom nay", "toi nay", "chieu nay", "trua nay", "sang nay", "dem nay"))) {
            targetDate = today;
        } else if (containsAnyPhrase(cleanedForDate, List.of("ngay mai", "toi mai", "chieu mai", "trua mai", "sang mai", "dem mai"))
                || ChatbotTextNormalizer.containsWholePhrase(cleanedForDate, "mai")) {
            targetDate = today.plusDays(1);
        } else if (containsAnyPhrase(cleanedForDate, List.of("ngay kia", "ngay mot"))) {
            targetDate = today.plusDays(2);
        } else if (containsAnyPhrase(cleanedForDate, List.of("thu 7", "thu bay"))) {
            targetDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        } else if (containsAnyPhrase(cleanedForDate, List.of("chu nhat", "cn"))) {
            targetDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else if (containsAnyPhrase(cleanedForDate, List.of("cuoi tuan", "weekend"))) {
            weekendOnly = true;
            timeSlot = "WEEKEND";
            timeLabel = "Cuối tuần (Thứ 7 - Chủ nhật)";
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(normTimeText);
        if (dateMatcher.find() && targetDate == null) {
            try {
                int day = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int year = dateMatcher.group(3) != null
                        ? parseYear(dateMatcher.group(3), today.getYear())
                        : today.getYear();
                targetDate = LocalDate.of(year, month, day);
            } catch (Exception ignored) {
            }
        }

        // 2. Phân tích khung giờ cụ thể (range -> sau Xh -> trước Xh -> giờ đơn -> buổi)
        Matcher rangeMatcher = RANGE_TIME_PATTERN.matcher(normTimeText);
        if (rangeMatcher.find()) {
            LocalTime t1 = toLocalTime(rangeMatcher.group(1), rangeMatcher.group(2), null);
            LocalTime t2 = toLocalTime(rangeMatcher.group(3), rangeMatcher.group(4), null);
            if (t1 != null && t2 != null && !t2.isBefore(t1)) {
                startTime = t1;
                endTime = t2;
                timeSlot = "CUSTOM";
                timeLabel = String.format("Từ %s đến %s", t1, t2);
            }
        }

        boolean explicitAfternoonHint = containsRawToken(rawLower, "chiều")
                || containsAnyPhrase(normalizedWords, List.of("buoi chieu", "chieu nay", "chieu mai"));
        boolean explicitEveningHint = containsRawToken(rawLower, "tối")
                || containsRawToken(rawLower, "đêm")
                || containsAnyPhrase(normalizedWords, List.of("buoi toi", "toi nay", "toi mai", "khuya", "dem nay", "dem"));

        if (startTime == null) {
            Matcher afterMatcher = AFTER_TIME_PATTERN.matcher(normTimeText);
            if (afterMatcher.find()) {
                int hour = Integer.parseInt(afterMatcher.group(1));
                int min = parseMinute(afterMatcher.group(2));
                if (hour >= 1 && hour <= 11 && (explicitEveningHint || explicitAfternoonHint)) {
                    hour += 12;
                }
                if (hour >= 0 && hour <= 23) {
                    startTime = LocalTime.of(hour, min);
                    endTime = LocalTime.of(23, 59);
                    timeSlot = "CUSTOM";
                    timeLabel = "Từ " + startTime + " trở đi";
                }
            }
        }

        if (startTime == null) {
            Matcher beforeMatcher = BEFORE_TIME_PATTERN.matcher(normTimeText);
            if (beforeMatcher.find()) {
                int hour = Integer.parseInt(beforeMatcher.group(1));
                int min = parseMinute(beforeMatcher.group(2));
                if (hour >= 1 && hour <= 11 && (explicitEveningHint || explicitAfternoonHint)) {
                    hour += 12;
                }
                if (hour >= 0 && hour <= 23) {
                    startTime = LocalTime.of(0, 0);
                    endTime = LocalTime.of(hour, min);
                    timeSlot = "CUSTOM";
                    timeLabel = "Trước " + endTime;
                }
            }
        }

        if (startTime == null) {
            Matcher singleMatcher = SINGLE_TIME_PATTERN.matcher(normTimeText);
            if (singleMatcher.find()) {
                LocalTime exact = toLocalTime(singleMatcher.group(1), singleMatcher.group(2), singleMatcher.group(3));
                if (exact != null) {
                    if (exact.getHour() >= 1 && exact.getHour() <= 11 && (explicitEveningHint || explicitAfternoonHint)) {
                        exact = exact.plusHours(12);
                    }
                    startTime = exact.isAfter(LocalTime.of(0, 45)) ? exact.minusMinutes(45) : LocalTime.of(0, 0);
                    endTime = exact.isBefore(LocalTime.of(22, 59)) ? exact.plusMinutes(60) : LocalTime.of(23, 59);
                    timeSlot = "CUSTOM";
                    timeLabel = "Khoảng " + exact + " (" + startTime + " - " + endTime + ")";
                }
            }
        }

        // 3. Nếu chưa có giờ số cụ thể, xét các buổi trong ngày (tránh nhầm "tôi" -> "tối", "tôi muốn" -> "tối muộn", "chiếu" -> "chiều", "sang" -> "sáng")
        if (startTime == null) {
            boolean hasPronounToiMuon = containsRawToken(rawLower, "muốn")
                    || normalizedWords.contains("muon xem")
                    || normalizedWords.contains("muon tim")
                    || normalizedWords.contains("muon di")
                    || normalizedWords.contains("muon hoi")
                    || normalizedWords.contains("muon dat");

            boolean isLateNight = containsRawPhrase(rawLower, "tối muộn")
                    || containsRawToken(rawLower, "muộn")
                    || containsRawToken(rawLower, "đêm")
                    || containsRawToken(rawLower, "khuya")
                    || containsAnyPhrase(normalizedWords, List.of("khuya", "suat muon", "sau gio lam muon", "chieu muon", "dem nay"))
                    || (!hasPronounToiMuon && !containsRawToken(rawLower, "tôi")
                        && ChatbotTextNormalizer.containsWholePhrase(normalizedWords, "toi muon"));

            boolean isEvening = containsRawToken(rawLower, "tối")
                    || containsAnyPhrase(normalizedWords, List.of("buoi toi", "toi nay", "toi mai", "ca toi", "suat toi", "xem toi"))
                    || (pendingStep == ChatbotStep.TIME && "toi".equals(normalizedWords));

            boolean isNoon = containsRawToken(rawLower, "trưa")
                    || containsAnyPhrase(normalizedWords, List.of("buoi trua", "trua nay", "trua mai", "gio nghi trua", "nghi trua", "ca trua", "trua"));

            boolean isAfternoon = containsRawToken(rawLower, "chiều")
                    || containsAnyPhrase(normalizedWords, List.of("buoi chieu", "chieu nay", "chieu mai", "ca chieu"))
                    || (pendingStep == ChatbotStep.TIME && "chieu".equals(normalizedWords));

            boolean isMorning = containsRawToken(rawLower, "sáng")
                    || containsAnyPhrase(normalizedWords, List.of("buoi sang", "sang nay", "sang mai", "ca sang", "suat sang"))
                    || (pendingStep == ChatbotStep.TIME && "sang".equals(normalizedWords));

            if (isLateNight) {
                timeSlot = "LATE_NIGHT";
                startTime = LocalTime.of(21, 0);
                endTime = LocalTime.of(23, 59);
                timeLabel = "Tối muộn / đêm (21:00 - 23:59)";
            } else if (isEvening) {
                timeSlot = "EVENING";
                startTime = LocalTime.of(18, 0);
                endTime = LocalTime.of(21, 59);
                timeLabel = "Buổi tối (18:00 - 21:59)";
            } else if (isNoon) {
                timeSlot = "NOON";
                startTime = LocalTime.of(11, 30);
                endTime = LocalTime.of(13, 59);
                timeLabel = "Buổi trưa (11:30 - 13:59)";
            } else if (isAfternoon) {
                timeSlot = "AFTERNOON";
                startTime = LocalTime.of(14, 0);
                endTime = LocalTime.of(17, 59);
                timeLabel = "Buổi chiều (14:00 - 17:59)";
            } else if (isMorning) {
                timeSlot = "MORNING";
                startTime = LocalTime.of(8, 0);
                endTime = LocalTime.of(11, 59);
                timeLabel = "Buổi sáng (08:00 - 11:59)";
            }
        }

        if (targetDate != null && timeLabel == null && !weekendOnly) {
            timeLabel = "Ngày " + targetDate;
            timeSlot = "DATE_ONLY";
        } else if (targetDate != null && timeLabel != null) {
            timeLabel = timeLabel + " • " + targetDate;
        }

        return new TimeExtraction(timeSlot, timeLabel, startTime, endTime, targetDate, weekendOnly);
    }

    private boolean containsRawToken(String rawLower, String accentedWord) {
        if (rawLower == null || rawLower.isBlank()) {
            return false;
        }
        String cleaned = rawLower.replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
        return ChatbotTextNormalizer.containsWholePhrase(cleaned, accentedWord);
    }

    private boolean containsRawPhrase(String rawLower, String accentedPhrase) {
        if (rawLower == null || rawLower.isBlank()) {
            return false;
        }
        String cleaned = rawLower.replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
        return ChatbotTextNormalizer.containsWholePhrase(cleaned, accentedPhrase);
    }

    private LocalTime toLocalTime(String hourStr, String minStr, String periodHint) {
        try {
            int hour = Integer.parseInt(hourStr);
            int minute = parseMinute(minStr);
            if (periodHint != null) {
                if (("chieu".equals(periodHint) || "toi".equals(periodHint) || "dem".equals(periodHint)) && hour >= 1 && hour <= 11) {
                    hour += 12;
                }
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseMinute(String minStr) {
        if (minStr == null || minStr.isBlank()) {
            return 0;
        }
        int val = Integer.parseInt(minStr.trim());
        return (val >= 0 && val <= 59) ? val : 0;
    }

    private int parseYear(String yearStr, int fallbackYear) {
        int y = Integer.parseInt(yearStr);
        if (y < 100) {
            return 2000 + y;
        }
        return y >= 2020 ? y : fallbackYear;
    }

    private boolean containsAnyPhrase(String normalizedText, List<String> phrases) {
        for (String phrase : phrases) {
            if (ChatbotTextNormalizer.containsWholePhrase(normalizedText, phrase)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildGenreSynonyms() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hanh dong", "hanh dong");
        map.put("action", "hanh dong");
        map.put("vo thuat", "hanh dong");
        map.put("danh nhau", "hanh dong");

        map.put("kinh di", "kinh di");
        map.put("phim ma", "kinh di");
        map.put("horror", "kinh di");
        map.put("ron toc gay", "kinh di");

        map.put("phim hai", "hai huoc");
        map.put("hai huoc", "hai huoc");
        map.put("hai kich", "hai huoc");
        map.put("comedy", "hai huoc");
        map.put("vui nhon", "hai huoc");

        map.put("hoat hinh", "hoat hinh");
        map.put("anime", "hoat hinh");
        map.put("animation", "hoat hinh");
        map.put("cartoon", "hoat hinh");
        map.put("thieu nhi", "hoat hinh");

        map.put("tam ly", "tam ly");
        map.put("tinh cam", "tam ly");
        map.put("lang man", "tam ly");
        map.put("drama", "tam ly");
        map.put("romance", "tam ly");

        map.put("khoa hoc vien tuong", "khoa hoc vien tuong");
        map.put("vien tuong", "khoa hoc vien tuong");
        map.put("sci fi", "khoa hoc vien tuong");
        map.put("scifi", "khoa hoc vien tuong");

        map.put("gia tuong", "gia tuong");
        map.put("phep thuat", "gia tuong");
        map.put("fantasy", "fantasy");

        map.put("phieu luu", "phieu luu");
        map.put("tham hiem", "phieu luu");
        map.put("adventure", "phieu luu");

        map.put("giat gan", "giat gan");
        map.put("hoi hop", "giat gan");
        map.put("thriller", "giat gan");

        map.put("bi an", "bi an");
        map.put("trinh tham", "bi an");
        map.put("mystery", "bi an");

        map.put("hinh su", "hinh su");
        map.put("toi pham", "hinh su");
        map.put("crime", "hinh su");

        map.put("gia dinh", "gia dinh");
        map.put("family", "gia dinh");

        map.put("am nhac", "am nhac");
        map.put("ca nhac", "am nhac");
        map.put("music", "am nhac");

        map.put("lich su", "lich su");
        map.put("co trang", "lich su");
        map.put("history", "lich su");

        map.put("tieu su", "tieu su");
        map.put("tai lieu", "tieu su");
        map.put("biography", "tieu su");
        return Collections.unmodifiableMap(map);
    }
}
