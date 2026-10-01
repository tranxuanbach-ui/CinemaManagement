package com.example.main.repository;

import com.example.main.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
   boolean existsByUsername(String username);
}
