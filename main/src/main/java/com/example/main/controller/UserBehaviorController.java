package com.example.main.controller;

import com.example.main.entity.BehaviorType;
import com.example.main.service.UserBehaviorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/behaviors")
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;

    public UserBehaviorController(UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping("/record")
    public ResponseEntity<String> recordBehavior(
            @RequestParam Long userId,
            @RequestParam Long movieId,
            @RequestParam BehaviorType type) {
        userBehaviorService.recordClickOrView(userId, movieId, type);
        return ResponseEntity.ok("Đã lưu hành vi: " + type + " thành công!");
    }

    @PostMapping("/rate")
    public ResponseEntity<String> recordRating(
            @RequestParam Long userId,
            @RequestParam Long movieId,
            @RequestParam Double score) {
        userBehaviorService.recordRating(userId, movieId, score);
        return ResponseEntity.ok("Đã lưu đánh giá: " + score + " sao thành công!");
    }
}