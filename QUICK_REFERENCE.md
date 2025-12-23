# Quick Reference - Smart Attendance URLs

## 🌐 Base URL

```
http://localhost:8086/SmartAttendance
```

## 🔓 Public URLs (No authentication required)

| URL              | Page         | Description                             |
| ---------------- | ------------ | --------------------------------------- |
| `/`              | Landing Page | Homepage với feature showcase           |
| `/login`         | Login Page   | Đăng nhập vào hệ thống                  |
| `/register`      | Registration | Đăng ký tài khoản mới (Student/Teacher) |
| `/access-denied` | Error 403    | Trang lỗi khi không có quyền truy cập   |

---

## 👨‍💼 Admin URLs (Require ROLE_ADMIN)

### Dashboard

- `GET /admin/dashboard` - Trang tổng quan admin

### Teachers Management

- `GET /admin/teachers` - Danh sách giảng viên
- `GET /admin/teachers/create` - Form tạo giảng viên
- `POST /admin/teachers/create` - Xử lý tạo giảng viên
- `GET /admin/teachers/edit/{id}` - Form sửa giảng viên
- `POST /admin/teachers/edit/{id}` - Xử lý cập nhật
- `POST /admin/teachers/toggle-status/{id}` - Bật/tắt trạng thái

### Students Management

- `GET /admin/students` - Danh sách sinh viên
- `GET /admin/students/create` - Form tạo sinh viên
- `POST /admin/students/create` - Xử lý tạo sinh viên
- `GET /admin/students/edit/{id}` - Form sửa sinh viên
- `POST /admin/students/edit/{id}` - Xử lý cập nhật
- `POST /admin/students/toggle-status/{id}` - Bật/tắt trạng thái

### Classes Management

- `GET /admin/classes` - Danh sách lớp học
- `GET /admin/classes/create` - Form tạo lớp
- `POST /admin/classes/create` - Xử lý tạo lớp
- `GET /admin/classes/{id}` - Chi tiết lớp học
- `GET /admin/classes/edit/{id}` - Form sửa lớp
- `POST /admin/classes/edit/{id}` - Xử lý cập nhật
- `POST /admin/classes/toggle-status/{id}` - Bật/tắt trạng thái

---

## 👨‍🏫 Teacher URLs (Require ROLE_TEACHER)

### Dashboard & Overview

- `GET /teacher/dashboard` - Dashboard giảng viên (stats + classes)
- `GET /teacher/classes` - Danh sách lớp của tôi

### Class Management

- `GET /teacher/classes/create` - Form tạo lớp mới
- `POST /teacher/classes/create` - Xử lý tạo lớp
- `GET /teacher/classes/{id}` - Chi tiết lớp + sessions
- `GET /teacher/classes/{id}/edit` - Form sửa lớp
- `POST /teacher/classes/{id}/edit` - Xử lý cập nhật

### Student Management

- `POST /teacher/classes/{classId}/import-students` - Import Excel
- `POST /teacher/classes/{classId}/add-student` - Thêm 1 sinh viên
- `POST /teacher/classes/{classId}/remove-student/{studentId}` - Xóa sinh viên

### Session Management

- `GET /teacher/sessions/create?classId={id}` - Form tạo buổi học
- `POST /teacher/sessions/create` - Xử lý tạo session
- `GET /teacher/sessions/{id}/attendance` - Xem danh sách điểm danh
- `GET /teacher/sessions/{id}/qr` - Trang QR scanner

---

## 👨‍🎓 Student URLs (Require ROLE_STUDENT)

### Dashboard & Classes

- `GET /student/dashboard` - Dashboard sinh viên
- `GET /student/classes` - Danh sách lớp đã đăng ký
- `GET /student/classes/{id}` - Chi tiết lớp + lịch sử điểm danh

### Attendance

- `GET /student/scan-qr` - Trang quét QR code
- `GET /student/attendance-history` - Lịch sử điểm danh toàn bộ

---

## 🔌 API Endpoints

### QR Code API (TEACHER only)

```
POST /api/qr/generate
Parameters:
  - sessionId: Long
  - latitude: Double
  - longitude: Double
  - expirationMinutes: Integer (default=5)
  - maxDistanceMeters: Integer (default=50)
Response: QRSessionDTO (JSON)
```

### Attendance API

**Check-in (STUDENT only)**

```
POST /api/attendance/checkin
Content-Type: application/json
Body: {
  "qrToken": "string",
  "latitude": 0.0,
  "longitude": 0.0,
  "deviceInfo": "string",
  "ipAddress": "string"
}
Response: AttendanceResponse (JSON)
```

**Get Session Attendance**

```
GET /api/attendance/session/{sessionId}
Response: List<AttendanceRecord> (JSON)
```

---

## 🔐 Authentication Endpoints

### Login

```
POST /login
Form Data:
  - username: String
  - password: String
  - remember-me: Boolean (optional)
Success: Redirect to /{role}/dashboard
Failure: Redirect to /login?error=true
```

### Register

```
POST /register
Form Data:
  - fullName: String
  - username: String
  - password: String
  - confirmPassword: String
  - email: String
  - phoneNumber: String (optional)
  - role: STUDENT | TEACHER
Success: Redirect to /login
Failure: Redirect to /register with error
```

### Logout

```
GET /logout
Effect: Invalidate session, delete cookies
Redirect: /login?logout=true
```

---

## 📁 Static Resources

- `/resources/**` - Tài nguyên tĩnh
- `/css/**` - CSS files
- `/js/**` - JavaScript files
- `/images/**` - Image files

---

## 🎨 View Templates Location

```
src/main/webapp/WEB-INF/views/
├── index.html                    # Landing page
├── layout.html                   # Base layout
├── auth/
│   ├── login.html               # Login page
│   └── register.html            # Registration page
├── error/
│   └── access-denied.html       # 403 error page
├── admin/
│   ├── dashboard.html
│   ├── teachers.html
│   ├── teacher-form.html
│   ├── students.html
│   ├── student-form.html
│   ├── classes.html
│   ├── class-form.html
│   └── class-detail.html
├── teacher/
│   ├── dashboard.html
│   ├── classes.html
│   ├── class-form.html
│   ├── class-detail.html
│   ├── session-form.html
│   ├── attendance.html
│   └── qr-scanner.html
└── student/
    ├── dashboard.html
    ├── classes.html
    ├── class-detail.html
    ├── scan-qr.html
    └── attendance-history.html
```

---

## 🧪 Test Accounts (After initial setup)

```java
// Create these manually in database or via admin panel

// Admin
username: admin
password: admin123
role: ADMIN

// Teacher
username: teacher01
password: teacher123
role: TEACHER

// Student
username: 20210001
password: student123
role: STUDENT
```

---

## 🚀 Quick Start Commands

### Build

```bash
mvn clean package -DskipTests
```

### Deploy

```bash
# Copy to Tomcat
cp target/SmartAttendance.war /path/to/tomcat/webapps/

# Start Tomcat
./catalina.sh start  # Linux/Mac
catalina.bat start   # Windows
```

### Access

```
Homepage: http://localhost:8086/SmartAttendance/
Login: http://localhost:8086/SmartAttendance/login
Register: http://localhost:8086/SmartAttendance/register
```

---

## 📊 HTTP Status Codes

| Code | Meaning      | Common Scenarios               |
| ---- | ------------ | ------------------------------ |
| 200  | OK           | Successful request             |
| 302  | Redirect     | After login/logout/submit form |
| 403  | Forbidden    | Access denied (wrong role)     |
| 404  | Not Found    | URL không tồn tại              |
| 500  | Server Error | Exception trong code           |

---

## 🔍 Debugging Tips

### Check if authenticated:

```
Visit: /student/dashboard
If redirects to /login → Not authenticated
If shows 403 → Authenticated but wrong role
If shows page → Success
```

### Check current user role:

```java
// In controller
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
auth.getAuthorities(); // Shows: [ROLE_STUDENT], [ROLE_TEACHER], etc.
```

### View session info:

```java
// In controller
HttpSession session = request.getSession(false);
if (session != null) {
    Enumeration<String> attrs = session.getAttributeNames();
    // List all session attributes
}
```

---

## 📝 Common Issues & Solutions

### Issue: 404 on /dashboard

**Solution**: Use role-specific URLs:

- Admin: `/admin/dashboard`
- Teacher: `/teacher/dashboard`
- Student: `/student/dashboard`

### Issue: 403 Forbidden

**Solution**: Check if user has correct role for the URL

### Issue: Redirect loop on login

**Solution**: Check CustomAuthenticationSuccessHandler is working

### Issue: CSRF token error on form submit

**Solution**: Ensure Thymeleaf form uses `th:action="@{/url}"`

### Issue: Session lost after redirect

**Solution**: Check Tomcat session configuration

---

**Quick Access**:

- Full Routes: [ROUTES_AND_VIEWS.md](ROUTES_AND_VIEWS.md)
- Authentication Guide: [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md)
