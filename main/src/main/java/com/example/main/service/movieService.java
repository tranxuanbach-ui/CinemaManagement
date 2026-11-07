
package com.example.main.service;

import com.example.main.dto.MovieRequest;
import com.example.main.dto.MovieResponse;
import com.example.main.dto.GenreResponse;
import com.example.main.entity.movie;
import com.example.main.entity.genre;
import com.example.main.repository.movieRepository;
import com.example.main.repository.genreRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class movieService {

    private final movieRepository movieRepository;
    private final genreRepository genreRepository;

    // Chuyển Entity movie thành MovieResponse
    private MovieResponse toResponse(movie m) {
        List<GenreResponse> genres = m.getGenres()
                .stream()
                .map(g -> new GenreResponse(g.getId(), g.getName()))
                .toList();

        return new MovieResponse(
                m.getId(),
                m.getTitle(),
                m.getReleaseDate(),
                m.getDescription(),
                m.getPosterUrl(),
                m.getTrailer(),
                m.getRating(),
                m.getRuntimeMinutes(),
                genres
        );
    }

    // Tìm danh sách thể loại theo ID
    private Set<genre> getGenres(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }

        if (genreIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("ID thể loại không được để trống");
        }

        Set<Long> ids = new HashSet<>(genreIds);

        List<genre> genres = genreRepository.findAllById(ids);

        if (genres.size() != ids.size()) {
            throw new IllegalArgumentException("Có ID thể loại không tồn tại");
        }

        return new HashSet<>(genres);
    }

    // Lấy danh sách phim
    @Transactional(readOnly = true)
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lấy phim theo ID
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        return toResponse(m);
    }

    // Thêm phim
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên phim không được để trống");
        }

        movie m = new movie();

        m.setTitle(request.getTitle());
        m.setReleaseDate(request.getReleaseDate());
        m.setDescription(request.getDescription());
        m.setPosterUrl(request.getPosterUrl());
        m.setTrailer(request.getTrailer());
        m.setRating(request.getRating());
        m.setRuntimeMinutes(request.getRuntimeMinutes());
        m.setGenres(getGenres(request.getGenreIds()));

        movie savedMovie = movieRepository.save(m);

        return toResponse(savedMovie);
    }

    // Cập nhật phim
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên phim không được để trống");
        }

        movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        m.setTitle(request.getTitle());
        m.setReleaseDate(request.getReleaseDate());
        m.setDescription(request.getDescription());
        m.setPosterUrl(request.getPosterUrl());
        m.setTrailer(request.getTrailer());
        m.setRating(request.getRating());
        m.setRuntimeMinutes(request.getRuntimeMinutes());
        m.setGenres(getGenres(request.getGenreIds()));

        movie updatedMovie = movieRepository.save(m);

        return toResponse(updatedMovie);
    }

    // Xóa phim
    @Transactional
    public void deleteMovie(Long id) {
        movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        movieRepository.delete(m);
    }
}
