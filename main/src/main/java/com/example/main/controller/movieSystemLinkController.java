package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.movieSystemLinkRequest;
import com.example.main.dto.movieSystemLinkResponse;
import com.example.main.service.movieSystemLinkService;
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
public class movieSystemLinkController {

    private final movieSystemLinkService linkService;

    // Lọc linh hoạt: GET /api/movie-system-links?movieId=1&systemId=2 (khách tự do xem)
    @GetMapping
    public ResponseEntity<apiResponse<List<movieSystemLinkResponse>>> getAllLinks(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long systemId) {
        List<movieSystemLinkResponse> list = linkService.getAllLinks(movieId, systemId);
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách liên kết phim thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<movieSystemLinkResponse>> getLinkById(@PathVariable Long id) {
        movieSystemLinkResponse response = linkService.getLinkById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin liên kết phim thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<movieSystemLinkResponse>> createLink(
            @Valid @RequestBody movieSystemLinkRequest request) {
        movieSystemLinkResponse created = linkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Tạo liên kết phim thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<movieSystemLinkResponse>> updateLink(
            @PathVariable Long id,
            @Valid @RequestBody movieSystemLinkRequest request) {
        movieSystemLinkResponse updated = linkService.updateLink(id, request);
        return ResponseEntity.ok(apiResponse.success("Cập nhật liên kết phim thành công", updated));
    }

    // Cả ADMIN và STAFF đều có quyền xóa liên kết
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<Void>> deleteLink(@PathVariable Long id) {
        linkService.deleteLink(id);
        return ResponseEntity.ok(apiResponse.success("Xóa liên kết phim thành công"));
    }
}