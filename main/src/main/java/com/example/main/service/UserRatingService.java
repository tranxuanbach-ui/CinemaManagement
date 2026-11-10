package com.example.main.service;

import com.example.main.dto.RatingRequest;
import com.example.main.dto.RatingResponse;
import com.example.main.entity.Movie;
import com.example.main.entity.User;
import com.example.main.entity.UserRating;
import com.example.main.enums.Role;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserRatingRepository;
import com.example.main.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRatingService {

    private final UserRatingRepository userRatingRepository;
    private final MovieRepository movieRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public RatingResponse createOrUpdateRating(RatingRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim với ID: " + request.getMovieId()));

        Optional<UserRating> existingRatingOpt = userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movie.getId());

        UserRating ratingToSave;
        if (existingRatingOpt.isPresent()) {
            ratingToSave = existingRatingOpt.get();
            ratingToSave.setRating(request.getRating());
        } else {
            ratingToSave = new UserRating(currentUser, movie, request.getRating());
        }

        UserRating saved = userRatingRepository.save(ratingToSave);
        updateMovieAverageRating(movie.getId());

        return RatingResponse.fromEntity(saved);
    }

    @Transactional
    public RatingResponse updateRating(Long id, Double newRating) {
        if (newRating == null || newRating < 0.0 || newRating > 10.0) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 0.0 đến 10.0");
        }

        User currentUser = securityUtils.getCurrentUser();

        UserRating rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));

        // Kiểm tra quyền: chỉ người tạo đánh giá hoặc ADMIN mới được chỉnh sửa
        if (!rating.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        rating.setRating(newRating);
        UserRating saved = userRatingRepository.save(rating);

        updateMovieAverageRating(rating.getMovie().getId());

        return RatingResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteRating(Long id) {
        User currentUser = securityUtils.getCurrentUser();

        UserRating rating = userRatingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá với ID: " + id));

        // Kiểm tra quyền: chỉ người tạo đánh giá hoặc ADMIN mới được xóa
        if (!rating.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

        Long movieId = rating.getMovie().getId();
        userRatingRepository.delete(rating);

        updateMovieAverageRating(movieId);
    }

    @Transactional
    public void deleteRatingByMovieId(Long movieId) {
        User currentUser = securityUtils.getCurrentUser();

        UserRating rating = userRatingRepository.findByUserIdAndMovieId(currentUser.getId(), movieId)
                .orElseThrow(() -> new IllegalArgumentException("Bạn chưa đánh giá phim này"));

        userRatingRepository.delete(rating);
        updateMovieAverageRating(movieId);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getMyRatings() {
        User currentUser = securityUtils.getCurrentUser();
        return userRatingRepository.findByUserId(currentUser.getId()).stream()
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRatingForMovie(Long movieId) {
        User currentUser = securityUtils.getCurrentUser();
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

    @Transactional
    public void updateMovieAverageRating(Long movieId) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) return;

        List<UserRating> ratings = userRatingRepository.findByMovieId(movieId);

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
            for (UserRating ur : ratings) {
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

