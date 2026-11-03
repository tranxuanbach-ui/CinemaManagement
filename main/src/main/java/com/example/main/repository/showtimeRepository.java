package com.example.main.repository;

import com.example.main.entity.showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface showtimeRepository extends JpaRepository<showtime, Long> {
    List<showtime> findByMovieIdAndShowDate(Long movieId, LocalDate showDate);
}