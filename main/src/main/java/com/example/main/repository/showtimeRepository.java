package com.example.main.repository;

import com.example.main.entity.showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface showtimeRepository extends JpaRepository<showtime, Long> {
    List<showtime> findByMovieId(Long movieId);
    List<showtime> findByCinemaId(Long cinemaId);
    List<showtime> findByShowDate(LocalDate showDate);
    List<showtime> findByMovieIdAndShowDate(Long movieId, LocalDate showDate);
    List<showtime> findByCinemaIdAndShowDate(Long cinemaId, LocalDate showDate);
    List<showtime> findByMovieIdAndCinemaIdAndShowDate(Long movieId, Long cinemaId, LocalDate showDate);
}