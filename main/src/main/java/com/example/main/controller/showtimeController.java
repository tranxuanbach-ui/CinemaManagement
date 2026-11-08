package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.showtimeRequest;
import com.example.main.dto.showtimeResponse;
import com.example.main.service.showtimeService;
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
public class showtimeController {

    private final showtimeService showtimeService;

    // Tra cứu lịch chiếu: GET /api/showtimes?movieId=1&cinemaId=2&showDate=2026-11-06 (khách tự do xem)
    @GetMapping
    public ResponseEntity<apiResponse<List<showtimeResponse>>> getAllShowtimes(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate) {
        List<showtimeResponse> list = showtimeService.getAllShowtimes(movieId, cinemaId, showDate);
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách suất chiếu thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<apiResponse<showtimeResponse>> getShowtimeById(@PathVariable Long id) {
        showtimeResponse response = showtimeService.getShowtimeById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin suất chiếu thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<showtimeResponse>> createShowtime(
            @Valid @RequestBody showtimeRequest request) {
        showtimeResponse created = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Tạo suất chiếu thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<showtimeResponse>> updateShowtime(
            @PathVariable Long id,
            @Valid @RequestBody showtimeRequest request) {
        showtimeResponse updated = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(apiResponse.success("Cập nhật suất chiếu thành công", updated));
    }

    // Cả ADMIN và STAFF đều có quyền xóa lịch chiếu
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<Void>> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok(apiResponse.success("Xóa suất chiếu thành công"));
    }
}