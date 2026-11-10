package com.example.main.repository;

import com.example.main.entity.MovieSystemLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieSystemLinkRepository extends JpaRepository<MovieSystemLink, Long> {
    List<MovieSystemLink> findByMovieId(Long movieId);
    List<MovieSystemLink> findBySystemId(Long systemId);
    Optional<MovieSystemLink> findByMovieIdAndSystemId(Long movieId, Long systemId);
    boolean existsByMovieIdAndSystemId(Long movieId, Long systemId);

    @Modifying
    @Query("DELETE FROM MovieSystemLink l WHERE l.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}