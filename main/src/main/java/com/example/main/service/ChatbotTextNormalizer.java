package com.example.main.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ChatbotTextNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private ChatbotTextNormalizer() {
    }

    /**
     * Chuyển chuỗi tiếng Việt về dạng chữ thường, bỏ dấu tiếng Việt, giữ chữ và số để so khớp từ khóa.
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String noMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        String lower = noMarks.toLowerCase(Locale.ROOT);
        String alnum = NON_ALNUM.matcher(lower).replaceAll(" ");
        return MULTI_SPACE.matcher(alnum).replaceAll(" ").trim();
    }

    /**
     * Chuẩn hóa giữ lại dấu hai chấm và dấu gạch chéo để phân tích giờ/ngày chính xác.
     */
    public static String normalizeKeepTimePunctuation(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String noMarks = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        String lower = noMarks.toLowerCase(Locale.ROOT);
        return MULTI_SPACE.matcher(lower).replaceAll(" ").trim();
    }

    /**
     * Kiểm tra cụm từ xuất hiện theo ranh giới từ đầy đủ để tránh khớp nhầm (VD: "hue" vs "thuy khue", "q1" vs "q10").
     */
    public static boolean containsWholePhrase(String normalizedHaystack, String normalizedNeedle) {
        if (normalizedHaystack == null || normalizedHaystack.isBlank()
                || normalizedNeedle == null || normalizedNeedle.isBlank()) {
            return false;
        }
        String paddedHaystack = " " + normalizedHaystack + " ";
        String paddedNeedle = " " + normalizedNeedle.trim() + " ";
        return paddedHaystack.contains(paddedNeedle);
    }
}

