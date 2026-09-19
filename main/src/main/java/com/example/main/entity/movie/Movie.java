package com.example.main.entity.movie;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String tittle;
    private String description;
    private String posterUrl;
    private String trailerUrl;
    //Temporary - dat string de build crawller truoc
    private String genres;
    private String directors;
    private String actors;

}
