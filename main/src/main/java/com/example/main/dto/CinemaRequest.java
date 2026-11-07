package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class CinemaRequest {

    @NotNull(message = "ID hệ thống rạp (systemId) không được để trống")
    @JsonProperty("systemId")
    private Long systemId;

    @NotBlank(message = "Tên rạp không được để trống")
    @Size(max = 255, message = "Tên rạp không được vượt quá 255 ký tự")
    private String name;
}