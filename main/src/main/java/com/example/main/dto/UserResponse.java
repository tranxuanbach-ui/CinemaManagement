package com.example.main.dto;

import com.example.main.entity.user;
import com.example.main.enums.role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class userResponse {
    private Long id;
    private String username;
    private String email;
    private role role;
    private String avatarUrl;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static userResponse fromUser(user u) {
        if (u == null) return null;
        return userResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .avatarUrl(u.getAvatar_url())
                .enabled(u.getEnabled())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}

