package com.example.main.dto;

import com.example.main.entity.userRating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RatingResponse fromEntity(userRating entity) {
        if (entity == null) return null;
        return RatingResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .movieId(entity.getMovie() != null ? entity.getMovie().getId() : null)
                .movieTitle(entity.getMovie() != null ? entity.getMovie().getTitle() : null)
                .moviePosterUrl(entity.getMovie() != null ? entity.getMovie().getPosterUrl() : null)
                .rating(entity.getRating())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

