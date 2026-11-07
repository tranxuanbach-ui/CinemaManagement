# Tài liệu Tính năng Đánh giá phim (Rating) và Danh sách yêu thích (Wishlist)

Tài liệu này ghi chép chi tiết về việc xây dựng chức năng CRUD cho **Đánh giá phim (`userRating`)** và **Danh sách phim yêu thích (`userWishList`)**, cùng cơ chế bảo mật bắt buộc đăng nhập (Session/Cookie) trong dự án **CinemaManagement**.

---

## 1. Cơ chế Bảo mật & Phân quyền

- **Yêu cầu đăng nhập bắt buộc:**
  - Tất cả các endpoint được định tuyến dưới đường dẫn `/api/user/**`.
  - Trong `securityConfig.java`, quy tắc `.requestMatchers("/user/**").authenticated()` đảm bảo mọi request chưa đăng nhập đều bị chặn ngay tại bộ lọc của Spring Security và trả về mã lỗi **`401 Unauthorized`**.
- **Chống giả mạo người dùng (`securityUtils`):**
  - Không truyền `userId` qua Request Body hoặc URL parameter để tránh việc người dùng thao túng ID của người khác.
  - Server tự động trích xuất thông tin người dùng đang đăng nhập thông qua `SecurityContextHolder.getContext().getAuthentication()`.
- **Kiểm soát quyền sở hữu (Ownership Validation):**
  - Khi cập nhật hoặc xóa đánh giá / danh sách yêu thích, hệ thống kiểm tra: chỉ **chủ sở hữu** của bản ghi hoặc tài khoản có quyền **`ADMIN`** mới được phép thực hiện. Nếu vi phạm sẽ nhận mã lỗi **`403 Forbidden`**.

---

## 2. Chi tiết các tệp tin đã tạo và cập nhật

### 2.1. Tiện ích & Ngoại lệ
* **`main/src/main/java/com/example/main/security/securityUtils.java`** *(Tạo mới)*:
  - Hàm `getCurrentUser()`: Lấy đối tượng `user` trong DB tương ứng với phiên đăng nhập hiện tại. Ném `AccessDeniedException` nếu chưa đăng nhập hoặc không tìm thấy tài khoản.
* **`main/src/main/java/com/example/main/exception/GlobalExceptionHandler.java`** *(Cập nhật)*:
  - Bổ sung `@ExceptionHandler(AccessDeniedException.class)` để trả về phản hồi JSON chuẩn với mã **`403 Forbidden`** khi không đủ quyền.

### 2.2. Tầng Repository
* **`main/src/main/java/com/example/main/repository/userRatingRepository.java`** *(Cập nhật)*:
  - Bổ sung `boolean existsByUserIdAndMovieId(Long userId, Long movieId)`
  - Bổ sung `void deleteByUserIdAndMovieId(Long userId, Long movieId)`
  - Bổ sung câu truy vấn tính điểm trung bình của phim:
    ```java
    @Query("SELECT AVG(r.rating) FROM userRating r WHERE r.movie.id = :movieId")
    Double getAverageRatingByMovieId(@Param("movieId") Long movieId);
    ```

### 2.3. DTOs (Data Transfer Objects) - Package `com.example.main.dto`
* **`RatingRequest.java`** *(Tạo mới)*:
  - `movieId` (`@NotNull`)
  - `rating` (`@NotNull`, `@DecimalMin("0.0")`, `@DecimalMax("10.0")`)
* **`RatingResponse.java`** *(Tạo mới)*:
  - Chứa thông tin: `id`, `userId`, `username`, `movieId`, `movieTitle`, `moviePosterUrl`, `rating`, `createdAt`, `updatedAt`.
* **`WishListRequest.java`** *(Tạo mới)*:
  - `movieId` (`@NotNull`)
* **`WishListResponse.java`** *(Tạo mới)*:
  - Chứa thông tin: `id`, `userId`, `username`, `movieId`, `movieTitle`, `moviePosterUrl`, `releaseDate`, `createdAt`.

### 2.4. Tầng Nghiệp vụ (Service) - Package `com.example.main.service`
* **`userRatingService.java`** *(Tạo mới)*:
  - `createOrUpdateRating(RatingRequest)`: Thêm mới hoặc cập nhật đánh giá (nếu đã từng đánh giá phim này). Đồng thời tự động tính toán lại điểm đánh giá trung bình và cập nhật cột `rating` trong bảng `movie`.
  - `updateRating(Long id, Double newRating)`: Cập nhật điểm đánh giá theo ID bản ghi (kiểm tra quyền sở hữu).
  - `deleteRating(Long id)`: Xóa đánh giá theo ID bản ghi (kiểm tra quyền sở hữu và cập nhật lại điểm phim).
  - `deleteRatingByMovieId(Long movieId)`: Xóa đánh giá của user hiện tại cho một phim cụ thể.
  - `getMyRatings()`: Lấy danh sách toàn bộ các đánh giá của user đang đăng nhập.
  - `getMyRatingForMovie(Long movieId)`: Lấy đánh giá của user cho một bộ phim cụ thể.
  - `getRatingsByMovieId(Long movieId)`: Lấy tất cả đánh giá của người dùng đối với một bộ phim.
  - `getRatingById(Long id)`: Lấy chi tiết đánh giá theo ID.

* **`userWishListService.java`** *(Tạo mới)*:
  - `addToWishList(WishListRequest)`: Thêm phim vào danh sách yêu thích (kiểm tra trùng lặp: nếu đã thêm trước đó sẽ báo lỗi).
  - `removeFromWishList(Long wishListId)`: Xóa khỏi danh sách theo ID mục yêu thích (kiểm tra quyền sở hữu).
  - `removeMovieFromWishList(Long movieId)`: Xóa nhanh khỏi danh sách theo ID phim.
  - `getMyWishList()`: Lấy toàn bộ danh sách phim yêu thích của user đang đăng nhập.
  - `isInWishList(Long movieId)`: Kiểm tra phim đã có trong danh sách yêu thích chưa (trả về `true`/`false`).
  - `getWishListById(Long id)`: Lấy chi tiết mục yêu thích theo ID.

### 2.5. Tầng Điều khiển (Controller) - Package `com.example.main.controller`
* **`userRatingController.java`** *(Tạo mới)*: Định tuyến `/user/ratings` (URL đầy đủ: `/api/user/ratings`).
* **`userWishListController.java`** *(Tạo mới)*: Định tuyến `/user/wishlist` (URL đầy đủ: `/api/user/wishlist`).

---

## 3. Danh sách Endpoints & Hướng dẫn kiểm thử (Postman)

> **Điều kiện tiên quyết:** Phải đăng nhập trước thông qua `POST /api/auth/login` để nhận Cookie `JSESSIONID`.

### 3.1. Phim yêu thích (Wishlist)

#### 1. Thêm phim vào danh sách yêu thích
- **URL:** `POST /api/user/wishlist`
- **Body (JSON):**
  ```json
  {
    "movieId": 1
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "success": true,
    "message": "Đã thêm phim vào danh sách yêu thích",
    "data": {
      "id": 1,
      "userId": 2,
      "username": "nguyenvana",
      "movieId": 1,
      "movieTitle": "Avatar 2",
      "moviePosterUrl": "https://example.com/poster.jpg",
      "releaseDate": "2022-12-16",
      "createdAt": "2026-11-07T13:30:00"
    }
  }
  ```

#### 2. Lấy danh sách phim yêu thích của tôi
- **URL:** `GET /api/user/wishlist/my`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Lấy danh sách phim yêu thích thành công",
    "data": [
      {
        "id": 1,
        "userId": 2,
        "username": "nguyenvana",
        "movieId": 1,
        "movieTitle": "Avatar 2",
        "moviePosterUrl": "https://example.com/poster.jpg",
        "releaseDate": "2022-12-16",
        "createdAt": "2026-11-07T13:30:00"
      }
    ]
  }
  ```

#### 3. Kiểm tra phim đã có trong danh sách yêu thích chưa
- **URL:** `GET /api/user/wishlist/check/1`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Kiểm tra danh sách yêu thích thành công",
    "data": true
  }
  ```

#### 4. Xóa phim khỏi danh sách yêu thích theo ID phim
- **URL:** `DELETE /api/user/wishlist/movie/1`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Đã xóa phim khỏi danh sách yêu thích",
    "data": null
  }
  ```

#### 5. Xóa khỏi danh sách yêu thích theo ID mục
- **URL:** `DELETE /api/user/wishlist/1`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Đã xóa khỏi danh sách yêu thích",
    "data": null
  }
  ```

---

### 3.2. Đánh giá phim (Rating)

#### 1. Đánh giá phim (Thêm mới hoặc Cập nhật)
- **URL:** `POST /api/user/ratings`
- **Body (JSON):**
  ```json
  {
    "movieId": 1,
    "rating": 8.5
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "success": true,
    "message": "Đánh giá phim thành công",
    "data": {
      "id": 1,
      "userId": 2,
      "username": "nguyenvana",
      "movieId": 1,
      "movieTitle": "Avatar 2",
      "moviePosterUrl": "https://example.com/poster.jpg",
      "rating": 8.5,
      "createdAt": "2026-11-07T13:30:00",
      "updatedAt": "2026-11-07T13:30:00"
    }
  }
  ```

#### 2. Cập nhật điểm đánh giá theo ID bản ghi
- **URL:** `PUT /api/user/ratings/1?rating=9.0`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Cập nhật đánh giá thành công",
    "data": {
      "id": 1,
      "userId": 2,
      "username": "nguyenvana",
      "movieId": 1,
      "movieTitle": "Avatar 2",
      "rating": 9.0,
      "updatedAt": "2026-11-07T13:32:00"
    }
  }
  ```

#### 3. Lấy danh sách các phim tôi đã đánh giá
- **URL:** `GET /api/user/ratings/my`
- **Response (200 OK):** Danh sách các đánh giá của người dùng hiện tại.

#### 4. Lấy đánh giá của tôi cho một bộ phim cụ thể
- **URL:** `GET /api/user/ratings/movie/1`
- **Response (200 OK):** Trả về thông tin đánh giá của chính người dùng hiện tại (hoặc `null` nếu chưa đánh giá).

#### 5. Xem tất cả đánh giá của một bộ phim
- **URL:** `GET /api/user/ratings/all/movie/1`
- **Response (200 OK):** Danh sách toàn bộ đánh giá từ cộng đồng cho phim có ID = 1.

#### 6. Xóa đánh giá
- **Xóa theo ID đánh giá:** `DELETE /api/user/ratings/1`
- **Xóa đánh giá của tôi đối với phim:** `DELETE /api/user/ratings/movie/1`
- **Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Xóa đánh giá thành công",
    "data": null
  }
  ```

