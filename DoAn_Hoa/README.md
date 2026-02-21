# Hệ Thống Quản Lý Điểm Danh Sinh Viên Bằng QR Code

## I. Tổng Quan Dự Án

Đây là một ứng dụng Android được phát triển để hiện đại hóa và tự động hóa quy trình điểm danh trong các lớp học. Hệ thống cho phép giảng viên quản lý lớp học, tạo mã QR điểm danh, và theo dõi chuyên cần của sinh viên. Sinh viên sử dụng ứng dụng để thực hiện điểm danh một cách nhanh chóng và an toàn thông qua việc quét mã QR, kết hợp với các biện pháp xác thực để đảm bảo tính minh bạch.

## II. Phân Quyền & Quản Lý Tài Khoản

Hệ thống có 2 loại tài khoản chính với các quyền hạn rõ ràng.

### 1. Tài Khoản `Admin` (Giảng Viên)

- **Vai trò:** Là người quản lý toàn bộ hệ thống.
- **Quyền hạn:**
    - **Quản lý lớp học:** Tạo, sửa, xóa thông tin lớp học.
    - **Quản lý sinh viên:** Thêm sinh viên vào lớp, tạo và quản lý tài khoản cho sinh viên.
    - **Quản lý điểm danh:** Tạo mã QR điểm danh, theo dõi và chỉnh sửa trạng thái điểm danh của sinh viên khi cần thiết (ví dụ: do lỗi thiết bị).
    - **Xác thực thiết bị:** Phê duyệt cho sinh viên đăng nhập bằng thiết bị mới.
    - **Thống kê:** Xem báo cáo, thống kê chuyên cần của lớp học và của từng sinh viên.

### 2. Tài Khoản `User` (Sinh Viên)

- **Vai trò:** Người tham gia điểm danh.
- **Quyền hạn:**
    - **Đăng nhập:** Truy cập vào hệ thống bằng tài khoản được cấp.
    - **Điểm danh:** Quét mã QR để ghi nhận sự có mặt trong buổi học.
    - **Xem lịch sử:** Theo dõi lịch sử điểm danh và thống kê chuyên cần của cá nhân.
    - **Đổi mật khẩu:** Tự quản lý bảo mật tài khoản.

---

## III. Các Luồng Chức Năng Chi Tiết

### 1. Quản Lý Tài Khoản Sinh Viên

- **Tạo tài khoản:** Do `Admin` (Giảng viên) thực hiện. Sinh viên không thể tự đăng ký.
- **Thông tin tài khoản:**
    - **Username:** Mã số sinh viên (gồm 10 chữ số).
    - **Mật khẩu:**
        - `Admin` cấp mật khẩu mặc định khi tạo tài khoản.
        - Sinh viên **bắt buộc** phải đổi mật khẩu trong lần đăng nhập đầu tiên để đảm bảo bảo mật.
- **Tìm kiếm:** `Admin` có thể tìm kiếm sinh viên theo mã số hoặc tên ở các trang quản lý.

### 2. Luồng Đăng Nhập & Bảo Mật (Rất Quan Trọng)

Quy trình đăng nhập của sinh viên được thiết kế để chống gian lận.

- **Bước 1: Đăng nhập:** Sinh viên nhập Mã sinh viên và Mật khẩu.
- **Bước 2: Liên kết thiết bị:**
    - **Lần đầu tiên:** Hệ thống sẽ tự động lưu lại định danh của thiết bị và liên kết với tài khoản sinh viên.
    - **Những lần sau:** Hệ thống kiểm tra xem thiết bị đang đăng nhập có phải là thiết bị đã được liên kết hay không.
- **Xử lý các trường hợp:**
    - **Đăng nhập thành công:** Thông tin chính xác và đăng nhập trên thiết bị đã liên kết.
    - **Sai thông tin:** Thông báo "Sai tên đăng nhập hoặc mật khẩu".
    - **Đăng nhập trên thiết bị lạ:**
        - Đăng nhập bị **chặn**.
        - Hiển thị thông báo: *"Tài khoản đã được liên kết với một thiết bị khác. Vui lòng liên hệ giảng viên để được hỗ trợ."*

### 3. Xác Thực Thiết Bị Mới

Khi sinh viên cần đổi thiết bị (do mất máy, hỏng hóc,...), quy trình sẽ cần sự can thiệp của `Admin`.

1.  Sinh viên cố gắng đăng nhập trên thiết bị mới và bị chặn.
2.  Sinh viên liên hệ với `Admin`.
3.  `Admin` truy cập chức năng quản lý, tìm tài khoản của sinh viên đó và thực hiện thao tác **"Hủy liên kết thiết bị cũ"**.
4.  Sau khi `Admin` xác nhận, sinh viên có thể đăng nhập lại trên thiết bị mới. Hệ thống sẽ ghi nhận đây là thiết bị hợp lệ mới.

### 4. Quản Lý Lớp Học (Dành cho Admin)

- **Giao diện chính:** Màn hình chính của `Admin` sẽ ưu tiên chức năng **Tạo Lớp Học**. Các chức năng quản lý khác (quản lý sinh viên, thống kê,...) sẽ được đặt trong thanh menu (Navigation Drawer).
- **Tạo lớp học:**
    - **Thông tin:** Tên lớp, môn học, giảng viên, học kỳ, năm học, phòng học, ngày bắt đầu/kết thúc.
    - **Thêm sinh viên vào lớp:**
        - Sau khi điền thông tin lớp, `Admin` nhấn vào nút **"Thêm sinh viên"**.
        - Giao diện cho phép thêm mã sinh viên vào lớp (tối thiểu 8 sinh viên). Danh sách sinh viên được hiển thị trong `ListView` có thể cuộn, kèm nút **Sửa/Xóa** cho từng người.
        - Mọi thao tác (Thêm, Sửa, Xóa) đều có hộp thoại xác nhận và thông báo kết quả (thành công/thất bại).
        - **Quan trọng:** Sinh viên được thêm vào lớp học này mới có quyền điểm danh cho các buổi học của lớp đó.
- **Trạng thái lớp học:**
    - **Đang diễn ra:** (Màu xanh)
    - **Chưa tới giờ / Đã kết thúc / Bị khóa:** (Màu đỏ)
- **Tìm kiếm:** `Admin` có thể tìm kiếm lớp học nhanh chóng theo tên lớp, môn học.

### 5. Điểm Danh Bằng QR Code

- **Tạo QR Code (Admin):**
    - `Admin` tạo mã QR cho mỗi buổi học. Mã này chứa thông tin buổi học và thời gian điểm danh được phép.
    - **Quy định thời gian:**
        - **0–10 phút đầu:** Ghi nhận "Đúng giờ".
        - **Sau 10 phút:** Ghi nhận "Đi muộn".
        - **Sau 45 phút:** Mã QR tự động hết hạn.
- **Luồng Điểm Danh (Sinh viên):**
    1.  **Mở camera và quét mã QR.**
    2.  **Kiểm tra điều kiện:**
        - **GPS:** Kiểm tra sinh viên có ở trong phạm vi lớp học (bán kính 50m) không. Nếu không, thông báo: *"Bạn đang ở ngoài phạm vi điểm danh."*
        - **Lớp học:** Kiểm tra mã sinh viên có trong danh sách lớp hay không. Nếu không, thông báo: *"Bạn không có trong danh sách lớp học này."*
        - **Điểm danh lại:** Kiểm tra sinh viên đã điểm danh hay chưa. Nếu rồi, thông báo: *"Bạn đã điểm danh lúc [giờ] - Trạng thái: [trạng thái]."*
    3.  **Xác minh khuôn mặt:**
        - Sau khi quét QR thành công, ứng dụng hiển thị thông báo "Đang xác minh khuôn mặt..." và mở camera trước.
        - **Thành công:** Thông báo "Xác minh thành công!" và ghi nhận điểm danh.
        - **Thất bại:** Thông báo lỗi (ví dụ: "Không nhận diện được khuôn mặt"), kèm 2 nút:
            - **Thử lại:** Quét lại khuôn mặt.
            - **Hủy:** Quay về trang chủ, thông báo "Hủy điểm danh."
- **Ghi nhận kết quả:** Hệ thống lưu lại chính xác ngày, giờ, và trạng thái điểm danh.

### 6. Thống Kê & Báo Cáo (Dành cho Admin)

- **Giao diện trực quan:**
    - Xem danh sách điểm danh của một buổi học, lọc nhanh các sinh viên chưa điểm danh.
    - Thống kê chuyên cần theo ngày/tuần/tháng.
- **Biểu đồ (Thư viện MPAndroidChart):**
    - **Biểu đồ tròn:** Hiển thị tỷ lệ sinh viên có mặt / vắng mặt.
    - **Biểu đồ cột:** So sánh số buổi vắng của từng sinh viên.
- **Xuất báo cáo:** Cho phép xuất file thống kê ra định dạng PDF hoặc Excel để lưu trữ.
