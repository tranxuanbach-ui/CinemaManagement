package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.MovieSystemLinkRequest;
import com.example.main.dto.MovieSystemLinkResponse;
import com.example.main.service.MovieSystemLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movie-system-links")
@RequiredArgsConstructor
public class MovieSystemLinkController {

    private final MovieSystemLinkService linkService;

    // Lọc linh hoạt: GET /api/movie-system-links?movieId=1&systemId=2 (khách tự do xem)
    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieSystemLinkResponse>>> getAllLinks(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long systemId) {
        List<MovieSystemLinkResponse> list = linkService.getAllLinks(movieId, systemId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách liên kết phim thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieSystemLinkResponse>> getLinkById(@PathVariable Long id) {
        MovieSystemLinkResponse response = linkService.getLinkById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin liên kết phim thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<MovieSystemLinkResponse>> createLink(
            @Valid @RequestBody MovieSystemLinkRequest request) {
        MovieSystemLinkResponse created = linkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo liên kết phim thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<MovieSystemLinkResponse>> updateLink(
            @PathVariable Long id,
            @Valid @RequestBody MovieSystemLinkRequest request) {
        MovieSystemLinkResponse updated = linkService.updateLink(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật liên kết phim thành công", updated));
    }

    // Cả ADMIN và STAFF đều có quyền xóa liên kết
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteLink(@PathVariable Long id) {
        linkService.deleteLink(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa liên kết phim thành công"));
    }
}