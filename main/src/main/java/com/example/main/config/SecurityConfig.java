package com.example.main.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

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
                // 1. Cho phép tải tự do toàn bộ giao diện tĩnh (HTML, CSS, JS, Ảnh) & trang Admin
                .requestMatchers(
                    "/", "/index.html", "/*.html",
                    "/css/**", "/js/**", "/media/**", "/favicon.ico",
                    "/admin", "/admin/**"
                ).permitAll()

                // 2. Cho phép tự do truy cập các API xác thực
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()

                // 3. Cho phép khách (chưa đăng nhập) xem phim, thể loại, rạp, hệ thống rạp, link rạp, lịch chiếu và đánh giá công khai
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                    "/api/movies/**", "/movies/**",
                    "/api/genres/**", "/genres/**",
                    "/api/cinemas/**", "/cinemas/**",
                    "/api/cinema-systems/**", "/cinema-systems/**",
                    "/api/movie-system-links/**", "/movie-system-links/**",
                    "/api/showtimes/**", "/showtimes/**",
                    "/api/user/ratings/all/**", "/user/ratings/all/**"
                ).permitAll()

                // 4. CHỈ BẮT BUỘC ĐĂNG NHẬP KHI ĐÁNH GIÁ VÀ THEO DÕI (Wishlist & Ratings)
                .requestMatchers(
                    "/api/user/ratings/**", "/user/ratings/**",
                    "/api/user/wishlist/**", "/user/wishlist/**",
                    "/api/user/**", "/user/**"
                ).authenticated()

                // 5. Cả ADMIN và STAFF đều được phép truy cập vào khu vực quản trị & quản lý user
                .requestMatchers(
                    "/api/admin/**", "/api/staff/**", "/admin/**", "/staff/**",
                    "/api/users/**", "/users/**"
                ).hasAnyRole("ADMIN", "STAFF")

                // 6. Bắt buộc đăng nhập với mọi API (POST/PUT/DELETE) còn lại
                .anyRequest().authenticated()
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
                    response.getWriter().write("{\"success\":false,\"message\":\"Bạn không đủ quyền thực hiện thao tác này\"}");
                })
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
