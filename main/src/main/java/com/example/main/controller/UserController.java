package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.UserAdminRequest;
import com.example.main.dto.UserResponse;
import com.example.main.service.UserService;
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
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> list = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserAdminRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm người dùng thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UserAdminRequest request) {
        UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công", updated));
    }

    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "";
        UserResponse updated = userService.toggleUserStatus(id, currentUsername);
        String msg = Boolean.TRUE.equals(updated.getEnabled())
                ? "Đã kích hoạt tài khoản"
                : "Đã khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "";
        userService.deleteUser(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }
}

