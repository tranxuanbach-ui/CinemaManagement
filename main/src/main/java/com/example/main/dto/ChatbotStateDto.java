package com.example.main.dto;

import com.example.main.enums.ChatbotStep;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotStateDto {

    private String location;

    @JsonProperty("matched_cinema_ids")
    @JsonAlias("matchedCinemaIds")
    @Builder.Default
    private List<Long> matchedCinemaIds = new ArrayList<>();

    @JsonProperty("matched_cinema_names")
    @JsonAlias("matchedCinemaNames")
    @Builder.Default
    private List<String> matchedCinemaNames = new ArrayList<>();

    private String genre;

    @JsonProperty("genre_skipped")
    @JsonAlias("genreSkipped")
    private boolean genreSkipped;

    @JsonProperty("time_slot")
    @JsonAlias("timeSlot")
    private String timeSlot;

    @JsonProperty("time_label")
    @JsonAlias("timeLabel")
    private String timeLabel;

    @JsonProperty("start_time")
    @JsonAlias("startTime")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonProperty("end_time")
    @JsonAlias("endTime")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @JsonProperty("target_date")
    @JsonAlias("targetDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonProperty("weekend_only")
    @JsonAlias("weekendOnly")
    private boolean weekendOnly;

    @JsonProperty("time_skipped")
    @JsonAlias("timeSkipped")
    private boolean timeSkipped;

    @JsonProperty("pending_step")
    @JsonAlias("pendingStep")
    @Builder.Default
    private ChatbotStep pendingStep = ChatbotStep.LOCATION;

    public ChatbotStateDto copy() {
        return ChatbotStateDto.builder()
                .location(this.location)
                .matchedCinemaIds(this.matchedCinemaIds != null ? new ArrayList<>(this.matchedCinemaIds) : new ArrayList<>())
                .matchedCinemaNames(this.matchedCinemaNames != null ? new ArrayList<>(this.matchedCinemaNames) : new ArrayList<>())
                .genre(this.genre)
                .genreSkipped(this.genreSkipped)
                .timeSlot(this.timeSlot)
                .timeLabel(this.timeLabel)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .targetDate(this.targetDate)
                .weekendOnly(this.weekendOnly)
                .timeSkipped(this.timeSkipped)
                .pendingStep(this.pendingStep != null ? this.pendingStep : ChatbotStep.LOCATION)
                .build();
    }
}

