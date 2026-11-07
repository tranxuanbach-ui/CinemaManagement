TÀI LIỆU HƯỚNG DẪN 4 CRUD liên quan đến Cinema
Toàn bộ 4 module trong dự án (Cinema System, Cinema, Movie System Link, Showtime) đều được xây dựng theo chuẩn mô hình phân tầng Spring Boot (Layered Architecture):
Controller (Tầng tiếp nhận): Nhận yêu cầu từ phía người dùng qua đường dẫn API (URL) và điều hướng xử lý.
Service (Tầng nghiệp vụ): "Bộ não" kiểm tra logic (ví dụ: kiểm tra xem dữ liệu có bị trùng lặp không, các mã định danh liên kết có thực sự tồn tại trong cơ sở dữ liệu hay không trước khi lưu).
Repository (Tầng dữ liệu): Tương tác trực tiếp với cơ sở dữ liệu để thêm, sửa, xóa hoặc tìm kiếm bản ghi nhanh chóng.
DTO & Request/Response: Đóng gói và chuyển đổi dữ liệu qua lại dưới định dạng JSON, giúp bảo mật cấu trúc bảng bên trong và tự động dịch các giá trị phức tạp (như ngày giờ, tên rạp, tên phim) để hiển thị trực quan cho người dùng.

1. Quản lý Hệ thống Rạp (Cinema System)
Ý nghĩa: Quản lý các tập đoàn hoặc thương hiệu rạp chiếu phim lớn (ví dụ: CGV, Beta). Đây là bảng gốc đứng đầu trong chuỗi phân cấp.
Cách code vận hành:
Yêu cầu thông tin tối thiểu là tên hệ thống rạp (name).   
Code kiểm tra nghiêm ngặt không được để trống tên và kiểm tra xem tên hệ thống rạp đã tồn tại trong hệ thống hay chưa để tránh bị trùng lặp.   
2. Quản lý Cụm Rạp (Cinema)
Ý nghĩa: Quản lý các rạp chiếu cụ thể nằm trong một hệ thống lớn (ví dụ: rạp CGV Vincom Bà Triệu thuộc thương hiệu CGV).
Cách code vận hành:
Mỗi cụm rạp sinh ra bắt buộc phải đi kèm với mã hệ thống rạp (systemId) để biết rạp này thuộc về tập đoàn nào.   
Khi hiển thị, hệ thống tự động gom nhóm để trả về cả tên cụm rạp lẫn tên hệ thống rạp cha (systemName), giúp người quản lý dễ dàng tra cứu.   
3. Liên kết Phim với Hệ thống Rạp (Movie System Link)
Ý nghĩa: Quản lý các đường dẫn (detailLink) trang thông tin hoặc trang đặt vé chi tiết của một bộ phim ứng với từng hãng rạp riêng biệt.   
Cách code vận hành:Đóng vai trò là "cầu nối" ghép giữa mã bộ phim (movieId) và mã hệ thống rạp (systemId).   
Khi tra cứu, hệ thống trả về tên phim cụ thể (movieTitle) và tên hãng rạp (systemName) đi kèm đường dẫn chi tiết.   
4. Quản lý Suất Chiếu (Showtime)
Ý nghĩa: Quản lý lịch chiếu phim chi tiết (phim gì, chiếu ở rạp nào, vào ngày giờ nào và phiên bản gì).   
Cách code vận hành:Kết hợp giữa mã phim (movieId) và mã cụm rạp (cinemaId).   
Xử lý thông minh các định dạng ngày chiếu (yyyy-MM-dd), giờ chiếu (HH:mm) và ghi nhận rõ phiên bản phim (movieVersion như 2D phụ đề, 3D...), hỗ trợ bộ lọc tìm kiếm đa dạng theo ngày, theo rạp hoặc theo phim

1. QUY TRÌNH XÁC THỰC (BẮT BUỘC CHẠY ĐẦU TIÊN)
Để các API bên dưới hoạt động thành công (không bị lỗi phân quyền), hệ thống yêu cầu thực hiện lần lượt các bước xác thực qua phiên làm việc ($session) trên PowerShell:

Bước 1: Khởi tạo phiên làm việc lưu Cookie
PowerShell
$session = New-Microsoft.PowerShell.Commands.WebRequestSession
Bước 2: Đăng ký tài khoản mới (Nếu chưa có)
PowerShell
$registerBody = '{"username":"testuser","password":"password123","email":"test@example.com"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -ContentType "application/json" -Body $registerBody -WebSession $session
Bước 3: Đăng nhập hệ thống (Để lấy quyền truy cập)
PowerShell
$loginBody = '{"username":"testuser","password":"password123"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -WebSession $session
2. GIẢI THÍCH CÁC CHỨC NĂNG CRUD (DÀNH CHO NGƯỜI KHÔNG CHUYÊN)
Hệ thống quản lý rạp chia làm 2 cấp bậc chính liên kết chặt chẽ với nhau:

Hệ thống rạp (Cinema System): Là các tập đoàn hoặc thương hiệu rạp lớn (Ví dụ: CGV Cinemas, Beta Cinemas, Lotte Cinema).

Cụm rạp (Cinema): Là các rạp chiếu cụ thể nằm trong một hệ thống rạp (Ví dụ: Rạp CGV Vincom Bà Triệu thuộc hệ thống CGV). Khi tạo rạp, bắt buộc phải gắn với mã định danh của hệ thống rạp đó (systemId).

3. TẬP LỆNH POWERSHELL KIỂM THỬ CHI TIẾT (4 NHÓM CRUD)
Nhóm 1: Quản lý Hệ thống Rạp (Cinema System)
Tạo mới Hệ thống rạp (Create):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Post -ContentType "application/json" -Body '{"name": "CGV Cinemas"}' -WebSession $session
Lấy danh sách Hệ thống rạp (Read):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems" -Method Get -WebSession $session
Cập nhật Hệ thống rạp (Update - Ví dụ sửa ID số 1):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems/1" -Method Put -ContentType "application/json" -Body '{"name": "CGV Cinemas Updated"}' -WebSession $session
Xóa Hệ thống rạp (Delete - Ví dụ xóa ID số 2):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinema-systems/2" -Method Delete -WebSession $session
Nhóm 2: Quản lý Cụm Rạp (Cinema)
Tạo mới Cụm rạp trực thuộc hệ thống (Create - systemId là ID của hãng rạp ở nhóm 1):

PowerShell
$cinemaBody = '{"systemId": 1, "name": "CGV Vincom Ba Trieu"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Post -ContentType "application/json" -Body $cinemaBody -WebSession $session
Lấy danh sách Cụm rạp (Read):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas" -Method Get -WebSession $session
Cập nhật Cụm rạp (Update - Ví dụ sửa rạp ID số 1):

PowerShell
$updateCinemaBody = '{"systemId": 1, "name": "CGV Vincom Ba Trieu - VIP"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas/1" -Method Put -ContentType "application/json" -Body $updateCinemaBody -WebSession $session
Xóa Cụm rạp (Delete - Ví dụ xóa rạp ID số 1):

PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/api/cinemas/1" -Method Delete -WebSession $session
