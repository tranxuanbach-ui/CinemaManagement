package com.example.main.dto.user;

import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;

import java.time.LocalDate;

public class UserRatingDto {
    private Long id;
    private User user;
    private Movie movie;
    private float rating;
    private LocalDate watchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public LocalDate getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(LocalDate watchedAt) {
        this.watchedAt = watchedAt;
    }
}
