package com.example.main.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movie")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(name = "trailer", length = 1000)
    private String trailer;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "default_rating")
    private Double defaultRating;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genre",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Showtime> showtimes = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieSystemLink> systemLinks = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRating> userRatings = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserWishList> wishListedBy = new ArrayList<>();

    public Movie() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getTrailer() { return trailer; }
    public void setTrailer(String trailer) { this.trailer = trailer; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Double getDefaultRating() { return defaultRating; }
    public void setDefaultRating(Double defaultRating) { this.defaultRating = defaultRating; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }
    public Set<Genre> getGenres() { return genres; }
    public void setGenres(Set<Genre> genres) { this.genres = genres; }
    public List<Showtime> getShowtimes() { return showtimes; }
    public void setShowtimes(List<Showtime> showtimes) { this.showtimes = showtimes; }
    public List<MovieSystemLink> getSystemLinks() { return systemLinks; }
    public void setSystemLinks(List<MovieSystemLink> systemLinks) { this.systemLinks = systemLinks; }
    public List<UserRating> getUserRatings() { return userRatings; }
    public void setUserRatings(List<UserRating> userRatings) { this.userRatings = userRatings; }
    public List<UserWishList> getWishListedBy() { return wishListedBy; }
    public void setWishListedBy(List<UserWishList> wishListedBy) { this.wishListedBy = wishListedBy; }
}