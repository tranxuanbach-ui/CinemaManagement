package com.example.main.service;

import com.example.main.dto.ratingRequest;
import com.example.main.dto.ratingResponse;
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
    public ratingResponse createOrUpdateRating(ratingRequest request) {
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

        return ratingResponse.fromEntity(saved);
    }

    @Transactional
    public ratingResponse updateRating(Long id, Double newRating) {
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

        return ratingResponse.fromEntity(saved);
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
    public List<ratingResponse> getMyRatings() {
        user currentUser = securityUtils.getCurrentUser();
        return userRatingRepository.findByUserId(currentUser.getId()).stream()
                .map(ratingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ratingResponse getMyRatingForMovie(Long movieId) {
        user currentUser = securityUtils.getCurrentUser();
        return userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movieId)
                .map(ratingResponse::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ratingResponse> getRatingsByMovieId(Long movieId) {
        return userRatingRepository.findByMovieId(movieId).stream()
                .map(ratingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ratingResponse getRatingById(Long id) {
        securityUtils.getCurrentUser(); // Yêu cầu đăng nhập
        return userRatingRepository.findById(id)
                .map(ratingResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));
    }

    @Transactional
    public void updateMovieAverageRating(Long movieId) {
        movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) return;

        List<userRating> ratings = userRatingRepository.findByMovieId(movieId);

        Double defaultRating = movie.getDefaultRating();
        boolean hasValidDefault = defaultRating != null && defaultRating > 0.0;

        double sum = 0.0;
        int count = 0;

        // Nếu điểm mặc định (đánh giá gốc) khác 0 thì tính vào trung bình cộng
        if (hasValidDefault) {
            sum += defaultRating;
            count++;
        }

        // Cộng tất cả các đánh giá của các user
        if (ratings != null && !ratings.isEmpty()) {
            for (userRating ur : ratings) {
                if (ur.getRating() != null && ur.getRating() > 0.0) {
                    sum += ur.getRating();
                    count++;
                }
            }
        }

        if (count > 0) {
            double avg = sum / count;
            // Làm tròn 1 chữ số thập phân
            movie.setRating(Math.round(avg * 10.0) / 10.0);
        } else {
            // Mặc định 0.0 và chưa có ai đánh giá
            movie.setRating(0.0);
        }

        movieRepository.save(movie);
    }
}

