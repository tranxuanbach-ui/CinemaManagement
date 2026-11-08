package com.example.main.repository;

import com.example.main.entity.userRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface userRatingRepository extends JpaRepository<userRating, Long> {
    Optional<userRating> findByUserIdAndMovieId(Long userId, Long movieId);
    List<userRating> findByUserId(Long userId);
    List<userRating> findByMovieId(Long movieId);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM userRating r WHERE r.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT AVG(r.rating) FROM userRating r WHERE r.movie.id = :movieId")
    Double getAverageRatingByMovieId(@Param("movieId") Long movieId);
}
