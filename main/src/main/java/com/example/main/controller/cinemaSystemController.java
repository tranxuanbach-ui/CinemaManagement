package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.CinemaSystemRequest;
import com.example.main.dto.CinemaSystemResponse;
import com.example.main.service.cinemaSystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cinema-systems")
@RequiredArgsConstructor
public class cinemaSystemController {

    private final cinemaSystemService cinemaSystemService;

    // Tất cả user đã đăng nhập đều xem được
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CinemaSystemResponse>>> getAllSystems() {
        List<CinemaSystemResponse> list = cinemaSystemService.getAllSystems();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hệ thống rạp thành công", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CinemaSystemResponse>> getSystemById(@PathVariable Long id) {
        CinemaSystemResponse response = cinemaSystemService.getSystemById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hệ thống rạp thành công", response));
    }

    // Chỉ ADMIN và STAFF được thêm mới
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CinemaSystemResponse>> createSystem(
            @Valid @RequestBody CinemaSystemRequest request) {
        CinemaSystemResponse created = cinemaSystemService.createSystem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo hệ thống rạp thành công", created));
    }

    // Chỉ ADMIN và STAFF được cập nhật
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<CinemaSystemResponse>> updateSystem(
            @PathVariable Long id,
            @Valid @RequestBody CinemaSystemRequest request) {
        CinemaSystemResponse updated = cinemaSystemService.updateSystem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hệ thống rạp thành công", updated));
    }

    // Chỉ ADMIN được xóa hệ thống rạp (hoặc thêm 'STAFF' nếu nhóm bạn muốn)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystem(@PathVariable Long id) {
        cinemaSystemService.deleteSystem(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa hệ thống rạp thành công"));
    }
}