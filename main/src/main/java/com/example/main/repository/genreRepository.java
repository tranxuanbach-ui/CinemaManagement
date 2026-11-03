package com.example.main.repository;

import com.example.main.entity.genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface genreRepository extends JpaRepository<genre, Long> {
    // Tìm thể loại theo tên (ví dụ: "Kinh Dị", "Hành Động") để tránh bị trùng lặp khi import dữ liệu
    Optional<genre> findByName(String name);
}