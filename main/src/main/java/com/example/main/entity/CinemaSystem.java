package com.example.main.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema_system")
public class CinemaSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cinema> cinemas = new ArrayList<>();

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieSystemLink> movieLinks = new ArrayList<>();

    public CinemaSystem() {}

    public CinemaSystem(String name) {
        this.name = name;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Cinema> getCinemas() { return cinemas; }
    public void setCinemas(List<Cinema> cinemas) { this.cinemas = cinemas; }
    public List<MovieSystemLink> getMovieLinks() { return movieLinks; }
    public void setMovieLinks(List<MovieSystemLink> movieLinks) { this.movieLinks = movieLinks; }
}