package com.example.main.repository;

import com.example.main.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovieId(Long movieId);
    List<Showtime> findByCinemaId(Long cinemaId);
    List<Showtime> findByShowDate(LocalDate showDate);
    List<Showtime> findByMovieIdAndShowDate(Long movieId, LocalDate showDate);
    List<Showtime> findByCinemaIdAndShowDate(Long cinemaId, LocalDate showDate);
    List<Showtime> findByMovieIdAndCinemaIdAndShowDate(Long movieId, Long cinemaId, LocalDate showDate);

    @Modifying
    @Query("DELETE FROM Showtime s WHERE s.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}