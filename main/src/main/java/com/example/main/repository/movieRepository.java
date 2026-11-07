package com.example.main.repository;

import com.example.main.entity.movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface movieRepository extends JpaRepository<movie, Long> {
}