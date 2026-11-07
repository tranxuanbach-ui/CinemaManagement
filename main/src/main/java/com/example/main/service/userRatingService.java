package com.example.main.service;

import com.example.main.dto.RatingRequest;
import com.example.main.dto.RatingResponse;
import com.example.main.entity.movie;
import com.example.main.entity.user;
import com.example.main.entity.userRating;
import com.example.main.enums.role;
import com.example.main.repository.movieRepository;
import com.example.main.repository.userRatingRepository;
import com.example.main.security.securityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class userRatingService {

    private final userRatingRepository userRatingRepository;
    private final movieRepository movieRepository;
    private final securityUtils securityUtils;

    @Transactional
    public RatingResponse createOrUpdateRating(RatingRequest request) {
        user currentUser = securityUtils.getCurrentUser();

        movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        Optional<userRating> existingRatingOpt = userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movie.getId());

        userRating ratingToSave;
        if (existingRatingOpt.isPresent()) {
            ratingToSave = existingRatingOpt.get();
            ratingToSave.setRating(request.getRating());
        } else {
            ratingToSave = new userRating(currentUser, movie, request.getRating());
        }

        userRating saved = userRatingRepository.save(ratingToSave);
        updateMovieAverageRating(movie.getId());

        return RatingResponse.fromEntity(saved);
    }

    @Transactional
    public RatingResponse updateRating(Long id, Double newRating) {
        if (newRating == null || newRating < 0.0 || newRating > 10.0) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 0.0 đến 10.0");
        }

        user currentUser = securityUtils.getCurrentUser();

        userRating rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));

        // Kiểm tra quyền: chỉ người tạo đánh giá hoặc ADMIN mới được chỉnh sửa
        if (!rating.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        rating.setRating(newRating);
        userRating saved = userRatingRepository.save(rating);

        updateMovieAverageRating(rating.getMovie().getId());

        return RatingResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteRating(Long id) {
        user currentUser = securityUtils.getCurrentUser();

        userRating rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));

        // Kiểm tra quyền: chỉ người tạo đánh giá hoặc ADMIN mới được xóa
        if (!rating.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

        Long movieId = rating.getMovie().getId();
        userRatingRepository.delete(rating);

        updateMovieAverageRating(movieId);
    }

    @Transactional
    public void deleteRatingByMovieId(Long movieId) {
        user currentUser = securityUtils.getCurrentUser();

        userRating rating = userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movieId)
                .orElseThrow(() -> new IllegalArgumentException("Bạn chưa đánh giá phim này"));

        userRatingRepository.delete(rating);
        updateMovieAverageRating(movieId);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getMyRatings() {
        user currentUser = securityUtils.getCurrentUser();
        return userRatingRepository.findByUserId(currentUser.getId()).stream()
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRatingForMovie(Long movieId) {
        user currentUser = securityUtils.getCurrentUser();
        return userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movieId)
                .map(RatingResponse::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsByMovieId(Long movieId) {
        return userRatingRepository.findByMovieId(movieId).stream()
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingResponse getRatingById(Long id) {
        securityUtils.getCurrentUser(); // Yêu cầu đăng nhập
        return userRatingRepository.findById(id)
                .map(RatingResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));
    }

    private void updateMovieAverageRating(Long movieId) {
        Double avgRating = userRatingRepository.getAverageRatingByMovieId(movieId);
        movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie != null) {
            if (avgRating != null) {
                // Làm tròn 1 chữ số thập phân
                movie.setRating(Math.round(avgRating * 10.0) / 10.0);
            } else {
                movie.setRating(null);
            }
            movieRepository.save(movie);
        }
    }
}

