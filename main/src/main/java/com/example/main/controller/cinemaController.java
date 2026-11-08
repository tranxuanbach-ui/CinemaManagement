package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.cinemaRequest;
import com.example.main.dto.cinemaResponse;
import com.example.main.service.cinemaService;
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
public class cinemaController {

    private final cinemaService cinemaService;

    // Lấy toàn bộ rạp hoặc lọc theo hệ thống rạp: GET /api/cinemas?systemId=1 (khách tự do xem)
    @GetMapping
    public ResponseEntity<apiResponse<List<cinemaResponse>>> getAllCinemas(
            @RequestParam(required = false) Long systemId) {
        List<cinemaResponse> list = cinemaService.getAllCinemas(systemId);
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách rạp chiếu thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<cinemaResponse>> getCinemaById(@PathVariable Long id) {
        cinemaResponse response = cinemaService.getCinemaById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin rạp chiếu thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<cinemaResponse>> createCinema(
            @Valid @RequestBody cinemaRequest request) {
        cinemaResponse created = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Thêm rạp chiếu thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<cinemaResponse>> updateCinema(
            @PathVariable Long id,
            @Valid @RequestBody cinemaRequest request) {
        cinemaResponse updated = cinemaService.updateCinema(id, request);
        return ResponseEntity.ok(apiResponse.success("Cập nhật rạp chiếu thành công", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<Void>> deleteCinema(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok(apiResponse.success("Xóa rạp chiếu thành công"));
    }
}