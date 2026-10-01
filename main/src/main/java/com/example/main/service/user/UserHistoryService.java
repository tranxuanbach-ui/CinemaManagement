package com.example.main.service.user;

import com.example.main.dto.user.UserHistoryDto;
import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;
import com.example.main.entity.user.UserHistory;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserHistoryRepository;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserHistoryService {
    private final UserHistoryRepository userHistoryRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserHistoryService(UserHistoryRepository userHistoryRepository, UserRepository userRepository, MovieRepository movieRepository) {
        this.userHistoryRepository = userHistoryRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public UserHistory createHistory(UserHistoryDto request) {
        User user = userRepository.getReferenceById(request.getUser().getId());
        Movie movie = movieRepository.getReferenceById(request.getMovie().getId());

        UserHistory history = new UserHistory();
        history.setUser(user);
        history.setMovie(movie);
        history.setWatchedAt(request.getWatchedAt() != null ? request.getWatchedAt() : LocalDate.now());

        return userHistoryRepository.save(history);
    }

    public List<UserHistory> getAllHistories() {
        return userHistoryRepository.findAll();
    }

    public UserHistory getHistoryById(Long id) {
        return userHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User history not found with id: " + id));
    }

    public List<UserHistory> getHistoriesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return userHistoryRepository.findByUserId(userId);
    }

    @Transactional
    public UserHistory updateHistory(Long id, UserHistoryDto request) {
        UserHistory history = getHistoryById(id);

        User user = userRepository.getReferenceById(request.getUser().getId());
        Movie movie = movieRepository.getReferenceById(request.getMovie().getId());

        history.setUser(user);
        history.setMovie(movie);
        if (request.getWatchedAt() != null) {
            history.setWatchedAt(request.getWatchedAt());
        }

        return userHistoryRepository.save(history);
    }

    public void deleteHistory(Long id) {
        if (!userHistoryRepository.existsById(id)) {
            throw new RuntimeException("User history not found with id: " + id);
        }
        userHistoryRepository.deleteById(id);
    }
}