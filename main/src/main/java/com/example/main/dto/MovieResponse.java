
package com.example.main.dto;

import java.time.LocalDate;
import java.util.List;

public class movieResponse {

    private Long id;
    private String title;
    private LocalDate releaseDate;
    private String description;
    private String posterUrl;
    private String trailer;
    private Double rating;
    private Integer runtimeMinutes;
    private List<genreResponse> genres;

    public movieResponse() {}

    public movieResponse(Long id, String title, LocalDate releaseDate,
                         String description, String posterUrl,
                         String trailer, Double rating,
                         Integer runtimeMinutes, List<genreResponse> genres) {
        this.id = id;
        this.title = title;
        this.releaseDate = releaseDate;
        this.description = description;
        this.posterUrl = posterUrl;
        this.trailer = trailer;
        this.rating = rating;
        this.runtimeMinutes = runtimeMinutes;
        this.genres = genres;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public void setRuntimeMinutes(Integer runtimeMinutes) {
        this.runtimeMinutes = runtimeMinutes;
    }

    public List<genreResponse> getGenres() {
        return genres;
    }

    public void setGenres(List<genreResponse> genres) {
        this.genres = genres;
    }
}
