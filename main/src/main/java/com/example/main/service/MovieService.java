
package com.example.main.service;

import com.example.main.dto.MovieRequest;
import com.example.main.dto.MovieResponse;
import com.example.main.dto.GenreResponse;
import com.example.main.entity.Movie;
import com.example.main.entity.Genre;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.GenreRepository;
import com.example.main.repository.UserWishListRepository;
import com.example.main.repository.UserRatingRepository;
import com.example.main.repository.ShowtimeRepository;
import com.example.main.repository.MovieSystemLinkRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final UserWishListRepository userWishListRepository;
    private final UserRatingRepository userRatingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieSystemLinkRepository movieSystemLinkRepository;

    // Chuyển Entity movie thành movieResponse
    private MovieResponse toResponse(Movie m) {
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
    private Set<Genre> getGenres(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }

        if (genreIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("ID thể loại không được để trống");
        }

        Set<Long> ids = new HashSet<>(genreIds);

        List<Genre> genres = genreRepository.findAllById(ids);

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
        Movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        return toResponse(m);
    }

    // Thêm phim
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên phim không được để trống");
        }

        Movie m = new Movie();

        m.setTitle(request.getTitle());
        m.setReleaseDate(request.getReleaseDate());
        m.setDescription(request.getDescription());
        m.setPosterUrl(request.getPosterUrl());
        m.setTrailer(request.getTrailer());
        m.setRating(request.getRating());
        m.setRuntimeMinutes(request.getRuntimeMinutes());
        m.setGenres(getGenres(request.getGenreIds()));

        Movie savedMovie = movieRepository.save(m);

        return toResponse(savedMovie);
    }

    // Cập nhật phim
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên phim không được để trống");
        }

        Movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        m.setTitle(request.getTitle());
        m.setReleaseDate(request.getReleaseDate());
        m.setDescription(request.getDescription());
        m.setPosterUrl(request.getPosterUrl());
        m.setTrailer(request.getTrailer());
        m.setRating(request.getRating());
        m.setRuntimeMinutes(request.getRuntimeMinutes());
        m.setGenres(getGenres(request.getGenreIds()));

        Movie updatedMovie = movieRepository.save(m);

        return toResponse(updatedMovie);
    }

    // Xóa phim - Tự động xóa khỏi phần theo dõi của user và các liên kết phụ thuộc
    @Transactional
    public void deleteMovie(Long id) {
        Movie m = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + id));

        // 1. Tự động xóa phim khỏi phần theo dõi (wishlist) của tất cả người dùng
        userWishListRepository.deleteByMovieId(id);

        // 2. Xóa các đánh giá của người dùng
        userRatingRepository.deleteByMovieId(id);

        // 3. Xóa các suất chiếu
        showtimeRepository.deleteByMovieId(id);

        // 4. Xóa các liên kết hệ thống rạp
        movieSystemLinkRepository.deleteByMovieId(id);

        // 5. Xóa quan hệ thể loại
        if (m.getGenres() != null) {
            m.getGenres().clear();
        }

        // 6. Xóa phim khỏi database
        movieRepository.delete(m);
    }
}
