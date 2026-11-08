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
public class cinemaSystemResponse {
    private Long id;
    private String name;

    public static cinemaSystemResponse fromEntity(cinemaSystem entity) {
        if (entity == null) return null;
        return cinemaSystemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}