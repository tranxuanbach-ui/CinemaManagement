package com.example.main.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "showtime")
public class showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private cinema cinema;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "show_time", nullable = false)
    private LocalTime showTime;

    @Column(name = "movie_version", length = 50, nullable = false)
    private String movieVersion;

    public showtime() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public movie getMovie() { return movie; }
    public void setMovie(movie movie) { this.movie = movie; }
    public cinema getCinema() { return cinema; }
    public void setCinema(cinema cinema) { this.cinema = cinema; }
    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }
    public LocalTime getShowTime() { return showTime; }
    public void setShowTime(LocalTime showTime) { this.showTime = showTime; }
    public String getMovieVersion() { return movieVersion; }
    public void setMovieVersion(String movieVersion) { this.movieVersion = movieVersion; }
}