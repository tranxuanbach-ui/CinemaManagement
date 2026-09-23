package com.example.main.controller;

import com.example.main.service.MovieImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies/import")
public class MovieImportController {
    private final MovieImportService movieImportService;

    public MovieImportController(MovieImportService movieImportService) {
        this.movieImportService = movieImportService;
    }

    @PostMapping
    public ResponseEntity<String> importMovies() {
        movieImportService.importAllMovies();

        return ResponseEntity.ok("Movies imported successfully!");
    }
}