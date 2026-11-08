package com.example.main.repository;

import com.example.main.entity.cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface cinemaRepository extends JpaRepository<cinema, Long> {
    // Lấy danh sách các rạp thuộc về 1 hệ thống rạp cụ thể (VD: Tất cả rạp của CGV)
    List<cinema> findBySystemId(Long systemId);

    // Kiểm tra trùng tên rạp trong cùng một hệ thống rạp
    boolean existsByNameIgnoreCaseAndSystemId(String name, Long systemId);

    // Tìm kiếm cụm rạp theo tên và hệ thống rạp
    java.util.Optional<cinema> findByNameIgnoreCaseAndSystemId(String name, Long systemId);
}