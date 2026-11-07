Mô tả chức năng CRUD Movie
1. Giới thiệu
Hai chức năng được triển khai là quản lý thể loại phim (Genre) và quản lý phim (Movie).

2. Chức năng quản lý thể loại phim (Genre)
Chức năng Genre cho phép quản lý danh sách các thể loại phim trong hệ thống.
2.1. Create -- Thêm thể loại
Cho phép người dùng thêm một thể loại phim mới bằng cách nhập tên thể loại.
- Hệ thống kiểm tra tên thể loại có bị bỏ trống hay không.
- Hệ thống kiểm tra tên thể loại đã tồn tại hay chưa.
- Nếu dữ liệu hợp lệ, thể loại mới được lưu vào cơ sở dữ liệu.
2.2. Read -- Xem thể loại
Cho phép người dùng xem thông tin thể loại phim.
- Xem danh sách tất cả thể loại.
- Xem thông tin chi tiết của một thể loại theo ID.
- Thông tin hiển thị gồm ID và tên thể loại.
2.3. Update -- Cập nhật thể loại
Cho phép người dùng thay đổi tên của một thể loại đã tồn tại.
- Hệ thống tìm thể loại theo ID.
- Kiểm tra tên mới không được để trống.
- Kiểm tra tên mới không bị trùng với thể loại khác.
- Nếu hợp lệ, hệ thống lưu thông tin đã cập nhật.
2.4. Delete -- Xóa thể loại
Cho phép người dùng xóa một thể loại khỏi hệ thống.
- Hệ thống kiểm tra thể loại có tồn tại hay không.
- Khi xóa, các liên kết giữa thể loại đó và phim được gỡ bỏ.
- Các phim liên quan không bị xóa theo thao tác này.

3. Chức năng quản lý phim (Movie)
Chức năng Movie cho phép quản lý thông tin các bộ phim và liên kết phim
với các thể loại tương ứng.

Thông tin phim gồm: - Tên phim. - Ngày phát hành. - Mô tả phim. - Đường
dẫn poster. - Đường dẫn trailer. - Điểm đánh giá. - Thời lượng phim. -
Danh sách thể loại phim.
3.1. Create -- Thêm phim
Cho phép người dùng thêm một bộ phim mới vào hệ thống.
- Người dùng nhập thông tin của bộ phim.
- Người dùng có thể chọn một hoặc nhiều thể loại đã tồn tại.
- Hệ thống kiểm tra tên phim không được để trống.
- Hệ thống kiểm tra các ID thể loại được chọn có tồn tại hay không.
- Nếu dữ liệu hợp lệ, phim được lưu vào cơ sở dữ liệu và liên kết với
  các thể loại đã chọn.
3.2. Read -- Xem phim
Cho phép người dùng xem thông tin phim.
- Xem danh sách tất cả phim.
- Xem thông tin chi tiết của một phim theo ID.
- Thông tin trả về bao gồm các thuộc tính của phim và danh sách thể
  loại mà phim thuộc về.
3.3. Update -- Cập nhật phim
Cho phép người dùng chỉnh sửa thông tin của một bộ phim đã có.
- Hệ thống tìm phim theo ID.
- Người dùng có thể thay đổi thông tin phim.
- Người dùng có thể thay đổi danh sách thể loại của phim.
- Hệ thống kiểm tra tên phim và các ID thể loại.
- Nếu dữ liệu hợp lệ, thông tin mới được lưu vào cơ sở dữ liệu.
3.4. Delete -- Xóa phim
Cho phép người dùng xóa một bộ phim khỏi hệ thống.
- Hệ thống tìm phim theo ID.
- Nếu phim tồn tại, hệ thống thực hiện thao tác xóa.
- Các liên kết thể loại của phim được xử lý theo quan hệ Many-to-Many
  đã khai báo trong entity.

4. Mối quan hệ giữa Movie và Genre
Movie và Genre có quan hệ Many-to-Many (nhiều-nhiều):
- Một bộ phim có thể thuộc nhiều thể loại.
- Một thể loại có thể được sử dụng cho nhiều bộ phim.
- Quan hệ này được quản lý thông qua bảng trung gian movie_genre,
  gồm khóa liên kết movie_id và genre_id.
Khi thêm hoặc cập nhật phim, hệ thống nhận danh sách ID thể loại để
thiết lập quan hệ giữa phim và thể loại.