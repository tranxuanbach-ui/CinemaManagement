package com.example.main.controller;

import com.example.main.dto.user.UserHistoryDto;
import com.example.main.entity.user.UserHistory;
import com.example.main.service.user.UserHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/histories")
public class UserHistoryController {

    private final UserHistoryService userHistoryService;

    public UserHistoryController(UserHistoryService userHistoryService) {
        this.userHistoryService = userHistoryService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UserHistory> createHistory(@RequestBody UserHistoryDto request) {
        UserHistory createdHistory = userHistoryService.createHistory(request);
        return ResponseEntity.ok(createdHistory);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<UserHistory>> getAllHistories() {
        List<UserHistory> histories = userHistoryService.getAllHistories();
        return ResponseEntity.ok(histories);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<UserHistory> getHistoryById(@PathVariable Long id) {
        UserHistory history = userHistoryService.getHistoryById(id);
        return ResponseEntity.ok(history);
    }

    // READ BY USER ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserHistory>> getHistoriesByUserId(@PathVariable Long userId) {
        List<UserHistory> histories = userHistoryService.getHistoriesByUserId(userId);
        return ResponseEntity.ok(histories);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<UserHistory> updateHistory(
            @PathVariable Long id,
            @RequestBody UserHistoryDto request) {
        UserHistory updatedHistory = userHistoryService.updateHistory(id, request);
        return ResponseEntity.ok(updatedHistory);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        userHistoryService.deleteHistory(id);
        return ResponseEntity.noContent().build();
    }
}