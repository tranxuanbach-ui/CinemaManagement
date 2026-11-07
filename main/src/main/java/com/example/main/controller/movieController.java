
package com.example.main.controller;

import com.example.main.dto.MovieRequest;
import com.example.main.dto.MovieResponse;
import com.example.main.service.movieService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class movieController {

    private final movieService movieService;

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
    public ResponseEntity<MovieResponse> createMovie(
            @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movieService.createMovie(request));
    }

    // Cập nhật phim
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable Long id,
            @RequestBody MovieRequest request) {
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    // Xóa phim
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
