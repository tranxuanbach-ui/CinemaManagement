package com.example.main.service;

import com.example.main.entity.BehaviorType;
import com.example.main.entity.User;
import com.example.main.entity.UserBehavior;
import com.example.main.entity.movie.Movie;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserBehaviorRepository;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBehaviorService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserBehaviorService(UserBehaviorRepository userBehaviorRepository, 
                               UserRepository userRepository, 
                               MovieRepository movieRepository) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public void recordClickOrView(Long userId, Long movieId, BehaviorType type) {
        if (type == BehaviorType.RATE) {
            throw new IllegalArgumentException("Hàm này không dùng để lưu đánh giá.");
        }
        
        // Sửa ở đây: Sử dụng getReferenceById thay vì findById để tránh lỗi fetch
        User user = userRepository.getReferenceById(userId);
        Movie movie = movieRepository.getReferenceById(movieId);

        UserBehavior behavior = UserBehavior.builder()
                .user(user)
                .movie(movie)
                .behaviorType(type)
                .build();

        userBehaviorRepository.save(behavior);
    }

    @Transactional
    public void recordRating(Long userId, Long movieId, Double score) {
        // Sửa tương tự ở đây
        User user = userRepository.getReferenceById(userId);
        Movie movie = movieRepository.getReferenceById(movieId);

        UserBehavior behavior = UserBehavior.builder()
                .user(user)
                .movie(movie)
                .behaviorType(BehaviorType.RATE)
                .ratingScore(score)
                .build();

        userBehaviorRepository.save(behavior);
    }
}