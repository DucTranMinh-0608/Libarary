# Library Manager

> Ứng dụng quản lý thư viện hiện đại được xây dựng bằng JavaFX, tích hợp Firebase Authentication và Firestore, hỗ trợ quản lý sách, người dùng và mượn trả tài liệu trực tuyến.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.12-blue.svg)](https://openjfx.io/)
[![Firebase](https://img.shields.io/badge/Firebase-9.3.0-yellow.svg)](https://firebase.google.com/)
[![Maven](https://img.shields.io/badge/Maven-4.0.0-red.svg)](https://maven.apache.org/)

## Công nghệ sử dụng

### Core Technologies
- **Java 17** - Ngôn ngữ lập trình chính
- **JavaFX 17.0.12** - Framework xây dựng giao diện desktop
- **Maven** - Quản lý dependencies và build tool

### Backend & Database
- **Firebase Admin SDK 9.3.0** - Xác thực và quản lý người dùng
- **Google Cloud Firestore 3.27.0** - Cơ sở dữ liệu NoSQL cloud
- **MongoDB Driver 5.2.0** - Hỗ trợ database MongoDB (optional)

### UI & Icons
- **Ikonli 12.3.1** - Thư viện icon cho JavaFX
- **Ant Design Icons Pack** - Bộ icon Ant Design
- **BootstrapFX 0.4.0** - CSS framework cho JavaFX

### Utilities
- **Lombok 1.18.34** - Giảm boilerplate code
- **Apache Commons Lang3 3.17.0** - Thư viện tiện ích Java
- **Java Dotenv 5.2.2** - Quản lý biến môi trường
- **JSON 20240303** - Xử lý dữ liệu JSON

### External APIs
- **Google Books API** - Tìm kiếm và lấy thông tin sách


## Tính năng chính

### Quản lý tài liệu
- Thêm, xóa, sửa, tìm kiếm sách
- Phân loại sách theo thể loại

### Quản lý người dùng
- Đăng nhập/đăng ký bằng email & password
- Cập nhật thông tin cá nhân (SĐT, ngày sinh,tên)
- Phân quyền Admin/User

### Quản lý mượn trả
- Mượn sách Online
- Kiểm tra điều kiện mượn (Trạng thái)

### Tính năng nâng cao
- **Đề xuất sách** theo số lượt mượn và đánh giá
- **Bình luận & đánh giá** sách
- **Yêu thích** - Đánh dấu sách yêu thích
- **Dashboard** thống kê cho Admin
- **Tìm kiếm nâng cao** với nhiều tiêu chí