package com.example.main.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class securityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF để cho phép các REST API POST/PUT gửi body dạng JSON hoạt động
            .csrf(csrf -> csrf.disable())

            // Cấu hình lưu trữ phiên đăng nhập trong HttpSession
            .securityContext(context -> context.securityContextRepository(securityContextRepository()))

            .authorizeHttpRequests(auth -> auth
                // Cho phép tự do truy cập các API xác thực (register, login, logout, me, ...)
                .requestMatchers("/auth/**").permitAll()

                // Cả ADMIN và STAFF đều được phép truy cập vào khu vực quản trị
                .requestMatchers("/admin/**", "/staff/**").hasAnyRole("ADMIN", "STAFF")

                // Các API người dùng yêu cầu phải đăng nhập
                .requestMatchers("/user/**").authenticated()

                // Mọi request còn lại cho phép truy cập
                .anyRequest().permitAll()
            )

            // Trả về JSON khi chưa đăng nhập hoặc không đủ quyền thay vì chuyển hướng trang HTML
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"message\":\"Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này\"}");
                })
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
