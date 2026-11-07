package com.example.main.dto;

import com.example.main.entity.userWishList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishListResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private LocalDate releaseDate;
    private LocalDateTime createdAt;

    public static WishListResponse fromEntity(userWishList entity) {
        if (entity == null) return null;
        return WishListResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .username(entity.getUser() != null ? entity.getUser().getUsername() : null)
                .movieId(entity.getMovie() != null ? entity.getMovie().getId() : null)
                .movieTitle(entity.getMovie() != null ? entity.getMovie().getTitle() : null)
                .moviePosterUrl(entity.getMovie() != null ? entity.getMovie().getPosterUrl() : null)
                .releaseDate(entity.getMovie() != null ? entity.getMovie().getReleaseDate() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

