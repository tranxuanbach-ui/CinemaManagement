package com.example.main.dto;

import com.example.main.entity.movieSystemLink;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSystemLinkResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long systemId;
    private String systemName;
    private String detailLink;

    public static MovieSystemLinkResponse fromEntity(movieSystemLink entity) {
        if (entity == null) return null;
        return MovieSystemLinkResponse.builder()
                .id(entity.getId())
                .movieId(entity.getMovie() != null ? entity.getMovie().getId() : null)
                .movieTitle(entity.getMovie() != null ? entity.getMovie().getTitle() : null)
                .systemId(entity.getSystem() != null ? entity.getSystem().getId() : null)
                .systemName(entity.getSystem() != null ? entity.getSystem().getName() : null)
                .detailLink(entity.getDetailLink())
                .build();
    }
}