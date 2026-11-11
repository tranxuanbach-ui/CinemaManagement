package com.example.main.repository;

import com.example.main.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    @Query("SELECT c FROM Cinema c JOIN FETCH c.system ORDER BY c.system.name ASC, c.name ASC")
    List<Cinema> findAllWithSystem();

    // Lấy danh sách các rạp thuộc về 1 hệ thống rạp cụ thể (VD: Tất cả rạp của CGV)
    List<Cinema> findBySystemId(Long systemId);

    // Kiểm tra trùng tên rạp trong cùng một hệ thống rạp
    boolean existsByNameIgnoreCaseAndSystemId(String name, Long systemId);

    // Tìm kiếm cụm rạp theo tên và hệ thống rạp
    java.util.Optional<Cinema> findByNameIgnoreCaseAndSystemId(String name, Long systemId);
}