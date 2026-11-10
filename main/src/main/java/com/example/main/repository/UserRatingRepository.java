package com.example.main.repository;

import com.example.main.entity.UserRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    Optional<UserRating> findByUserIdAndMovieId(Long userId, Long movieId);
    List<UserRating> findByUserId(Long userId);
    List<UserRating> findByMovieId(Long movieId);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM UserRating r WHERE r.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT AVG(r.rating) FROM UserRating r WHERE r.movie.id = :movieId")
    Double getAverageRatingByMovieId(@Param("movieId") Long movieId);
}
