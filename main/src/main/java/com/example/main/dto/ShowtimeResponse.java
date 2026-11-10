package com.example.main.dto;

import com.example.main.entity.Showtime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long cinemaId;
    private String cinemaName;
    private Long systemId;
    private String systemName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate showDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime showTime;

    private String movieVersion;

    public static ShowtimeResponse fromEntity(Showtime entity) {
        if (entity == null) return null;
        Long sysId = null;
        String sysName = null;
        if (entity.getCinema() != null && entity.getCinema().getSystem() != null) {
            sysId = entity.getCinema().getSystem().getId();
            sysName = entity.getCinema().getSystem().getName();
        }

        return ShowtimeResponse.builder()
                .id(entity.getId())
                .movieId(entity.getMovie() != null ? entity.getMovie().getId() : null)
                .movieTitle(entity.getMovie() != null ? entity.getMovie().getTitle() : null)
                .cinemaId(entity.getCinema() != null ? entity.getCinema().getId() : null)
                .cinemaName(entity.getCinema() != null ? entity.getCinema().getName() : null)
                .systemId(sysId)
                .systemName(sysName)
                .showDate(entity.getShowDate())
                .showTime(entity.getShowTime())
                .movieVersion(entity.getMovieVersion())
                .build();
    }
}