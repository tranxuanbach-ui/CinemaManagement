package com.example.main.dto.user;

import com.example.main.entity.movie.Movie;
import com.example.main.entity.user.User;

import java.time.LocalDate;

public class UserClickDto {
    private Long id;
    private User user;
    private Movie movie;
    private LocalDate clickedAt;

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

    public LocalDate getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDate clickedAt) {
        this.clickedAt = clickedAt;
    }
}
