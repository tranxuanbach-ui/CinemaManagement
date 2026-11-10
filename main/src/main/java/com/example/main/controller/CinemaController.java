package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.CinemaRequest;
import com.example.main.dto.CinemaResponse;
import com.example.main.service.CinemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    // Lấy toàn bộ rạp hoặc lọc theo hệ thống rạp: GET /api/cinemas?systemId=1 (khách tự do xem)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CinemaResponse>>> getAllCinemas(
            @RequestParam(required = false) Long systemId) {
        List<CinemaResponse> list = cinemaService.getAllCinemas(systemId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách rạp chiếu thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getCinemaById(@PathVariable Long id) {
        CinemaResponse response = cinemaService.getCinemaById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin rạp chiếu thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CinemaResponse>> createCinema(
            @Valid @RequestBody CinemaRequest request) {
        CinemaResponse created = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm rạp chiếu thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CinemaResponse>> updateCinema(
            @PathVariable Long id,
            @Valid @RequestBody CinemaRequest request) {
        CinemaResponse updated = cinemaService.updateCinema(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật rạp chiếu thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteCinema(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa rạp chiếu thành công"));
    }
}