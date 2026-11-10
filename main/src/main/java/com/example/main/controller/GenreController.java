
package com.example.main.controller;

import com.example.main.dto.GenreRequest;
import com.example.main.dto.GenreResponse;
import com.example.main.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    // 1. Lấy danh sách thể loại
    @GetMapping
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    // 2. Lấy thể loại theo ID
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getGenreById(
            @PathVariable Long id) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }

    // 3. Thêm thể loại
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<GenreResponse> createGenre(
            @RequestBody GenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(genreService.createGenre(request));
    }

    // 4. Cập nhật thể loại
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<GenreResponse> updateGenre(
            @PathVariable Long id,
            @RequestBody GenreRequest request) {
        return ResponseEntity.ok(
                genreService.updateGenre(id, request));
    }

    // 5. Xóa thể loại - Cả ADMIN và STAFF đều có quyền quản lý thể loại
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteGenre(
            @PathVariable Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}
