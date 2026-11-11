package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMovieRecommendationDto {

    @JsonProperty("movie_id")
    private Long movieId;

    private String title;
    private Double rating;

    @JsonProperty("poster_url")
    private String posterUrl;

    @JsonProperty("runtime_minutes")
    private Integer runtimeMinutes;

    private List<String> genres;

    @JsonProperty("cinema_showtimes")
    private List<CinemaShowtimeGroupDto> cinemaShowtimes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinemaShowtimeGroupDto {
        @JsonProperty("cinema_id")
        private Long cinemaId;

        @JsonProperty("cinema_name")
        private String cinemaName;

        @JsonProperty("system_name")
        private String systemName;

        private List<ShowtimeSlotDto> schedules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowtimeSlotDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @JsonProperty("date_label")
        private String dateLabel;

        @JsonProperty("busy_badge")
        private String busyBadge;

        private List<String> times;
    }
}

