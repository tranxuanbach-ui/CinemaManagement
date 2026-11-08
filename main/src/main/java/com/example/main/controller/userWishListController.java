package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.wishListRequest;
import com.example.main.dto.wishListResponse;
import com.example.main.service.userWishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class userWishListController {

    private final userWishListService userWishListService;

    @PostMapping
    public ResponseEntity<apiResponse<wishListResponse>> addToWishList(@Valid @RequestBody wishListRequest request) {
        wishListResponse response = userWishListService.addToWishList(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Đã thêm phim vào danh sách yêu thích", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<apiResponse<Void>> removeFromWishList(@PathVariable Long id) {
        userWishListService.removeFromWishList(id);
        return ResponseEntity.ok(apiResponse.success("Đã xóa khỏi danh sách yêu thích"));
    }

    @DeleteMapping("/movie/{movieId}")
    public ResponseEntity<apiResponse<Void>> removeMovieFromWishList(@PathVariable Long movieId) {
        userWishListService.removeMovieFromWishList(movieId);
        return ResponseEntity.ok(apiResponse.success("Đã xóa phim khỏi danh sách yêu thích"));
    }

    @GetMapping("/my")
    public ResponseEntity<apiResponse<List<wishListResponse>>> getMyWishList() {
        List<wishListResponse> responses = userWishListService.getMyWishList();
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách phim yêu thích thành công", responses));
    }

    @GetMapping("/check/{movieId}")
    public ResponseEntity<apiResponse<Boolean>> checkWishList(@PathVariable Long movieId) {
        boolean isInWishList = userWishListService.isInWishList(movieId);
        return ResponseEntity.ok(apiResponse.success("Kiểm tra danh sách yêu thích thành công", isInWishList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<wishListResponse>> getWishListById(@PathVariable Long id) {
        wishListResponse response = userWishListService.getWishListById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy chi tiết mục yêu thích thành công", response));
    }
}

