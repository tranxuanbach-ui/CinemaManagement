package com.example.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataImportResponse {
    private String message;
    private int totalJsonMovies;
    private int createdMovies;
    private int updatedMovies;
    private int deletedMovies;
    private int userWishlistsRemoved;
    private int totalShowtimesImported;
    private List<String> deletedMovieTitles;
    private List<String> updatedMovieTitles;
    private List<String> createdMovieTitles;
}

