package com.example.main.dto;

import com.example.main.enums.ChatbotStep;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {

    private String reply;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("pending_step")
    private ChatbotStep pendingStep;

    private ChatbotStateDto state;

    @JsonProperty("matched_cinemas")
    private List<ChatbotCinemaDto> matchedCinemas;

    private List<ChatbotMovieRecommendationDto> recommendations;

    @JsonProperty("quick_replies")
    private List<String> quickReplies;

    @JsonProperty("used_high_rating_fallback")
    private boolean usedHighRatingFallback;

    @JsonProperty("used_ideal_busy_schedule")
    private boolean usedIdealBusySchedule;

    @JsonProperty("ai_provider")
    private String aiProvider;
}

