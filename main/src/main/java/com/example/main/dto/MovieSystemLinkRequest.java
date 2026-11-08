package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class movieSystemLinkRequest {

    @NotNull(message = "ID phim (movieId) không được để trống")
    private Long movieId;

    @NotNull(message = "ID hệ thống rạp (systemId) không được để trống")
    private Long systemId;

    @NotBlank(message = "Đường dẫn chi tiết (detailLink) không được để trống")
    @Size(max = 1000, message = "Đường dẫn không được vượt quá 1000 ký tự")
    private String detailLink;
}