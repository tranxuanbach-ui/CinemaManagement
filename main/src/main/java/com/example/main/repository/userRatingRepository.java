package com.example.main.repository;

import com.example.main.entity.userRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface userRatingRepository extends JpaRepository<userRating, Long> {
    Optional<userRating> findByUserIdAndMovieId(Long userId, Long movieId);
    List<userRating> findByUserId(Long userId);
    List<userRating> findByMovieId(Long movieId);
}
