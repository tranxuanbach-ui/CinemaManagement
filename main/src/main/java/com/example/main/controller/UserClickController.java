package com.example.main.controller;

import com.example.main.dto.user.UserClickDto;
import com.example.main.entity.user.UserClick;
import com.example.main.service.user.UserClickService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clicks")
public class UserClickController {

    private final UserClickService userClickService;

    public UserClickController(UserClickService userClickService) {
        this.userClickService = userClickService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UserClick> createUserClick(@RequestBody UserClickDto request) {
        UserClick createdClick = userClickService.createUserClick(request);
        return ResponseEntity.ok(createdClick);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<UserClick>> getAllUserClick() {
        List<UserClick> clicks = userClickService.getAllUserClick();
        return ResponseEntity.ok(clicks);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserClick> getUserClickById(@PathVariable Long id) {
        UserClick click = userClickService.getUserClickById(id);
        return ResponseEntity.ok(click);
    }

    // READ BY USER ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserClick>> getUserClickByUserId(@PathVariable Long userId) {
        List<UserClick> clicks = userClickService.getUserClickByUserId(userId);
        return ResponseEntity.ok(clicks);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<UserClick> updateUserClick(
            @PathVariable Long id,
            @RequestBody UserClickDto request) {
        UserClick updatedClick = userClickService.updateUserClick(id, request);
        return ResponseEntity.ok(updatedClick);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        userClickService.deleteHistory(id);
        return ResponseEntity.noContent().build();
    }
}