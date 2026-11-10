# Tài liệu Tính năng Gợi ý phim (Recommendation System)

Tính năng gợi ý phim tự động tính toán **Phần trăm mức độ ưu tiên (`matchPercentage` từ 0.0% đến 100.0%)** của người dùng dành cho từng bộ phim dựa trên **Thể loại (Genre)** từ dữ liệu lịch sử tương tác (**`UserWishList`** và **`UserRating`**), đồng thời giải quyết bài toán **Cold Start** cho người dùng mới hoặc khách chưa đăng nhập.

---

## 1. Cơ chế và Thuật toán Tính toán Phần trăm Ưu tiên

### 1.1. Chuẩn hóa Phân bố Sở thích Thể loại theo Phần trăm (Genre Preference Distribution)
Thay vì để điểm số tăng không giới hạn khi người dùng lưu/đánh giá nhiều phim, hệ thống chuẩn hóa tổng sở thích thành **100.0%**:
1. **Thu thập điểm số thô:**
   - Mỗi thể loại trong **`UserWishList`**: `+5.0 điểm`.
   - Mỗi thể loại trong **`UserRating`**: Phim đánh giá tích cực ($\ge 5$ sao) đóng góp `(Rating - 4.0)` điểm (ví dụ: 10 sao $\rightarrow$ `+6.0`, 8 sao $\rightarrow$ `+4.0`, $<5$ sao $\rightarrow$ `0` điểm).
2. **Quy đổi ra tỷ lệ phần trăm sở thích:**
   $$P(g) = \frac{\text{Điểm thô của thể loại } g}{\text{Tổng điểm thô của tất cả thể loại}} \times 100\%$$
   $\rightarrow$ **Tổng tỷ lệ phần trăm của tất cả các thể loại luôn bằng đúng 100.0%.**

---

### 1.2. Chấm điểm Phần trăm Ưu tiên cho Phim Ứng viên (`matchPercentage`)
1. **Loại trừ phim đã tương tác:**
   - Các phim mà người dùng **đã đánh giá** hoặc **đã có trong Wishlist** sẽ được loại bỏ khỏi danh sách gợi ý.
2. **Công thức tính phần trăm ưu tiên tổng thể:**
   - **Độ trùng khớp thể loại (`genreMatchPercent`):**
     $$\text{genreMatchPercent} = \min\left(100.0, \sum_{g \in M.\text{genres}} P(g)\right)$$
   - **Chất lượng đánh giá phim (`ratingPercent`):**
     $$\text{ratingPercent} = \frac{\text{baseRating}}{10.0} \times 100\%$$
   - **Phần trăm ưu tiên cuối cùng (`matchPercentage`):**
     $$\text{matchPercentage} = (\text{genreMatchPercent} \times 0.7) + (\text{ratingPercent} \times 0.3)$$
     - **70%** quyết định bởi độ phù hợp thể loại yêu thích.
     - **30%** quyết định bởi chất lượng tổng thể của phim.
     - Kết quả luôn **nằm trong khoảng an toàn $[0.0\%, 100.0\%]$** và được làm tròn đến 1 chữ số thập phân (ví dụ: `87.5%`).

---

### 1.3. Xử lý Cold Start
Kích hoạt khi khách chưa đăng nhập, người dùng mới chưa có tương tác hoặc số phim gợi ý chưa đủ 10:
- Lấy các phim có điểm đánh giá cao nhất trong database: `ORDER BY COALESCE(rating, defaultRating, 0.0) DESC, releaseDate DESC`.
- Phần trăm ưu tiên của Cold Start tương ứng với tỷ lệ điểm của phim: $\text{matchPercentage} = \frac{\text{Rating}}{10.0} \times 100\%$.

---

## 2. Ví dụ Minh họa Tính toán Cụ thể

Giả sử người dùng **Nguyễn Văn A** có lịch sử tương tác như sau:
* **Wishlist:** Đã lưu **Phim A** (Thể loại: `Hành Động`, `Viễn Tưởng`).
* **Rating:**
  * Đã đánh giá **10 sao** cho **Phim B** (Thể loại: `Hành Động`, `Phiêu Lưu`).
  * Đã đánh giá **8 sao** cho **Phim C** (Thể loại: `Kinh Dị`).

### Bước 1: Tính điểm thô cho từng thể loại
* `Hành Động`: $+5.0$ (từ Wishlist) $+ (10 - 4.0 = +6.0)$ (từ Rating) $= \mathbf{11.0}$ điểm.
* `Viễn Tưởng`: $+5.0$ (từ Wishlist) $= \mathbf{5.0}$ điểm.
* `Phiêu Lưu`: $+(10 - 4.0 = +6.0)$ (từ Rating) $= \mathbf{6.0}$ điểm.
* `Kinh Dị`: $+(8 - 4.0 = +4.0)$ (từ Rating) $= \mathbf{4.0}$ điểm.
* **Tổng điểm thô** $= 11.0 + 5.0 + 6.0 + 4.0 = \mathbf{26.0}$ điểm.

### Bước 2: Chuẩn hóa ra % sở thích của từng thể loại ($P(g)$)
* `Hành Động`: $(11.0 / 26.0) \times 100\% = \mathbf{42.3\%}$
* `Phiêu Lưu`: $(6.0 / 26.0) \times 100\% = \mathbf{23.1\%}$
* `Viễn Tưởng`: $(5.0 / 26.0) \times 100\% = \mathbf{19.2\%}$
* `Kinh Dị`: $(4.0 / 26.0) \times 100\% = \mathbf{15.4\%}$
*(Tổng $= 42.3 + 23.1 + 19.2 + 15.4 = 100.0\%$)*

### Bước 3: Chấm điểm % ưu tiên cho 2 phim mới trong Database (ứng viên gợi ý)
* **Phim 1 (Avengers):** Thể loại: `Hành Động`, `Viễn Tưởng`. Điểm rating: `8.5 / 10`.
  * Độ trùng khớp thể loại: $42.3\% + 19.2\% = \mathbf{61.5\%}$.
  * Điểm chất lượng phim: $(8.5 / 10) \times 100\% = \mathbf{85.0\%}$.
  * $\text{matchPercentage} = (61.5\% \times 0.7) + (85.0\% \times 0.3) = 43.05 + 25.5 = \mathbf{68.6\%}$.

* **Phim 2 (The Conjuring):** Thể loại: `Kinh Dị`. Điểm rating: `7.5 / 10`.
  * Độ trùng khớp thể loại: $\mathbf{15.4\%}$.
  * Điểm chất lượng phim: $(7.5 / 10) \times 100\% = \mathbf{75.0\%}$.
  * $\text{matchPercentage} = (15.4\% \times 0.7) + (75.0\% \times 0.3) = 10.78 + 22.5 = \mathbf{33.3\%}$.

$\rightarrow$ **Kết luận:** **Phim 1** đạt **`68.6%`** cao hơn nhiều so với **Phim 2** đạt **`33.3%`**, vì vậy Phim 1 sẽ được xếp trên trong danh sách Top 10 gợi ý.

---

## 3. Các Bước Kiểm thử Thực tế (Testing Guide)

Bạn có thể dùng **Postman** hoặc **cURL** để kiểm tra tính năng theo quy trình chuẩn sau:

### Bước 1: Kiểm tra Cold Start (Khi chưa đăng nhập)
* **Request:** `GET http://localhost:8080/api/recommendations`
* **Mục đích:** Đảm bảo khách vãng lai vẫn nhận được danh sách gợi ý.
* **Kết quả kỳ vọng:**
  * Trả về Top 10 phim có rating cao nhất trong database.
  * Trường `matchPercentage` tương ứng với điểm rating (ví dụ rating 9.0 $\rightarrow$ `matchPercentage: 90.0`).

---

### Bước 2: Tạo tài khoản và Đăng nhập
* **Đăng ký:**
  * `POST http://localhost:8080/api/auth/register`
  * Body (JSON):
    ```json
    {
      "username": "testuser",
      "email": "testuser@gmail.com",
      "password": "password123"
    }
    ```
* **Đăng nhập:**
  * `POST http://localhost:8080/api/auth/login`
  * Body (JSON):
    ```json
    {
      "username": "testuser",
      "password": "password123"
    }
    ```
  * *(Postman sẽ tự động lưu Cookie `JSESSIONID` vào phiên làm việc).*

---

### Bước 3: Kiểm tra Cold Start cho Người dùng mới
* **Request:** `GET http://localhost:8080/api/recommendations` (có kèm Cookie đăng nhập).
* **Kết quả kỳ vọng:** Do người dùng chưa có wishlist hay rating nào, hệ thống tự động kích hoạt Cold Start và trả về Top 10 phim điểm cao nhất.

---

### Bước 4: Tạo dữ liệu sở thích (Wishlist & Rating)
* **Thêm phim có thể loại Khoa học viễn tưởng / Hành động vào Wishlist:**
  * `POST http://localhost:8080/api/user/wishlist`
  * Body (JSON):
    ```json
    {
      "movieId": 1
    }
    ```
* **Đánh giá điểm cao (10 sao) cho một phim Hành động khác:**
  * `POST http://localhost:8080/api/user/ratings`
  * Body (JSON):
    ```json
    {
      "movieId": 2,
      "rating": 10.0
    }
    ```

---

### Bước 5: Kiểm tra Gợi ý Cá nhân hóa theo Phần trăm
* **Request:** `GET http://localhost:8080/api/recommendations`
* **Kết quả kỳ vọng:**
  1. **Phim 1** và **Phim 2** đã xem/lưu **hoàn toàn biến mất** khỏi danh sách (đã bị lọc bỏ).
  2. Các phim còn lại có thể loại **Hành động** hoặc **Viễn tưởng** sẽ có `matchPercentage` cao (từ 70% đến 95%) và đứng đầu danh sách.
  3. Các phim có thể loại khác sẽ có `matchPercentage` thấp hơn hoặc nằm ở cuối danh sách.

---

### Bước 6: Kiểm tra API Cold Start độc lập
* **Request:** `GET http://localhost:8080/api/recommendations/cold-start`
* **Kết quả kỳ vọng:** Luôn trả về danh sách 10 phim có điểm đánh giá cao nhất toàn hệ thống mà không phụ thuộc vào trạng thái đăng nhập.

---

## 4. Cấu trúc Response Mẫu

```json
{
  "success": true,
  "message": "Lấy danh sách gợi ý phim thành công",
  "data": [
    {
      "id": 15,
      "title": "Spider-Man: Across the Spider-Verse",
      "releaseDate": "2023-06-02",
      "description": "Miles Morales du hành qua đa vũ trụ...",
      "posterUrl": "https://image.tmdb.org/t/p/w500/...",
      "trailer": "https://youtube.com/...",
      "rating": 8.8,
      "runtimeMinutes": 140,
      "genres": [
        { "id": 1, "name": "Hành Động" },
        { "id": 2, "name": "Khoa Học Viễn Tưởng" }
      ],
      "matchPercentage": 89.6
    }
  ]
}
```
