package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.userAdminRequest;
import com.example.main.dto.userResponse;
import com.example.main.service.userService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class userController {

    private final userService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<List<userResponse>>> getAllUsers() {
        List<userResponse> list = userService.getAllUsers();
        return ResponseEntity.ok(apiResponse.success("Lấy danh sách người dùng thành công", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<userResponse>> getUserById(@PathVariable Long id) {
        userResponse response = userService.getUserById(id);
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<userResponse>> createUser(
            @Valid @RequestBody userAdminRequest request) {
        userResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Thêm người dùng thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<userResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody userAdminRequest request) {
        userResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(apiResponse.success("Cập nhật thông tin người dùng thành công", updated));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<apiResponse<userResponse>> toggleUserStatus(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "";
        userResponse updated = userService.toggleUserStatus(id, currentUsername);
        String msg = Boolean.TRUE.equals(updated.getEnabled())
                ? "Đã kích hoạt tài khoản"
                : "Đã khóa tài khoản thành công";
        return ResponseEntity.ok(apiResponse.success(msg, updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<apiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "";
        userService.deleteUser(id, currentUsername);
        return ResponseEntity.ok(apiResponse.success("Xóa người dùng thành công"));
    }
}

