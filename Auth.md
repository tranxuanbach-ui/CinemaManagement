# Tài liệu Tính năng Xác thực (Authentication & Authorization)

Hệ thống xác thực cho dự án CinemaManagement được xây dựng trên nền tảng **Spring Boot + Spring Security** theo cơ chế **Session / Cookie (`JSESSIONID`)**, phục vụ cho các REST API.

---

## 1. Tổng quan cơ chế hoạt động

- **Cơ chế:** Quản lý phiên làm việc bằng Session trên máy chủ và Cookie `JSESSIONID` gửi về client.
- **Context Path:** `/api` (cấu hình trong `application.yaml`). Mọi endpoint xác thực đều có tiền tố `/api/auth/`.
- **Mã hóa mật khẩu:** Sử dụng thuật toán `BCryptPasswordEncoder`.
- **Đăng nhập linh hoạt:** Người dùng có thể đăng nhập bằng **Username** hoặc **Email**.

---

## 2. Chi tiết các tệp tin được cập nhật & tạo mới

### 2.1. Cập nhật cấu hình & bảo mật
* **`main/src/main/java/com/example/main/config/securityConfig.java`**:
  - Kích hoạt `@EnableWebSecurity`.
  - Khai báo Bean `AuthenticationManager` để hỗ trợ xác thực người dùng thủ công trong controller/service.
  - Khai báo Bean `SecurityContextRepository` (`HttpSessionSecurityContextRepository`) để lưu `SecurityContext` vào session khi đăng nhập thành công.
  - Tắt CSRF (`http.csrf(csrf -> csrf.disable())`) nhằm cho phép các client (Postman/Frontend) gửi JSON qua phương thức `POST/PUT` mà không bị chặn lỗi `403 Forbidden`.
  - Phân quyền endpoint:
    - `/auth/**`: Mở tự do cho mọi người dùng (`permitAll`).
    - `/admin/**`, `/staff/**`: Yêu cầu quyền `ADMIN` hoặc `STAFF`.
    - `/user/**`: Yêu cầu phải đăng nhập (`authenticated`).
  - Tùy biến `exceptionHandling`: Trả về JSON lỗi `401 Unauthorized` hoặc `403 Forbidden` thay vì chuyển hướng đến trang Form Login HTML mặc định của Spring.
  - Quản lý phiên: `SessionCreationPolicy.IF_REQUIRED`.

* **`main/src/main/java/com/example/main/security/customUserDetailsService.java`**:
  - Cải tiến phương thức `loadUserByUsername`: Cho phép tìm kiếm tài khoản theo cả `username` hoặc `email` (`findByUsername(identifier).or(() -> findByEmail(identifier))`).

---

### 2.2. DTOs (Data Transfer Objects) - Package `com.example.main.dto`
* **`ApiResponse.java`**:
  - Chuẩn hóa định dạng dữ liệu trả về cho toàn bộ API (`success`, `message`, `data`).
* **`RegisterRequest.java`**:
  - Dữ liệu gửi lên khi đăng ký tài khoản.
  - Tích hợp kiểm tra hợp lệ (Bean Validation):
    - `username`: Không được rỗng, độ dài từ 3 đến 50 ký tự.
    - `email`: Không được rỗng, định dạng email chuẩn.
    - `password`: Không được rỗng, độ dài tối thiểu 6 ký tự.
    - `avatarUrl`: Tùy chọn (URL ảnh đại diện).
* **`LoginRequest.java`**:
  - Dữ liệu đăng nhập gồm `username` (hoặc email) và `password`.
* **`UserResponse.java`**:
  - Dữ liệu người dùng trả về cho client sau khi đăng ký/đăng nhập/lấy thông tin thành công (loại trừ thông tin nhạy cảm như mật khẩu).

---

### 2.3. Tầng Nghiệp vụ (Service) - Package `com.example.main.service`
* **`authService.java`**:
  - `register(RegisterRequest)`:
    - Kiểm tra tính duy nhất của `username` và `email`.
    - Mã hóa mật khẩu bằng BCrypt.
    - Gán quyền mặc định là `role.USER` và trạng thái `enabled = true`.
    - Lưu vào cơ sở dữ liệu và trả về `UserResponse`.
  - `login(LoginRequest, HttpServletRequest, HttpServletResponse)`:
    - Xác thực thông tin đăng nhập qua `AuthenticationManager`.
    - Đưa thông tin xác thực vào `SecurityContextHolder`.
    - Lưu ngữ cảnh xác thực vào `HttpSession` thông qua `securityContextRepository.saveContext(...)` để Spring Security sinh cookie `JSESSIONID`.
    - Trả về thông tin người dùng.
  - `logout(HttpServletRequest, HttpServletResponse)`:
    - Hủy xác thực với `SecurityContextLogoutHandler`.
    - Hủy phiên làm việc (`session.invalidate()`) và xóa `SecurityContext`.
  - `getCurrentUser()`:
    - Đọc thông tin người dùng hiện tại từ session/SecurityContext.

---

### 2.4. Tầng Điều khiển (Controller) - Package `com.example.main.controller`
* **`authController.java`**:
  - `@RequestMapping("/auth")` (Đường dẫn thực tế: `/api/auth/...`)
  - `POST /register`: Tiếp nhận đăng ký tài khoản mới.
  - `POST /login`: Đăng nhập hệ thống và trả về Cookie phiên.
  - `POST /logout`: Đăng xuất và hủy phiên làm việc.
  - `GET /me`: Lấy thông tin tài khoản hiện đang đăng nhập từ Session.

---

### 2.5. Xử lý ngoại lệ toàn cục - Package `com.example.main.exception`
* **`GlobalExceptionHandler.java`**:
  - Bắt các ngoại lệ xác thực và trả về thông báo lỗi chuẩn JSON:
    - `MethodArgumentNotValidException`: Trả về chi tiết các trường bị lỗi validate (HTTP 400).
    - `BadCredentialsException`: "Tên đăng nhập hoặc mật khẩu không chính xác" (HTTP 401).
    - `DisabledException`: "Tài khoản của bạn đã bị khóa" (HTTP 403).
    - `IllegalArgumentException`: Trả về lỗi nghiệp vụ như tên đăng nhập/email đã tồn tại (HTTP 400).
    - `Exception`: Bắt các lỗi hệ thống phát sinh khác (HTTP 500).

---

## 3. Danh sách Endpoints & Hướng dẫn kiểm thử

### 3.1. Đăng ký tài khoản (`POST /api/auth/register`)
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "username": "nguyenvana",
    "email": "vana@example.com",
    "password": "password123"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "success": true,
    "message": "Đăng ký tài khoản thành công",
    "data": {
      "id": 1,
      "username": "nguyenvana",
      "email": "vana@example.com",
      "role": "USER",
      "avatarUrl": null,
      "enabled": true,
      "createdAt": "2026-11-05T17:00:00",
      "updatedAt": "2026-11-05T17:00:00"
    }
  }
  ```

---

### 3.2. Đăng nhập (`POST /api/auth/login`)
- **Headers:** `Content-Type: application/json`
- **Body (có thể dùng username hoặc email):**
  ```json
  {
    "username": "nguyenvana",
    "password": "password123"
  }
  ```
- **Response (200 OK):**
  * Kèm header `Set-Cookie: JSESSIONID=...; Path=/api; HttpOnly`
  ```json
  {
    "success": true,
    "message": "Đăng nhập thành công",
    "data": {
      "id": 1,
      "username": "nguyenvana",
      "email": "vana@example.com",
      "role": "USER",
      "avatarUrl": "https://example.com/avatar.jpg",
      "enabled": true
    }
  }
  ```

---

### 3.3. Xem thông tin người dùng hiện tại (`GET /api/auth/me`)
- **Headers:** Tự động gửi Cookie `JSESSIONID` (hoặc gửi qua Header `Cookie: JSESSIONID=...` trên Postman).
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Lấy thông tin người dùng thành công",
    "data": {
      "id": 1,
      "username": "nguyenvana",
      "email": "vana@example.com",
      "role": "USER",
      "avatarUrl": "https://example.com/avatar.jpg",
      "enabled": true
    }
  }
  ```

---

### 3.4. Đăng xuất (`POST /api/auth/logout`)
- **Headers:** Tự động gửi Cookie `JSESSIONID`.
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Đăng xuất thành công",
    "data": null
  }
  ```

