package com.example.main.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema")
public class Cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private CinemaSystem system;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Showtime> showtimes = new ArrayList<>();

    public Cinema() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CinemaSystem getSystem() { return system; }
    public void setSystem(CinemaSystem system) { this.system = system; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Showtime> getShowtimes() { return showtimes; }
    public void setShowtimes(List<Showtime> showtimes) { this.showtimes = showtimes; }
}