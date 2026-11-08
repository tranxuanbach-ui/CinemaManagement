package com.example.main.controller;

import com.example.main.dto.apiResponse;
import com.example.main.dto.loginRequest;
import com.example.main.dto.registerRequest;
import com.example.main.dto.userResponse;
import com.example.main.service.authService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class authController {

    private final authService authService;

    @PostMapping("/register")
    public ResponseEntity<apiResponse<userResponse>> register(@Valid @RequestBody registerRequest request) {
        userResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse.success("Đăng ký tài khoản thành công", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<apiResponse<userResponse>> login(
            @Valid @RequestBody loginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        userResponse userResponse = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(apiResponse.success("Đăng nhập thành công", userResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<apiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(apiResponse.success("Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<apiResponse<userResponse>> getCurrentUser() {
        userResponse userResponse = authService.getCurrentUser();
        return ResponseEntity.ok(apiResponse.success("Lấy thông tin người dùng thành công", userResponse));
    }
}

