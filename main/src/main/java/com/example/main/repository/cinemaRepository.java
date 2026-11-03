package com.example.main.repository;

import com.example.main.entity.cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface cinemaRepository extends JpaRepository<cinema, Long> {
    Optional<cinema> findByName(String name);
}