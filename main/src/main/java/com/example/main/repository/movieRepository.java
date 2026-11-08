package com.example.main.repository;

import com.example.main.entity.movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface movieRepository extends JpaRepository<movie, Long> {
    Optional<movie> findByTitleIgnoreCase(String title);
    Optional<movie> findByTitle(String title);
    boolean existsByTitleIgnoreCase(String title);
}