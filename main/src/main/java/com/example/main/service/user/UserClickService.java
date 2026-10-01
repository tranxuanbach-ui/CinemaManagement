package com.example.main.service.user;

import com.example.main.dto.user.UserClickDto;
import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;
import com.example.main.entity.user.UserClick;
import com.example.main.repository.MovieRepository;
import com.example.main.repository.UserClickRepository;
import com.example.main.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserClickService {
    private final UserClickRepository userClickRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserClickService(UserClickRepository userClickRepository, UserRepository userRepository, MovieRepository movieRepository) {
        this.userClickRepository = userClickRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    // CREATE
    public UserClick createUserClick(UserClickDto request) {
        User user = userRepository.findById(request.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUser().getId()));

        Movie movie = movieRepository.findById(request.getMovie().getId())
                .orElseThrow(() -> new RuntimeException("Movie not found with ID: " + request.getMovie().getId()));

        UserClick click = new UserClick();
        click.setUser(user);
        click.setMovie(movie);
        // Nếu không truyền ClickedAt từ request, mặc định lấy ngày hiện tại
        click.setClickedAt(request.getClickedAt() != null ? request.getClickedAt() : LocalDate.now());

        return userClickRepository.save(click);
    }

    // READ
    public List<UserClick> getAllUserClick() {
        return userClickRepository.findAll();
    }

    public UserClick getUserClickById(Long id) {
        return userClickRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User click not found with id: " + id));
    }

    public List<UserClick> getUserClickByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return userClickRepository.findByUserId(userId);
    }

    // UPDATE
    @Transactional
    public UserClick updateUserClick(Long id, UserClickDto request) {
        UserClick click = getUserClickById(id);

        User user = userRepository.findById(request.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUser().getId()));

        Movie movie = movieRepository.findById(request.getMovie().getId())
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + request.getMovie().getId()));

        click.setUser(user);
        click.setMovie(movie);

        if (request.getClickedAt() != null) {
            click.setClickedAt(request.getClickedAt());
        }

        return userClickRepository.save(click);
    }

    public void deleteHistory(Long id) {
        if (!userClickRepository.existsById(id)) {
            throw new RuntimeException("User history not found with id: " + id);
        }
        userClickRepository.deleteById(id);
    }
}