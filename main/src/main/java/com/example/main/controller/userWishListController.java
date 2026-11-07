package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.WishListRequest;
import com.example.main.dto.WishListResponse;
import com.example.main.service.userWishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/wishlist")
@RequiredArgsConstructor
public class userWishListController {

    private final userWishListService userWishListService;

    @PostMapping
    public ResponseEntity<ApiResponse<WishListResponse>> addToWishList(@Valid @RequestBody WishListRequest request) {
        WishListResponse response = userWishListService.addToWishList(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm phim vào danh sách yêu thích", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishList(@PathVariable Long id) {
        userWishListService.removeFromWishList(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi danh sách yêu thích"));
    }

    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<Void>> removeMovieFromWishList(@PathVariable Long movieId) {
        userWishListService.removeMovieFromWishList(movieId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa phim khỏi danh sách yêu thích"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WishListResponse>>> getMyWishList() {
        List<WishListResponse> responses = userWishListService.getMyWishList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phim yêu thích thành công", responses));
    }

    @GetMapping("/check/{movieId}")
    public ResponseEntity<ApiResponse<Boolean>> checkWishList(@PathVariable Long movieId) {
        boolean isInWishList = userWishListService.isInWishList(movieId);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra danh sách yêu thích thành công", isInWishList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WishListResponse>> getWishListById(@PathVariable Long id) {
        WishListResponse response = userWishListService.getWishListById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết mục yêu thích thành công", response));
    }
}

