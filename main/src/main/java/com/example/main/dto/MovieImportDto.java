package com.example.main.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MovieImportDto {
    private String title;
    private String type;

    @JsonProperty("release_date")
    private String releaseDate;

    private List<String> genres;
    private String description;

    @JsonProperty("poster_url")
    private String posterUrl;

    private String trailer;

    @JsonProperty("detail_link")
    private String detailLink;
    private Double rating;

    @JsonProperty("runtime_minutes")
    private Integer runtimeMinutes;

    private List<ShowtimeImportDto> showtimes;

    public List<ShowtimeImportDto> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowtimeImportDto> showtimes) {
        this.showtimes = showtimes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getReleaseDate() {
        return releaseDate;
    }
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
    public List<String> getGenres() {
        return genres;
    }
    public void setGenres(List<String> genres) {
        this.genres = genres;
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
    public String getDetailLink() {
        return detailLink;
    }
    public void setDetailLink(String detailLink) {
        this.detailLink = detailLink;
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
}