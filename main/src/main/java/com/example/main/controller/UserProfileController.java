package com.example.main.controller;

import com.example.main.dto.ApiResponse;
import com.example.main.dto.ChangePasswordRequest;
import com.example.main.dto.UpdateProfileRequest;
import com.example.main.dto.UserResponse;
import com.example.main.entity.User;
import com.example.main.repository.UserRepository;
import com.example.main.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", UserResponse.fromUser(currentUser)));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        String trimmedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(trimmedEmail, currentUser.getId())) {
            throw new IllegalArgumentException("Email '" + trimmedEmail + "' đã được sử dụng bởi tài khoản khác");
        }

        currentUser.setEmail(trimmedEmail);
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            currentUser.setAvatar_url(request.getAvatarUrl().trim());
        } else {
            currentUser.setAvatar_url("/media/logo.png");
        }

        User saved = userRepository.save(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", UserResponse.fromUser(saved)));
    }

    @PutMapping("/change-password")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }
}

