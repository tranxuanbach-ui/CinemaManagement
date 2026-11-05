package com.example.main.service;

import com.example.main.dto.LoginRequest;
import com.example.main.dto.RegisterRequest;
import com.example.main.dto.UserResponse;
import com.example.main.entity.user;
import com.example.main.enums.role;
import com.example.main.repository.userRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class authService {

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String trimmedUsername = request.getUsername().trim();
        String trimmedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + trimmedUsername + "' đã tồn tại");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email '" + trimmedEmail + "' đã được sử dụng");
        }

        user newUser = user.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role.USER)
                .enabled(true)
                .build();

        user savedUser = userRepository.save(newUser);
        return UserResponse.fromUser(savedUser);
    }

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Thực hiện xác thực thông qua AuthenticationManager và customUserDetailsService
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        // Lưu thông tin xác thực vào SecurityContext và HttpSession (JSESSIONID)
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        // Lấy thông tin user hiện tại từ database
        String principalUsername = authentication.getName();
        user currentUser = userRepository.findByUsername(principalUsername)
                .or(() -> userRepository.findByEmail(principalUsername))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy thông tin tài khoản"));

        return UserResponse.fromUser(currentUser);
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, auth);
        }
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Người dùng chưa đăng nhập");
        }

        String username = auth.getName();
        user currentUser = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));

        return UserResponse.fromUser(currentUser);
    }
}

