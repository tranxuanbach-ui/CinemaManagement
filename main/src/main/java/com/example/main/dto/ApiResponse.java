package com.example.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class apiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> apiResponse<T> success(String message, T data) {
        return apiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> apiResponse<T> success(String message) {
        return apiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> apiResponse<T> error(String message) {
        return apiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}

