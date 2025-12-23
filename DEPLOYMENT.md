# Hướng dẫn Deploy Smart Attendance System

## Yêu cầu hệ thống

- JDK 17 trở lên
- Apache Tomcat 9.x hoặc 10.x
- MySQL 8.0 trở lên
- Maven 3.6 trở lên

## Bước 1: Chuẩn bị Database

### 1.1 Tạo Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE smart_attendance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

### 1.2 Import Schema

```bash
mysql -u root -p smart_attendance < src/main/resources/database/schema.sql
```

## Bước 2: Cấu hình ứng dụng

### 2.1 Chỉnh sửa application.properties

Mở file `src/main/resources/application.properties` và cập nhật:

```properties
db.url=jdbc:mysql://localhost:3306/smart_attendance?useSSL=false&serverTimezone=UTC
db.username=root
db.password=YOUR_PASSWORD_HERE
```

## Bước 3: Build Project

### 3.1 Build với Maven

```bash
mvn clean package
```

Kết quả sẽ tạo file `target/SmartAttendance.war`

### 3.2 Kiểm tra build thành công

Đảm bảo không có lỗi trong quá trình build và file WAR được tạo thành công.

## Bước 4: Deploy lên Tomcat

### 4.1 Sử dụng Tomcat Manager (Recommended)

1. Truy cập Tomcat Manager: `http://localhost:8080/manager`
2. Đăng nhập với tài khoản admin
3. Tìm phần "WAR file to deploy"
4. Chọn file `SmartAttendance.war`
5. Click "Deploy"

### 4.2 Deploy thủ công

```bash
# Copy file WAR vào thư mục webapps của Tomcat
cp target/SmartAttendance.war /path/to/tomcat/webapps/

# Restart Tomcat
/path/to/tomcat/bin/shutdown.sh
/path/to/tomcat/bin/startup.sh
```

## Bước 5: Kiểm tra ứng dụng

### 5.1 Truy cập ứng dụng

Mở trình duyệt và truy cập:

```
http://localhost:8080/SmartAttendance
```

### 5.2 Đăng nhập với tài khoản demo

**Giáo viên:**

- Username: `teacher`
- Password: `password`

**Sinh viên:**

- Username: `student`
- Password: `password`

## Troubleshooting

### Lỗi kết nối Database

1. Kiểm tra MySQL đang chạy:

```bash
sudo systemctl status mysql
```

2. Kiểm tra thông tin kết nối trong `application.properties`

3. Kiểm tra user có quyền truy cập database:

```sql
GRANT ALL PRIVILEGES ON smart_attendance.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### Lỗi 404 Not Found

1. Đảm bảo file WAR đã được deploy thành công
2. Kiểm tra logs của Tomcat: `tail -f /path/to/tomcat/logs/catalina.out`
3. Xóa cache của Tomcat:

```bash
rm -rf /path/to/tomcat/work/Catalina/localhost/SmartAttendance
```

### Lỗi Spring Security

1. Kiểm tra SecurityConfig đã được load
2. Xem logs để tìm lỗi cụ thể
3. Đảm bảo BCryptPasswordEncoder được cấu hình đúng

### Lỗi Thymeleaf Template

1. Kiểm tra đường dẫn template trong WebMvcConfig
2. Đảm bảo các file .html nằm đúng vị trí: `/WEB-INF/views/`
3. Xóa cache Thymeleaf bằng cách set `cacheable=false`

## Production Deployment

### 1. Bật SSL/TLS

Cấu hình HTTPS trong Tomcat để bảo mật dữ liệu GPS và ảnh selfie.

### 2. Tối ưu Database

```sql
-- Tạo indexes cho performance
CREATE INDEX idx_attendance_date ON attendance_records(checked_in_at);
CREATE INDEX idx_session_class ON class_sessions(class_id, session_date);
```

### 3. Cấu hình Production Properties

```properties
hibernate.show_sql=false
hibernate.format_sql=false
hibernate.hbm2ddl.auto=validate
```

### 4. Bật Thymeleaf Cache

Trong `WebMvcConfig.java`:

```java
templateResolver.setCacheable(true);
```

### 5. Setup Logging

Tạo file `logback.xml` trong `src/main/resources`:

```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/smartattendance/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/smartattendance/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

## Monitoring

### 1. Kiểm tra logs

```bash
tail -f /var/log/smartattendance/app.log
```

### 2. Monitor Database

```sql
-- Kiểm tra số lượng attendance records
SELECT COUNT(*) FROM attendance_records;

-- Kiểm tra active sessions
SELECT * FROM class_sessions WHERE status = 'IN_PROGRESS';
```

### 3. Performance Monitoring

Sử dụng JConsole hoặc VisualVM để monitor JVM:

```bash
jconsole
```

## Backup

### Database Backup

```bash
# Daily backup
mysqldump -u root -p smart_attendance > backup_$(date +%Y%m%d).sql

# Restore
mysql -u root -p smart_attendance < backup_20241222.sql
```

## Support

Nếu gặp vấn đề, vui lòng:

1. Kiểm tra logs: `/var/log/smartattendance/app.log`
2. Kiểm tra Tomcat logs: `catalina.out`
3. Xem hướng dẫn trong README.md
