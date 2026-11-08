package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.ratingRequest;
import com.example.main.dto.ratingResponse;
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
    public ResponseEntity<apiResponse<ratingResponse>> rateMovie(@Valid @RequestBody ratingRequest request) {
        ratingResponse response = userRatingService.createOrUpdateRating(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Đánh giá phim thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<apiResponse<ratingResponse>> updateRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        ratingResponse response = userRatingService.updateRating(id, rating);
        return ResponseEntity.ok(apiResponse.success("Cập nhật đánh giá thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<apiResponse<Void>> deleteRating(@PathVariable Long id) {
        userRatingService.deleteRating(id);
        return ResponseEntity.ok(apiResponse.success("Xóa đánh giá thành công"));
    }

    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<apiResponse<Void>> deleteRatingByMovie(@PathVariable Long movieId) {
        userRatingService.deleteRatingByMovieId(movieId);
        return ResponseEntity.ok(apiResponse.success("Xóa đánh giá phim thành công"));
    }

    @GetMapping("/my")
    public ResponseEntity<apiResponse<List<ratingResponse>>> getMyRatings() {
        List<ratingResponse> responses = userRatingService.getMyRatings();
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách đánh giá của bạn thành công", responses));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<apiResponse<ratingResponse>> getMyRatingForMovie(@PathVariable Long movieId) {
        ratingResponse response = userRatingService.getMyRatingForMovie(movieId);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin đánh giá phim thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<ratingResponse>> getRatingById(@PathVariable Long id) {
        ratingResponse response = userRatingService.getRatingById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy chi tiết đánh giá thành công", response));
    }

    @GetMapping("/all/movie/{movieId}")
    public ResponseEntity<apiResponse<List<ratingResponse>>> getAllRatingsByMovie(@PathVariable Long movieId) {
        List<ratingResponse> responses = userRatingService.getRatingsByMovieId(movieId);
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách đánh giá của phim thành công", responses));
    }
}

