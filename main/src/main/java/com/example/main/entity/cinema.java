package com.example.main.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema")
public class cinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private cinemaSystem system;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<showtime> showtimes = new ArrayList<>();

    public cinema() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public cinemaSystem getSystem() { return system; }
    public void setSystem(cinemaSystem system) { this.system = system; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<showtime> getShowtimes() { return showtimes; }
    public void setShowtimes(List<showtime> showtimes) { this.showtimes = showtimes; }
}