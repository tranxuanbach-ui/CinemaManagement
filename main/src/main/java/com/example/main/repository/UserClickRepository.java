package com.example.main.repository;

import com.example.main.entity.user.UserClick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClickRepository extends JpaRepository<UserClick, Long> {
    List<UserClick> findByUserId(Long userId);
}
