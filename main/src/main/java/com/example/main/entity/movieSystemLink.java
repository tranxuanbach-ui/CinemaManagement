package com.example.main.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "movie_system_link",
    uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "system_id"})
)
public class movieSystemLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private cinemaSystem system;

    @Column(name = "detail_link", length = 1000, nullable = false)
    private String detailLink;

    public movieSystemLink() {}

    public movieSystemLink(movie movie, cinemaSystem system, String detailLink) {
        this.movie = movie;
        this.system = system;
        this.detailLink = detailLink;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public movie getMovie() { return movie; }
    public void setMovie(movie movie) { this.movie = movie; }
    public cinemaSystem getSystem() { return system; }
    public void setSystem(cinemaSystem system) { this.system = system; }
    public String getDetailLink() { return detailLink; }
    public void setDetailLink(String detailLink) { this.detailLink = detailLink; }
}