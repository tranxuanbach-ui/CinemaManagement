package com.example.main.dto;

import com.example.main.entity.cinema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class cinemaResponse {
    private Long id;
    private String name;
    private Long systemId;
    private String systemName;

    public static cinemaResponse fromEntity(cinema entity) {
        if (entity == null) return null;
        return cinemaResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .systemId(entity.getSystem() != null ? entity.getSystem().getId() : null)
                .systemName(entity.getSystem() != null ? entity.getSystem().getName() : null)
                .build();
    }
}