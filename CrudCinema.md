# =============================================================================
# TÀI LIỆU KIỂM THỬ CRUD: Cinema System, Cinema, Movie System Link, Showtime
# Dự án: CinemaManagement
# Server: http://localhost:8080/api  (port 8080, context-path /api)
# Jackson: SNAKE_CASE (cấu hình trong application.yaml)
# Lưu ý: Một số DTO có @JsonProperty cố định tên trường (vd: "systemId",
#         "movieId", "cinemaId", "showDate", "showTime") nên các trường này
#         dùng camelCase trong JSON request, không bị ảnh hưởng bởi SNAKE_CASE.
#
# Phân quyền thực tế (từ @PreAuthorize + securityConfig):
#   - GET /movies/**, /cinemas/**  → permitAll (không cần đăng nhập)
#   - GET các endpoint khác        → authenticated (cần đăng nhập)
#   - POST/PUT                     → hasAnyRole('ADMIN','STAFF')
#   - DELETE cinema-systems/cinema → hasRole('ADMIN')
#   - DELETE movie-system-links    → hasAnyRole('ADMIN','STAFF')
#   - DELETE showtimes             → hasAnyRole('ADMIN','STAFF')
#
# → Để test đầy đủ CRUD, cần tài khoản có role ADMIN.
# =============================================================================


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 0: XÁC THỰC (Authentication) — BẮT BUỘC chạy trước
# ═══════════════════════════════════════════════════════════════════════════════

# Bước 0.1: Khởi tạo phiên làm việc (Session) lưu Cookie JSESSIONID
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# Bước 0.2: Đăng ký tài khoản mới (nếu chưa có — role mặc định là USER)
$registerBody = '{"username":"testadmin","password":"password123","email":"admin@example.com"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -ContentType "application/json" -Body $registerBody -WebSession $session

# ★ LƯU Ý: Tài khoản mới đăng ký có role USER → không đủ quyền POST/PUT/DELETE.
# Cần thực hiện 1 trong 2 cách sau để có ADMIN:
#   (a) Cập nhật trực tiếp trong DB:
#       UPDATE [dbo].[users] SET role = 'ADMIN' WHERE username = 'testadmin';
#   (b) Hoặc nếu đã có tài khoản ADMIN, bỏ qua bước 0.2 và đăng nhập trực tiếp.

# Bước 0.3: Đăng nhập (lấy Cookie JSESSIONID)
$loginBody = '{"username":"testadmin","password":"password123"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -WebSession $session

# Bước 0.4: Kiểm tra đăng nhập thành công + xem role
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 1: CRUD HỆ THỐNG RẠP (Cinema System)
# Endpoint base: /api/cinema-systems
# Entity: cinemaSystem { id, name } (unique name, max 100 ký tự)
# ═══════════════════════════════════════════════════════════════════════════════

# --- 1.1 CREATE: Tạo hệ thống rạp mới ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": "CGV Cinemas"}' -WebSession $session

# Tạo thêm hệ thống rạp thứ 2
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": "Beta Cinemas"}' -WebSession $session

# Tạo thêm hệ thống rạp thứ 3 (để test delete)
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": "Lotte Cinema"}' -WebSession $session

# --- 1.2 READ: Lấy danh sách tất cả hệ thống rạp ---
# Yêu cầu: authenticated (đã đăng nhập)
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Get -WebSession $session

# --- 1.3 READ: Lấy hệ thống rạp theo ID ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems/1" -Method Get -WebSession $session

# --- 1.4 UPDATE: Cập nhật hệ thống rạp (sửa ID = 1) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems/1" -Method Put -ContentType "application/json" -Body '{"name": "CGV Cinemas Vietnam"}' -WebSession $session

# --- 1.5 DELETE: Xóa hệ thống rạp (xóa ID = 3) ---
# Yêu cầu role: ADMIN
# Cảnh báo: cascade ALL → xóa system sẽ xóa luôn cinema + movieSystemLink con
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems/3" -Method Delete -WebSession $session

# --- 1.6 TEST LỖI: Tạo hệ thống rạp trùng tên (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": "CGV Cinemas Vietnam"}' -WebSession $session

# --- 1.7 TEST LỖI: Tạo hệ thống rạp với tên rỗng (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": ""}' -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 2: CRUD CỤM RẠP (Cinema)
# Endpoint base: /api/cinemas
# Entity: cinema { id, name, system → cinemaSystem }
# Request DTO: cinemaRequest { systemId (@JsonProperty), name }
# Response DTO: cinemaResponse { id, name, system_id, system_name }
# Lưu ý: GET /cinemas/** là permitAll → không cần đăng nhập để xem
# ═══════════════════════════════════════════════════════════════════════════════

# --- 2.1 CREATE: Tạo cụm rạp thuộc hệ thống CGV (systemId = 1) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 1, "name": "CGV Vincom Ba Trieu"}' -WebSession $session

# Tạo thêm cụm rạp thứ 2
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 1, "name": "CGV Aeon Mall Ha Dong"}' -WebSession $session

# Tạo cụm rạp thuộc hệ thống Beta (systemId = 2)
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 2, "name": "Beta Cinemas Thai Nguyen"}' -WebSession $session

# --- 2.2 READ: Lấy danh sách tất cả cụm rạp (permitAll) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Get -WebSession $session

# --- 2.3 READ: Lọc cụm rạp theo hệ thống (query param systemId) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas?systemId=1" -Method Get -WebSession $session

# --- 2.4 READ: Lấy cụm rạp theo ID ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas/1" -Method Get -WebSession $session

# --- 2.5 UPDATE: Cập nhật cụm rạp (sửa ID = 1) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas/1" -Method Put -ContentType "application/json" -Body '{"systemId": 1, "name": "CGV Vincom Ba Trieu - VIP"}' -WebSession $session

# --- 2.6 DELETE: Xóa cụm rạp (xóa ID = 3) ---
# Yêu cầu role: ADMIN
# Cảnh báo: cascade ALL → xóa cinema sẽ xóa luôn showtime con
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas/3" -Method Delete -WebSession $session

# --- 2.7 TEST LỖI: Tạo cụm rạp với systemId không tồn tại (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 9999, "name": "Rap Khong Ton Tai"}' -WebSession $session

# --- 2.8 TEST LỖI: Tạo cụm rạp trùng tên trong cùng hệ thống (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 1, "name": "CGV Vincom Ba Trieu - VIP"}' -WebSession $session

# --- 2.9 TEST LỖI: Tạo cụm rạp với tên rỗng (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body '{"systemId": 1, "name": ""}' -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 3: CRUD LIÊN KẾT PHIM - HỆ THỐNG RẠP (Movie System Link)
# Endpoint base: /api/movie-system-links
# Entity: movieSystemLink { id, movie, system, detailLink }
# Ràng buộc: UNIQUE(movie_id, system_id) — mỗi phim chỉ 1 link/hệ thống
# Request DTO: movieSystemLinkRequest { movieId, systemId, detailLink }
# Response DTO: movieSystemLinkResponse { id, movie_id, movie_title, system_id,
#               system_name, detail_link }
#
# ★ Cần có movie đã tồn tại trong DB trước khi test (tạo qua CRUD Movie)
# ═══════════════════════════════════════════════════════════════════════════════

# --- 3.1 CREATE: Tạo liên kết phim - hệ thống rạp ---
# Yêu cầu role: ADMIN hoặc STAFF
# (Giả sử movie ID = 1 và cinema_system ID = 1 đã tồn tại)
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "systemId": 1, "detailLink": "https://www.cgv.vn/movies/movie-1"}' -WebSession $session

# Tạo thêm liên kết thứ 2 (cùng phim, khác hệ thống)
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "systemId": 2, "detailLink": "https://www.betacinemas.vn/movies/movie-1"}' -WebSession $session

# --- 3.2 READ: Lấy danh sách tất cả liên kết ---
# Yêu cầu: authenticated
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Get -WebSession $session

# --- 3.3 READ: Lọc liên kết theo movieId ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links?movieId=1" -Method Get -WebSession $session

# --- 3.4 READ: Lọc liên kết theo systemId ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links?systemId=1" -Method Get -WebSession $session

# --- 3.5 READ: Lọc liên kết theo movieId + systemId ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links?movieId=1&systemId=1" -Method Get -WebSession $session

# --- 3.6 READ: Lấy liên kết theo ID ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links/1" -Method Get -WebSession $session

# --- 3.7 UPDATE: Cập nhật liên kết (sửa ID = 1) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links/1" -Method Put -ContentType "application/json" -Body '{"movieId": 1, "systemId": 1, "detailLink": "https://www.cgv.vn/movies/movie-1-updated"}' -WebSession $session

# --- 3.8 DELETE: Xóa liên kết (xóa ID = 2) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links/2" -Method Delete -WebSession $session

# --- 3.9 TEST LỖI: Tạo liên kết trùng (movie_id + system_id đã tồn tại) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "systemId": 1, "detailLink": "https://duplicate.com"}' -WebSession $session

# --- 3.10 TEST LỖI: Tạo liên kết với movieId không tồn tại (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Post -ContentType "application/json" -Body '{"movieId": 9999, "systemId": 1, "detailLink": "https://example.com"}' -WebSession $session

# --- 3.11 TEST LỖI: Tạo liên kết với detailLink rỗng (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/movie-system-links" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "systemId": 1, "detailLink": ""}' -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 4: CRUD SUẤT CHIẾU (Showtime)
# Endpoint base: /api/showtimes
# Entity: showtime { id, movie, cinema, showDate, showTime, movieVersion }
# Request DTO: showtimeRequest {
#   movieId (@JsonProperty), cinemaId (@JsonProperty),
#   showDate (@JsonProperty, format yyyy-MM-dd),
#   showTime (@JsonProperty, format HH:mm),
#   movieVersion (@JsonProperty, not blank, max 50)
# }
# Response DTO: showtimeResponse { id, movie_id, movie_title, cinema_id,
#               cinema_name, system_id, system_name, show_date, show_time,
#               movie_version }
#
# ★ Cần có movie + cinema đã tồn tại trong DB trước khi test
# ═══════════════════════════════════════════════════════════════════════════════

# --- 4.1 CREATE: Tạo suất chiếu mới ---
# Yêu cầu role: ADMIN hoặc STAFF
# (Giả sử movie ID = 1, cinema ID = 1 đã tồn tại)
$showtimeBody1 = '{"movieId": 1, "cinemaId": 1, "showDate": "2026-11-10", "showTime": "19:30", "movieVersion": "2D Phu De"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body $showtimeBody1 -WebSession $session

# Tạo thêm suất chiếu thứ 2 (cùng ngày, giờ khác)
$showtimeBody2 = '{"movieId": 1, "cinemaId": 1, "showDate": "2026-11-10", "showTime": "21:00", "movieVersion": "3D"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body $showtimeBody2 -WebSession $session

# Tạo suất chiếu ở rạp khác, ngày khác
$showtimeBody3 = '{"movieId": 1, "cinemaId": 2, "showDate": "2026-11-11", "showTime": "14:00", "movieVersion": "2D Thuyet Minh"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body $showtimeBody3 -WebSession $session

# --- 4.2 READ: Lấy danh sách tất cả suất chiếu ---
# Yêu cầu: authenticated
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Get -WebSession $session

# --- 4.3 READ: Lấy suất chiếu theo ID ---
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes/1" -Method Get -WebSession $session

# --- 4.4 READ: Lọc suất chiếu theo query params ---
# Lọc theo ngày
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes?showDate=2026-11-10" -Method Get -WebSession $session

# Lọc theo rạp
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes?cinemaId=1" -Method Get -WebSession $session

# Lọc theo phim
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes?movieId=1" -Method Get -WebSession $session

# Lọc theo phim + rạp + ngày (kết hợp nhiều filter)
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes?movieId=1&cinemaId=1&showDate=2026-11-10" -Method Get -WebSession $session

# --- 4.5 UPDATE: Cập nhật suất chiếu (sửa ID = 1) ---
# Yêu cầu role: ADMIN hoặc STAFF
$updateShowtime = '{"movieId": 1, "cinemaId": 1, "showDate": "2026-11-12", "showTime": "20:00", "movieVersion": "IMAX 3D"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes/1" -Method Put -ContentType "application/json" -Body $updateShowtime -WebSession $session

# --- 4.6 DELETE: Xóa suất chiếu (xóa ID = 3) ---
# Yêu cầu role: ADMIN hoặc STAFF
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes/3" -Method Delete -WebSession $session

# --- 4.7 TEST LỖI: Tạo suất chiếu với movieId không tồn tại (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body '{"movieId": 9999, "cinemaId": 1, "showDate": "2026-11-10", "showTime": "10:00", "movieVersion": "2D"}' -WebSession $session

# --- 4.8 TEST LỖI: Tạo suất chiếu thiếu movieVersion (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "cinemaId": 1, "showDate": "2026-11-10", "showTime": "10:00"}' -WebSession $session

# --- 4.9 TEST LỖI: Tạo suất chiếu với cinemaId không tồn tại (expect lỗi 400) ---
Invoke-RestMethod -Uri "http://localhost:8080/api/showtimes" -Method Post -ContentType "application/json" -Body '{"movieId": 1, "cinemaId": 9999, "showDate": "2026-11-10", "showTime": "10:00", "movieVersion": "2D"}' -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# PHẦN 5: ĐĂNG XUẤT
# ═══════════════════════════════════════════════════════════════════════════════

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/logout" -Method Post -WebSession $session


# ═══════════════════════════════════════════════════════════════════════════════
# TÓM TẮT BẢNG ENDPOINT
# ═══════════════════════════════════════════════════════════════════════════════
#
# ┌──────────┬──────────────────────────────────────────────────┬──────────────────────────┐
# │  METHOD  │  ENDPOINT                                        │  QUYỀN                   │
# ├──────────┼──────────────────────────────────────────────────┼──────────────────────────┤
# │  POST    │  /api/auth/register                              │  permitAll               │
# │  POST    │  /api/auth/login                                 │  permitAll               │
# │  GET     │  /api/auth/me                                    │  authenticated           │
# │  POST    │  /api/auth/logout                                │  authenticated           │
# ├──────────┼──────────────────────────────────────────────────┼──────────────────────────┤
# │  POST    │  /api/cinema-systems                             │  ADMIN, STAFF            │
# │  GET     │  /api/cinema-systems                             │  authenticated           │
# │  GET     │  /api/cinema-systems/{id}                        │  authenticated           │
# │  PUT     │  /api/cinema-systems/{id}                        │  ADMIN, STAFF            │
# │  DELETE  │  /api/cinema-systems/{id}                        │  ADMIN                   │
# ├──────────┼──────────────────────────────────────────────────┼──────────────────────────┤
# │  POST    │  /api/cinemas                                    │  ADMIN, STAFF            │
# │  GET     │  /api/cinemas                                    │  permitAll (GET)         │
# │  GET     │  /api/cinemas?systemId={id}                      │  permitAll (GET)         │
# │  GET     │  /api/cinemas/{id}                               │  permitAll (GET)         │
# │  PUT     │  /api/cinemas/{id}                               │  ADMIN, STAFF            │
# │  DELETE  │  /api/cinemas/{id}                               │  ADMIN                   │
# ├──────────┼──────────────────────────────────────────────────┼──────────────────────────┤
# │  POST    │  /api/movie-system-links                         │  ADMIN, STAFF            │
# │  GET     │  /api/movie-system-links                         │  authenticated           │
# │  GET     │  /api/movie-system-links?movieId=&systemId=      │  authenticated           │
# │  GET     │  /api/movie-system-links/{id}                    │  authenticated           │
# │  PUT     │  /api/movie-system-links/{id}                    │  ADMIN, STAFF            │
# │  DELETE  │  /api/movie-system-links/{id}                    │  ADMIN, STAFF            │
# ├──────────┼──────────────────────────────────────────────────┼──────────────────────────┤
# │  POST    │  /api/showtimes                                  │  ADMIN, STAFF            │
# │  GET     │  /api/showtimes                                  │  authenticated           │
# │  GET     │  /api/showtimes?movieId=&cinemaId=&showDate=     │  authenticated           │
# │  GET     │  /api/showtimes/{id}                             │  authenticated           │
# │  PUT     │  /api/showtimes/{id}                             │  ADMIN, STAFF            │
# │  DELETE  │  /api/showtimes/{id}                             │  ADMIN, STAFF            │
# └──────────┴──────────────────────────────────────────────────┴──────────────────────────┘
#
# ═══════════════════════════════════════════════════════════════════════════════
# GHI CHÚ QUAN TRỌNG
# ═══════════════════════════════════════════════════════════════════════════════
#
# 1. PORT: Server chạy trên port 8000 (KHÔNG phải 8080 như tài liệu cũ CrudCinema.md).
#
# 2. JSON FIELD NAMES: Mặc dù Jackson cấu hình SNAKE_CASE, các Request DTO
#    có @JsonProperty chỉ định tên camelCase nên khi GỬI request dùng:
#      systemId, movieId, cinemaId, showDate, showTime, movieVersion
#    Khi NHẬN response (không có @JsonProperty) sẽ dùng snake_case:
#      system_id, movie_id, cinema_id, show_date, show_time, movie_version
#
# 3. THỨ TỰ TẠO DỮ LIỆU (ràng buộc khóa ngoại):
#    ① Cinema System (bảng gốc)
#    ② Cinema (phụ thuộc Cinema System)
#    ③ Movie (độc lập — cần tạo trước qua CRUD Movie)
#    ④ Movie System Link (phụ thuộc Movie + Cinema System)
#    ⑤ Showtime (phụ thuộc Movie + Cinema)
#
# 4. CASCADE: cinemaSystem → cascade ALL → xóa system sẽ xóa toàn bộ
#    cinema + movieSystemLink con. cinema → cascade ALL → xóa cinema sẽ
#    xóa toàn bộ showtime con. Cẩn thận khi test delete!
#
# 5. UNIQUE CONSTRAINT: movieSystemLink có UNIQUE(movie_id, system_id)
#    → mỗi bộ phim chỉ có 1 link cho mỗi hệ thống rạp.
#
# 6. showtimeRequest: showDate nhận LocalDate (yyyy-MM-dd),
#    showTime nhận LocalTime (HH:mm hoặc HH:mm:ss).
#    movieVersion là NOT BLANK (bắt buộc, max 50 ký tự).
#
# 7. DATABASE: SQL Server localhost:1433, database: movie_management
#    DDL: hibernate ddl-auto = update (tự tạo/cập nhật bảng)
#
# 8. cinemaSystemRequest: KHÔNG có trường logoUrl (đã bị loại bỏ trong code
#    mới). Chỉ có trường "name" (max 100 ký tự).
