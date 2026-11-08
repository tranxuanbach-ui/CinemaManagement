package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ratingRequest {

    @NotNull(message = "ID phim (movieId) không được để trống")
    private Long movieId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm đánh giá tối thiểu là 0.0")
    @DecimalMax(value = "10.0", message = "Điểm đánh giá tối đa là 10.0")
    private Double rating;
}

