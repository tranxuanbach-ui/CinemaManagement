package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinemaSystemRequest {

    @NotBlank(message = "Tên hệ thống rạp không được để trống")
    @Size(max = 100, message = "Tên hệ thống rạp không được vượt quá 100 ký tự")
    private String name;
}