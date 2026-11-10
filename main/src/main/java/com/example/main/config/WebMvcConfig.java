package com.example.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Tự động thêm tiền tố /api cho tất cả Controller (để Postman/Frontend gọi /api/...)
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Điều hướng http://localhost:8080/admin và /admin/ tới trang admin.html
        registry.addViewController("/admin").setViewName("forward:/admin.html");
        registry.addViewController("/admin/").setViewName("forward:/admin.html");
    }
}
