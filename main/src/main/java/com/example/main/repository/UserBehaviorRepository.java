package com.example.main.repository;

import com.example.main.entity.BehaviorType;
import com.example.main.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    List<UserBehavior> findByUserIdAndBehaviorTypeOrderByCreatedAtDesc(Long userId, BehaviorType type);
    
    boolean existsByUserIdAndMovieIdAndBehaviorType(Long userId, Long movieId, BehaviorType type);
}