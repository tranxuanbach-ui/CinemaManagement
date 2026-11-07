package com.example.main.repository;

import com.example.main.entity.cinemaSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface cinemaSystemRepository extends JpaRepository<cinemaSystem, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<cinemaSystem> findByNameIgnoreCase(String name);
}