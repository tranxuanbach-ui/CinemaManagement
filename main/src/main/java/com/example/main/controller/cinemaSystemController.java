package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.cinemaSystemRequest;
import com.example.main.dto.cinemaSystemResponse;
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

    // Lấy toàn bộ hệ thống rạp (khách tự do xem)
    @GetMapping
    public ResponseEntity<apiResponse<List<cinemaSystemResponse>>> getAllSystems() {
        List<cinemaSystemResponse> list = cinemaSystemService.getAllSystems();
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách hệ thống rạp thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<cinemaSystemResponse>> getSystemById(@PathVariable Long id) {
        cinemaSystemResponse response = cinemaSystemService.getSystemById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin hệ thống rạp thành công", response));
    }

    // Chỉ ADMIN và STAFF được thêm mới
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<cinemaSystemResponse>> createSystem(
            @Valid @RequestBody cinemaSystemRequest request) {
        cinemaSystemResponse created = cinemaSystemService.createSystem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Tạo hệ thống rạp thành công", created));
    }

    // Chỉ ADMIN và STAFF được cập nhật
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<cinemaSystemResponse>> updateSystem(
            @PathVariable Long id,
            @Valid @RequestBody cinemaSystemRequest request) {
        cinemaSystemResponse updated = cinemaSystemService.updateSystem(id, request);
        return ResponseEntity.ok(apiResponse.success("Cập nhật hệ thống rạp thành công", updated));
    }

    // Cả ADMIN và STAFF đều có quyền quản lý hệ thống rạp
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<Void>> deleteSystem(@PathVariable Long id) {
        cinemaSystemService.deleteSystem(id);
        return ResponseEntity.ok(apiResponse.success("Xóa hệ thống rạp thành công"));
    }
}