package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.MovieResponse;
import com.example.main.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/recommendations", "/movies/recommendations", "/api/recommendations", "/api/movies/recommendations"})
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * API Gợi ý top 10 phim dựa trên thể loại từ lịch sử wishlist và rating của người dùng.
     * Tự động áp dụng Cold Start (lấy top phim điểm cao nhất) nếu chưa đăng nhập hoặc là người dùng mới.
     * URL: GET /api/recommendations hoặc GET /api/movies/recommendations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getRecommendations(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<MovieResponse> recommendations = recommendationService.getRecommendations(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gợi ý phim thành công", recommendations));
    }

    /**
     * API Trả về trực tiếp danh sách phim điểm cao nhất trong database (Cold Start).
     * URL: GET /api/recommendations/cold-start
     */
    @GetMapping("/cold-start")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getColdStartRecommendations(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<MovieResponse> recommendations = recommendationService.getColdStartRecommendations(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phim điểm cao nhất (Cold Start) thành công", recommendations));
    }
}

