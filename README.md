# Smart Attendance System

Hệ thống điểm danh thông minh sử dụng công nghệ QR Code và định vị GPS

## Công nghệ sử dụng

- **Backend**: Spring MVC 5.3.30 (không dùng Spring Boot)
- **Database**: MySQL 8.0+
- **View Template**: Thymeleaf
- **Security**: Spring Security 5.8.6
- **Build Tool**: Maven
- **Java**: JDK 17

## Tính năng chính

### Dành cho Giáo viên

- Quản lý lớp học và sinh viên
- Tạo buổi học và mã QR điểm danh
- Xem báo cáo điểm danh real-time
- Điểm danh thủ công trong trường hợp đặc biệt

### Dành cho Sinh viên

- Quét mã QR để điểm danh
- Tự động chụp ảnh selfie
- Xác thực vị trí GPS
- Xem lịch sử điểm danh

### Tính năng bảo mật

- Mã QR có thời hạn (5 phút)
- Kiểm tra khoảng cách GPS (mặc định 50m)
- Chống trùng thiết bị
- Lưu trữ ảnh selfie để xác minh

## Cài đặt

### 1. Cấu hình Database

```bash
# Tạo database
mysql -u root -p < src/main/resources/database/schema.sql
```

### 2. Cấu hình application.properties

Chỉnh sửa file `src/main/resources/application.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/smart_attendance
db.username=root
db.password=your_password
```

### 3. Build và Deploy

```bash
# Build project
mvn clean package

# Deploy file WAR vào Tomcat
# Copy target/SmartAttendance.war vào thư mục webapps của Tomcat
```

### 4. Chạy ứng dụng

```bash
# Start Tomcat
# Truy cập: http://localhost:8080/SmartAttendance
```

## Tài khoản Demo

### Giáo viên

- **Username**: teacher
- **Password**: password

### Sinh viên

- **Username**: student
- **Password**: password

### Admin

- **Username**: admin
- **Password**: password

## Cấu trúc Project

```
SmartAttendance/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dinhkhang/code/
│   │   │       ├── config/          # Configuration classes
│   │   │       ├── controller/      # Controllers
│   │   │       ├── entity/          # JPA Entities
│   │   │       ├── repository/      # Spring Data JPA Repositories
│   │   │       ├── service/         # Business Logic
│   │   │       └── dto/            # Data Transfer Objects
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   └── database/
│   │   │       └── schema.sql
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── views/          # Thymeleaf templates
│   │       └── resources/
│   │           ├── css/
│   │           ├── js/
│   │           └── images/
│   └── test/
└── pom.xml
```

## API Endpoints

### Authentication

- `POST /login` - Đăng nhập
- `POST /logout` - Đăng xuất

### QR Code API (Teacher)

- `POST /api/qr/generate` - Tạo mã QR mới

### Attendance API (Student)

- `POST /api/attendance/checkin` - Điểm danh

### Teacher Dashboard

- `GET /teacher/dashboard` - Dashboard
- `GET /teacher/classes` - Danh sách lớp
- `GET /teacher/sessions/{id}/qr` - Tạo QR điểm danh

### Student Dashboard

- `GET /student/dashboard` - Dashboard
- `GET /student/scan-qr` - Quét mã QR
- `GET /student/attendance-history` - Lịch sử điểm danh

## Yêu cầu hệ thống

- Java JDK 17+
- MySQL 8.0+
- Apache Tomcat 9.0+
- Maven 3.6+
- Trình duyệt hỗ trợ HTML5 Geolocation API và Camera API

## Lưu ý

1. **GPS**: Ứng dụng yêu cầu quyền truy cập vị trí để hoạt động
2. **Camera**: Cần quyền truy cập camera để chụp ảnh selfie
3. **HTTPS**: Nên sử dụng HTTPS trong production để bảo mật
4. **Database**: Nhớ backup database định kỳ

## Tác giả

Đinh Khang - Smart Attendance System

## License

MIT License
