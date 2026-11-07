package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.RatingRequest;
import com.example.main.dto.RatingResponse;
import com.example.main.service.userRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/ratings")
@RequiredArgsConstructor
public class userRatingController {

    private final userRatingService userRatingService;

    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> rateMovie(@Valid @RequestBody RatingRequest request) {
        RatingResponse response = userRatingService.createOrUpdateRating(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá phim thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        RatingResponse response = userRatingService.updateRating(id, rating);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRating(@PathVariable Long id) {
        userRatingService.deleteRating(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công"));
    }

    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<Void>> deleteRatingByMovie(@PathVariable Long movieId) {
        userRatingService.deleteRatingByMovieId(movieId);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá phim thành công"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getMyRatings() {
        List<RatingResponse> responses = userRatingService.getMyRatings();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của bạn thành công", responses));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<RatingResponse>> getMyRatingForMovie(@PathVariable Long movieId) {
        RatingResponse response = userRatingService.getMyRatingForMovie(movieId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đánh giá phim thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RatingResponse>> getRatingById(@PathVariable Long id) {
        RatingResponse response = userRatingService.getRatingById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đánh giá thành công", response));
    }

    @GetMapping("/all/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getAllRatingsByMovie(@PathVariable Long movieId) {
        List<RatingResponse> responses = userRatingService.getRatingsByMovieId(movieId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của phim thành công", responses));
    }
}

