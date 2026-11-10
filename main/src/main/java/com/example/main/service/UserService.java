package com.example.main.service;

import com.example.main.dto.UserAdminRequest;
import com.example.main.dto.UserResponse;
import com.example.main.entity.User;
import com.example.main.enums.Role;
import com.example.main.repository.UserRepository;
import com.example.main.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
        return UserResponse.fromUser(u);
    }

    @Transactional
    public UserResponse createUser(UserAdminRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        // Chỉ ADMIN mới có quyền cấp vai trò STAFF hoặc ADMIN cho người dùng
        if (currentUser.getRole() != Role.ADMIN) {
            if (request.getRole() != null && request.getRole() != Role.USER) {
                throw new AccessDeniedException("Bạn không đủ quyền: Chỉ Quản trị viên (ADMIN) mới có quyền cấp vai trò STAFF hoặc ADMIN");
            }
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        String trimmedUsername = request.getUsername().trim();
        String trimmedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Username '" + trimmedUsername + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email '" + trimmedEmail + "' đã được sử dụng");
        }

        // Nếu STAFF tạo thì mặc định luôn là USER thường
        Role assignedRole = (currentUser.getRole() == Role.ADMIN && request.getRole() != null)
                ? request.getRole()
                : Role.USER;

        User newUser = User.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .avatar_url(request.getAvatarUrl())
                .build();

        User saved = userRepository.save(newUser);
        return UserResponse.fromUser(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserAdminRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        // Phân quyền:
        // STAFF quản lý USER; ADMIN quản lý STAFF và toàn hệ thống
        if (currentUser.getRole() != Role.ADMIN) {
            // 1. Chỉ ADMIN mới có quyền cấp hoặc đổi role cho người dùng
            if (request.getRole() != null && request.getRole() != u.getRole()) {
                throw new AccessDeniedException("Bạn không đủ quyền: Chỉ Quản trị viên (ADMIN) mới có quyền cấp hoặc thay đổi vai trò (Role)");
            }
            // 2. Cộng tác viên không được sửa tài khoản Quản trị viên (ADMIN)
            if (u.getRole() == Role.ADMIN) {
                throw new AccessDeniedException("Bạn không đủ quyền: Cộng tác viên không thể chỉnh sửa tài khoản Quản trị viên (ADMIN)");
            }
            // 3. Cộng tác viên không được sửa tài khoản của Cộng tác viên khác (Admin quản lý Staff)
            if (u.getRole() == Role.STAFF && !u.getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn không đủ quyền: Cộng tác viên không thể chỉnh sửa tài khoản của Cộng tác viên khác");
            }
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String trimmedEmail = request.getEmail().trim().toLowerCase();
            userRepository.findByEmail(trimmedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Email '" + trimmedEmail + "' đã được sử dụng bởi tài khoản khác");
                }
            });
            u.setEmail(trimmedEmail);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Chỉ cập nhật role nếu caller là ADMIN
        if (request.getRole() != null) {
            if (currentUser.getRole() == Role.ADMIN) {
                u.setRole(request.getRole());
            }
        }

        if (request.getEnabled() != null) {
            u.setEnabled(request.getEnabled());
        }

        if (request.getAvatarUrl() != null) {
            u.setAvatar_url(request.getAvatarUrl());
        }

        User updated = userRepository.save(u);
        return UserResponse.fromUser(updated);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long id, String currentUsername) {
        User currentUser = securityUtils.getCurrentUser();
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        if (u.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
            throw new IllegalArgumentException("Không thể tự khóa tài khoản của chính mình");
        }

        // STAFF chỉ có quyền khóa tài khoản USER, không có quyền khóa STAFF khác hoặc ADMIN
        if (currentUser.getRole() != Role.ADMIN) {
            if (u.getRole() != Role.USER) {
                throw new AccessDeniedException("Bạn không đủ quyền: Cộng tác viên chỉ có quyền khóa tài khoản Người dùng (USER), không thể khóa Cộng tác viên khác hoặc Quản trị viên");
            }
        }

        u.setEnabled(!Boolean.TRUE.equals(u.getEnabled()));
        User updated = userRepository.save(u);
        return UserResponse.fromUser(updated);
    }

    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User currentUser = securityUtils.getCurrentUser();
        // Staff không có quyền xóa user mà chỉ được khóa
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không đủ quyền: Cộng tác viên không có quyền xóa tài khoản. Bạn chỉ có quyền khóa tài khoản Người dùng!");
        }

        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        if (u.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản đang đăng nhập");
        }

        userRepository.delete(u);
    }
}

