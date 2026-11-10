package com.example.main.repository;

import com.example.main.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitleIgnoreCase(String title);
    Optional<Movie> findByTitle(String title);
    boolean existsByTitleIgnoreCase(String title);
}