package com.example.main.service.user;

import com.example.main.dto.user.UserHistoryDto;
import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;
import com.example.main.entity.user.UserHistory;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserHistoryRepository;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;

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

    public UserHistory createHistory(UserHistoryDto request) {
        User user = userRepository.findById(request.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUser().getId()));

        Movie movie = movieRepository.findById(request.getMovie().getId())
                .orElseThrow(() -> new RuntimeException("Movie not found with ID: " + request.getMovie().getId()));

        UserHistory history = new UserHistory();
        history.setUser(user);
        history.setMovie(movie);

        // Nếu không truyền watchedAt từ request, mặc định lấy ngày hiện tại
        history.setWatchedAt(request.getWatchedAt() != null ? request.getWatchedAt() : LocalDate.now());

        return userHistoryRepository.save(history);
    }

    // READ: Lấy toàn bộ lịch sử xem phim của tất cả user
    public List<UserHistory> getAllHistories() {
        return userHistoryRepository.findAll();
    }

    // READ: Lấy chi tiết 1 lịch sử theo ID lịch sử
    public UserHistory getHistoryById(Long id) {
        return userHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User history not found with id: " + id));
    }

    // READ: Lấy danh sách lịch sử xem phim theo User ID (tận dụng method bạn vừa viết ở Repository)
    public List<UserHistory> getHistoriesByUserId(Long userId) {
        // (Tuỳ chọn) Có thể check user có tồn tại trước hay không
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return userHistoryRepository.findByUserId(userId);
    }

    // UPDATE: Cập nhật lịch sử xem phim
    public UserHistory updateHistory(Long id, UserHistoryDto request) {
        UserHistory history = getHistoryById(id);

        User user = userRepository.findById(request.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUser().getId()));

        Movie movie = movieRepository.findById(request.getMovie().getId())
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + request.getMovie().getId()));

        history.setUser(user);
        history.setMovie(movie);
        if (request.getWatchedAt() != null) {
            history.setWatchedAt(request.getWatchedAt());
        }

        return userHistoryRepository.save(history);
    }

    // DELETE: Xóa lịch sử xem phim theo ID
    public void deleteHistory(Long id) {
        if (!userHistoryRepository.existsById(id)) {
            throw new RuntimeException("User history not found with id: " + id);
        }
        userHistoryRepository.deleteById(id);
    }
}