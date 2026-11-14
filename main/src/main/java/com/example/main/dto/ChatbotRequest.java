package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class ChatbotRequest {

    private String message;

    @JsonProperty("conversation_id")
    @JsonAlias({"conversationId", "sessionId", "session_id"})
    private String conversationId;

    private ChatbotStateDto state;

    private List<ChatbotHistoryItemDto> history;

    @JsonProperty("user_location")
    @JsonAlias({"userLocation", "location"})
    private String userLocation;

    private Double latitude;
    private Double longitude;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatbotHistoryItemDto {
        private String role;

        @JsonProperty("content")
        @JsonAlias({"text", "message"})
        private String content;
    }
}

