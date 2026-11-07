package com.example.main.dto;

import com.example.main.entity.cinemaSystem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaSystemResponse {
    private Long id;
    private String name;

    public static CinemaSystemResponse fromEntity(cinemaSystem entity) {
        if (entity == null) return null;
        return CinemaSystemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}