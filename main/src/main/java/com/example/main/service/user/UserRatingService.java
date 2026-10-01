package com.example.main.service.user;

import com.example.main.dto.user.UserRatingDto;
import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;
import com.example.main.entity.user.UserRating;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserRatingRepository;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserRatingService {
    private final UserRatingRepository userRatingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserRatingService(UserRatingRepository userRatingRepository,
                             UserRepository userRepository,
                             MovieRepository movieRepository) {
        this.userRatingRepository = userRatingRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public UserRating createUserRating(UserRatingDto request) {
        User user = userRepository.getReferenceById(request.getUser().getId());
        Movie movie = movieRepository.getReferenceById(request.getMovie().getId());

        UserRating rating = userRatingRepository.findByUserIdAndMovieId(user.getId(), movie.getId())
                .orElse(new UserRating());

        rating.setUser(user);
        rating.setMovie(movie);
        rating.setRating(request.getRating());
        rating.setWatchedAt(request.getWatchedAt() != null ? request.getWatchedAt() : LocalDate.now());

        return userRatingRepository.save(rating);
    }

    public List<UserRating> getAllUserRatings() {
        return userRatingRepository.findAll();
    }

    public UserRating getUserRatingById(Long id) {
        return userRatingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User rating not found with id: " + id));
    }

    public UserRating getUserRatingByUserIdAndMovieId(Long userId, Long movieId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        if (!movieRepository.existsById(movieId)) {
            throw new RuntimeException("Movie not found with id: " + movieId);
        }
        return userRatingRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RuntimeException("User rating not found for user id: " + userId + " and movie id: " + movieId));
    }

    @Transactional
    public UserRating updateUserRating(Long id, UserRatingDto request) {
        UserRating rating = getUserRatingById(id);

        User user = userRepository.getReferenceById(request.getUser().getId());
        Movie movie = movieRepository.getReferenceById(request.getMovie().getId());

        rating.setUser(user);
        rating.setMovie(movie);
        rating.setRating(request.getRating());

        if (request.getWatchedAt() != null) {
            rating.setWatchedAt(request.getWatchedAt());
        }

        return userRatingRepository.save(rating);
    }

    public void deleteUserRating(Long id) {
        if (!userRatingRepository.existsById(id)) {
            throw new RuntimeException("User rating not found with id: " + id);
        }
        userRatingRepository.deleteById(id);
    }
}