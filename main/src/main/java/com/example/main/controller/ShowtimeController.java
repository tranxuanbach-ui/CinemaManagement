package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.ShowtimeRequest;
import com.example.main.dto.ShowtimeResponse;
import com.example.main.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    // Tra cứu lịch chiếu: GET /api/showtimes?movieId=1&cinemaId=2&showDate=2026-11-06 (khách tự do xem)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getAllShowtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate) {
        List<ShowtimeResponse> list = showtimeService.getAllShowtimes(movieId, cinemaId, showDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách suất chiếu thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getShowtimeById(@PathVariable Long id) {
        ShowtimeResponse response = showtimeService.getShowtimeById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin suất chiếu thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> createShowtime(
            @Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse created = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo suất chiếu thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse updated = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật suất chiếu thành công", updated));
    }

    // Cả ADMIN và STAFF đều có quyền xóa lịch chiếu
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa suất chiếu thành công"));
    }
}