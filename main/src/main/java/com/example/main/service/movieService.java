
package com.example.main.service;

import com.example.main.dto.movieRequest;
import com.example.main.dto.movieResponse;
import com.example.main.dto.genreResponse;
import com.example.main.entity.movie;
import com.example.main.entity.genre;
import com.example.main.repository.movieRepository;
import com.example.main.repository.genreRepository;
import com.example.main.repository.userWishListRepository;
import com.example.main.repository.userRatingRepository;
import com.example.main.repository.showtimeRepository;
import com.example.main.repository.movieSystemLinkRepository;

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
    private final userWishListRepository userWishListRepository;
    private final userRatingRepository userRatingRepository;
    private final showtimeRepository showtimeRepository;
    private final movieSystemLinkRepository movieSystemLinkRepository;

    // Chuyển Entity movie thành movieResponse
    private movieResponse toResponse(movie m) {
        List<genreResponse> genres = m.getGenres()
                .stream()
                .map(g -> new genreResponse(g.getId(), g.getName()))
                .toList();

        return new movieResponse(
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
    public List<movieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Lấy phim theo ID
    @Transactional(readOnly = true)
    public movieResponse getMovieById(Long id) {
        movie m = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim"));

        return toResponse(m);
    }

    // Thêm phim
    @Transactional
    public movieResponse createMovie(movieRequest request) {
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
    public movieResponse updateMovie(Long id, movieRequest request) {
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

    // Xóa phim - Tự động xóa khỏi phần theo dõi của user và các liên kết phụ thuộc
    @Transactional
    public void deleteMovie(Long id) {
        movie m = movieRepository.findById(id)
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
