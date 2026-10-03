package com.example.main.controller;

import com.example.main.dto.user.UserRatingDto;
import com.example.main.entity.user.UserRating;
import com.example.main.service.user.UserRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-ratings")
public class UserRatingController {

    private final UserRatingService userRatingService;

    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    // CREATE / UPSERT
    @PostMapping
    public ResponseEntity<UserRating> createUserRating(@RequestBody UserRatingDto request) {
        UserRating createdRating = userRatingService.createUserRating(request);
        return ResponseEntity.ok(createdRating);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<UserRating>> getAllUserRatings() {
        List<UserRating> ratings = userRatingService.getAllUserRatings();
        return ResponseEntity.ok(ratings);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserRating> getUserRatingById(@PathVariable Long id) {
        UserRating rating = userRatingService.getUserRatingById(id);
        return ResponseEntity.ok(rating);
    }

    // READ BY USER ID AND MOVIE ID
    @GetMapping("/user/{userId}/movie/{movieId}")
    public ResponseEntity<UserRating> getUserRatingByUserIdAndMovieId(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        UserRating rating = userRatingService.getUserRatingByUserIdAndMovieId(userId, movieId);
        return ResponseEntity.ok(rating);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<UserRating> updateUserRating(
            @PathVariable Long id,
            @RequestBody UserRatingDto request) {
        UserRating updatedRating = userRatingService.updateUserRating(id, request);
        return ResponseEntity.ok(updatedRating);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserRating(@PathVariable Long id) {
        userRatingService.deleteUserRating(id);
        return ResponseEntity.noContent().build();
    }
}