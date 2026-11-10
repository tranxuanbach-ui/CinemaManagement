package com.example.main.dto;

import com.example.main.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAdminRequest {

    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    private Role role;

    private Boolean enabled;

    private String avatarUrl;
}

