package com.example.main.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema_system")
public class cinemaSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<cinema> cinemas = new ArrayList<>();

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<movieSystemLink> movieLinks = new ArrayList<>();

    public cinemaSystem() {}

    public cinemaSystem(String name) {
        this.name = name;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<cinema> getCinemas() { return cinemas; }
    public void setCinemas(List<cinema> cinemas) { this.cinemas = cinemas; }
    public List<movieSystemLink> getMovieLinks() { return movieLinks; }
    public void setMovieLinks(List<movieSystemLink> movieLinks) { this.movieLinks = movieLinks; }
}