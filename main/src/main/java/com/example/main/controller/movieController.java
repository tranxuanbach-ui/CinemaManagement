
package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.dataImportResponse;
import com.example.main.dto.movieRequest;
import com.example.main.dto.movieResponse;
import com.example.main.service.dataImportService;
import com.example.main.service.movieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class movieController {

    private final movieService movieService;
    private final dataImportService dataImportService;

    // Đồng bộ dữ liệu phim và lịch chiếu từ data_phim_java.json
    @PostMapping("/import-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<dataImportResponse>> importDataFromJson() {
        dataImportResponse response = dataImportService.importDataFromJson();
        return ResponseEntity.ok(apiResponse.success(response.getMessage(), response));
    }

    // Lấy danh sách phim
    @GetMapping
    public ResponseEntity<List<movieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    // Lấy phim theo ID
    @GetMapping("/{id}")
    public ResponseEntity<movieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    // Thêm phim
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<movieResponse> createMovie(
            @RequestBody movieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movieService.createMovie(request));
    }

    // Cập nhật phim
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<movieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody movieRequest request) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    // Xóa phim - Cả ADMIN và STAFF đều có quyền quản lý phim
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
