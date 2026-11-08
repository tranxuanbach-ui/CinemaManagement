package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class showtimeRequest {

    @NotNull(message = "ID phim (movieId) không được để trống")
    @JsonProperty("movieId")
    private Long movieId;

    @NotNull(message = "ID rạp chiếu (cinemaId) không được để trống")
    @JsonProperty("cinemaId")
    private Long cinemaId;

    @NotNull(message = "Ngày chiếu (showDate) không được để trống (định dạng yyyy-MM-dd)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("showDate")
    private LocalDate showDate;

    @NotNull(message = "Giờ chiếu (showTime) không được để trống (định dạng HH:mm hoặc HH:mm:ss)")
    @JsonFormat(pattern = "HH:mm[:ss]")
    @JsonProperty("showTime")
    private LocalTime showTime;

    @NotBlank(message = "Phiên bản phim (movieVersion) không được để trống")
    @Size(max = 50, message = "Phiên bản phim không được vượt quá 50 ký tự")
    @JsonProperty("movieVersion")
    private String movieVersion;
}