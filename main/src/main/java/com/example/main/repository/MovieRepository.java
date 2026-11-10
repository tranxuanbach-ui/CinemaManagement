package com.example.main.repository;

import com.example.main.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitleIgnoreCase(String title);
    Optional<Movie> findByTitle(String title);
    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres")
    List<Movie> findAllWithGenres();

    @Query("SELECT m FROM Movie m ORDER BY COALESCE(m.rating, m.defaultRating, 0.0) DESC, m.releaseDate DESC")
    List<Movie> findTopRatedMovies(Pageable pageable);
}
