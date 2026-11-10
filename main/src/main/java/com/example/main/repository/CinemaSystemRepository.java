package com.example.main.repository;

import com.example.main.entity.CinemaSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaSystemRepository extends JpaRepository<CinemaSystem, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<CinemaSystem> findByNameIgnoreCase(String name);
}