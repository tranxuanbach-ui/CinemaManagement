
package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.DataImportResponse;
import com.example.main.dto.MovieRequest;
import com.example.main.dto.MovieResponse;
import com.example.main.service.DataImportService;
import com.example.main.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final DataImportService dataImportService;

    // Đồng bộ dữ liệu phim và lịch chiếu từ data_phim_java.json
    @PostMapping("/import-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DataImportResponse>> importDataFromJson() {
        DataImportResponse response = dataImportService.importDataFromJson();
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    // Lấy danh sách phim
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    // Lấy phim theo ID
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    // Thêm phim
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<MovieResponse> createMovie(
            @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movieService.createMovie(request));
    }

    // Cập nhật phim
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody MovieRequest request) {
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
