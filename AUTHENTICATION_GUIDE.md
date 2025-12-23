# Smart Attendance - Authentication & Authorization Guide

## 🔐 Tổng quan hệ thống xác thực

### Công nghệ sử dụng

- **Spring Security 6.2.1** - Framework bảo mật
- **BCrypt Password Encoder** - Mã hóa mật khẩu
- **Role-Based Access Control (RBAC)** - Phân quyền theo vai trò

---

## 👥 Vai trò trong hệ thống

### 1. ADMIN (Quản trị viên)

- **Quyền truy cập**: `/admin/**`
- **Chức năng**:
  - Quản lý giảng viên (CRUD)
  - Quản lý sinh viên (CRUD)
  - Quản lý lớp học (CRUD)
  - Xem thống kê tổng quan
  - Kích hoạt/Vô hiệu hóa tài khoản

### 2. TEACHER (Giảng viên)

- **Quyền truy cập**: `/teacher/**`
- **Chức năng**:
  - Tạo và quản lý lớp học
  - Thêm/xóa sinh viên khỏi lớp
  - Tạo buổi học và QR code
  - Xem danh sách điểm danh
  - Xuất báo cáo Excel
  - Quản lý sessions

### 3. STUDENT (Sinh viên)

- **Quyền truy cập**: `/student/**`
- **Chức năng**:
  - Xem lớp học đã đăng ký
  - Quét QR code để điểm danh
  - Xem lịch sử điểm danh cá nhân
  - Xem thống kê điểm danh

---

## 🔑 Authentication Flow

### 1. Đăng ký tài khoản (`/register`)

**Endpoint**: `POST /register`

**Form Fields**:

```java
- fullName: String (required)
- username: String (required, alphanumeric only)
- password: String (required, min 6 characters)
- confirmPassword: String (required, must match password)
- email: String (required, valid email format)
- phoneNumber: String (optional, 10-11 digits)
- role: Enum (STUDENT | TEACHER)
```

**Validation Rules**:

- Username: Chỉ chứa chữ cái và số
- Password: Tối thiểu 6 ký tự
- Password confirmation: Phải khớp với password
- Email: Định dạng email hợp lệ
- Role: Chỉ cho phép đăng ký STUDENT hoặc TEACHER (không cho phép ADMIN)

**Process**:

```
1. User submits registration form
2. System validates input data
3. Check if username/email already exists
4. Hash password using BCrypt
5. Create new User entity with isActive = true
6. Save to database
7. Redirect to login page with success message
```

**Error Handling**:

- Username đã tồn tại → "Username already exists"
- Email đã tồn tại → "Email already exists"
- Password không khớp → "Mật khẩu và xác nhận mật khẩu không khớp!"
- Vai trò không hợp lệ → "Vai trò không hợp lệ!"

---

### 2. Đăng nhập (`/login`)

**Endpoint**: `POST /login`

**Form Fields**:

```java
- username: String (required)
- password: String (required)
- remember-me: Boolean (optional)
```

**Authentication Process**:

```
1. User submits login form
2. Spring Security intercepts at /login
3. DaoAuthenticationProvider calls UserService.loadUserByUsername()
4. System retrieves User from database
5. Check if user.isActive = true
6. BCrypt compares submitted password with stored hash
7. If valid:
   - Create Authentication object
   - Set SecurityContext
   - CustomAuthenticationSuccessHandler redirects by role
8. If invalid:
   - Redirect to /login?error=true
```

**Success Redirects** (CustomAuthenticationSuccessHandler):

- ADMIN → `/admin/dashboard`
- TEACHER → `/teacher/dashboard`
- STUDENT → `/student/dashboard`

**Error States**:

- Wrong credentials → `/login?error=true`
- User inactive → UsernameNotFoundException
- After logout → `/login?logout=true`

---

### 3. Authorization (Phân quyền)

**URL Pattern Security** (SecurityConfig):

```java
// Public access
.requestMatchers("/", "/login", "/register").permitAll()
.requestMatchers("/resources/**", "/css/**", "/js/**", "/images/**").permitAll()

// API endpoints
.requestMatchers("/api/attendance/checkin").hasRole("STUDENT")
.requestMatchers("/api/qr/**").hasRole("TEACHER")

// Role-based pages
.requestMatchers("/teacher/**").hasRole("TEACHER")
.requestMatchers("/student/**").hasRole("STUDENT")
.requestMatchers("/admin/**").hasRole("ADMIN")

// All other requests require authentication
.anyRequest().authenticated()
```

**Method-Level Security**:

```java
// In AdminController
@PreAuthorize("hasRole('ADMIN')")
public class AdminController { ... }

// Individual methods can also use @PreAuthorize
@PreAuthorize("hasRole('TEACHER')")
@GetMapping("/teacher/dashboard")
public String dashboard() { ... }
```

---

### 4. Đăng xuất (`/logout`)

**Endpoint**: `GET /logout` (với CSRF protection)

**Process**:

```
1. User clicks logout button/link
2. Spring Security invalidates session
3. Deletes JSESSIONID cookie
4. Clears SecurityContext
5. Redirects to /login?logout=true
```

**Configuration**:

```java
.logout(logout -> logout
    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
    .logoutSuccessUrl("/login?logout=true")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .permitAll())
```

---

## 🛡️ Security Features

### 1. Password Encryption

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- Sử dụng BCrypt với salt tự động
- Không lưu mật khẩu dạng plain text
- Hash không thể reverse

### 2. CSRF Protection

```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**")) // Disable for API
```

- Enabled cho tất cả form submissions
- Disabled cho REST API endpoints
- Thymeleaf tự động thêm CSRF token

### 3. Access Denied Handling

```java
.exceptionHandling(ex -> ex
    .accessDeniedPage("/access-denied"))
```

- Custom error page cho 403 Forbidden
- View: `error/access-denied.html`

### 4. Session Management

- Session invalidation on logout
- Cookie deletion
- SecurityContext cleanup

---

## 📋 User Interface Pages

### Authentication Pages

#### 1. Login Page (`/login`)

- **View**: `auth/login.html`
- **Features**:
  - Username/password fields
  - Remember-me checkbox
  - Error message display
  - Link to registration
  - Link to homepage
- **URL**: `http://localhost:8086/SmartAttendance/login`

#### 2. Registration Page (`/register`)

- **View**: `auth/register.html`
- **Features**:
  - Full name, username, password fields
  - Password confirmation with JS validation
  - Email and phone number
  - Role selection (Student/Teacher only)
  - Terms & conditions checkbox
  - Link to login page
- **URL**: `http://localhost:8086/SmartAttendance/register`

#### 3. Access Denied Page (`/access-denied`)

- **View**: `error/access-denied.html`
- **Features**:
  - 403 error message
  - Explanation of access restriction
  - Links to: Homepage, Back, Logout
- **URL**: Automatic redirect when unauthorized

#### 4. Landing Page (`/`)

- **View**: `index.html`
- **Features**:
  - Hero section with CTA buttons
  - Feature showcase
  - How it works guide
  - User roles overview
  - Dynamic nav based on authentication status
- **URL**: `http://localhost:8086/SmartAttendance/`

---

## 🔄 Complete User Journeys

### Journey 1: New Student Registration

```
1. Visit homepage (/)
2. Click "Đăng ký ngay" → /register
3. Fill form:
   - Full name: "Nguyễn Văn A"
   - Username: "20210001"
   - Password: "password123"
   - Confirm password: "password123"
   - Email: "student@example.com"
   - Role: STUDENT
4. Submit form
5. System validates and creates account
6. Redirect to /login with success message
7. Enter credentials and login
8. CustomAuthenticationSuccessHandler redirects to /student/dashboard
9. Student can now:
   - View enrolled classes
   - Scan QR codes
   - Check attendance history
```

### Journey 2: Teacher Access

```
1. Visit /login
2. Enter teacher credentials
3. System authenticates
4. Redirect to /teacher/dashboard
5. Teacher sees:
   - Total classes
   - Total students
   - Today's sessions
   - Class list with quick actions
6. Teacher can:
   - Create new class
   - Add students via Excel
   - Create sessions
   - Generate QR codes
   - View attendance reports
```

### Journey 3: Admin Management

```
1. Login as ADMIN
2. Redirect to /admin/dashboard
3. Admin sees:
   - Total teachers
   - Total students
   - Active classes count
4. Admin can:
   - Manage all users (teachers & students)
   - Create/edit/delete classes
   - Toggle user active status
   - View system-wide statistics
```

### Journey 4: Unauthorized Access Attempt

```
1. Student logs in successfully
2. Student tries to access /admin/dashboard
3. Spring Security intercepts request
4. Checks roles: Student does NOT have ROLE_ADMIN
5. Throws AccessDeniedException
6. Redirects to /access-denied
7. Shows 403 error page with helpful links
```

---

## 🧪 Testing Authentication

### Test Cases

#### 1. Registration Tests

```
✅ Valid registration (Student)
✅ Valid registration (Teacher)
❌ Duplicate username
❌ Duplicate email
❌ Password mismatch
❌ Invalid email format
❌ Username with special characters
❌ Password too short (< 6 chars)
❌ Attempt to register as ADMIN
```

#### 2. Login Tests

```
✅ Valid credentials (Teacher)
✅ Valid credentials (Student)
✅ Valid credentials (Admin)
❌ Wrong username
❌ Wrong password
❌ Inactive user account
❌ Empty username
❌ Empty password
```

#### 3. Authorization Tests

```
✅ Teacher accesses /teacher/dashboard
✅ Student accesses /student/dashboard
✅ Admin accesses /admin/dashboard
❌ Student tries to access /teacher/dashboard → 403
❌ Teacher tries to access /admin/dashboard → 403
❌ Unauthenticated user tries /student/dashboard → Redirect to login
```

#### 4. Session Tests

```
✅ Login → Session created
✅ Logout → Session invalidated
✅ Session persists across page navigation
✅ After logout, cannot access protected pages
```

---

## 📊 Database Schema

### User Entity

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hash
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Sample Data

```sql
-- Admin account
INSERT INTO users (username, password, full_name, email, role, is_active)
VALUES ('admin', '$2a$10$...', 'Administrator', 'admin@smartattendance.com', 'ADMIN', true);

-- Teacher account
INSERT INTO users (username, password, full_name, email, role, is_active)
VALUES ('teacher01', '$2a$10$...', 'Nguyễn Văn Giảng', 'teacher@example.com', 'TEACHER', true);

-- Student account
INSERT INTO users (username, password, full_name, email, role, is_active)
VALUES ('20210001', '$2a$10$...', 'Trần Thị Học', 'student@example.com', 'STUDENT', true);
```

---

## 🔧 Configuration Files

### SecurityConfig.java

```java
- CustomAuthenticationSuccessHandler: Role-based redirect
- BCryptPasswordEncoder: Password encryption
- DaoAuthenticationProvider: User authentication
- HttpSecurity: URL-based authorization
- CSRF configuration: API exemption
- Logout configuration: Session cleanup
```

### UserService.java

```java
- Implements UserDetailsService
- loadUserByUsername(): Spring Security integration
- createUser(): Registration with password encoding
- updateUser(): Profile management
- User activation/deactivation
```

### HomeController.java

```java
- GET /: Landing page
- GET /register: Registration form
- POST /register: Process registration
- GET /dashboard: Role-based redirect (fallback)
- GET /access-denied: 403 error page
```

---

## 🚀 Deployment Checklist

- [x] BCrypt password encoding enabled
- [x] CSRF protection configured
- [x] Role-based access control implemented
- [x] Custom success handler for login redirect
- [x] Access denied page created
- [x] Registration page with validation
- [x] Login page with error handling
- [x] Session management configured
- [x] Public resources accessible
- [x] API endpoints secured by role

---

## 📝 Notes & Best Practices

### Security Best Practices

1. ✅ Never store passwords in plain text
2. ✅ Use BCrypt with default strength (10 rounds)
3. ✅ Validate all user inputs (client & server side)
4. ✅ Use HTTPS in production
5. ✅ Implement rate limiting for login attempts
6. ✅ Log authentication events
7. ✅ Use CSRF tokens for state-changing operations

### Code Quality

1. ✅ Separation of concerns (Controller → Service → Repository)
2. ✅ Consistent error handling with flash messages
3. ✅ Clear navigation and user feedback
4. ✅ Responsive design for all authentication pages
5. ✅ Accessibility features (labels, ARIA)

### Future Enhancements

- [ ] Email verification for registration
- [ ] Password reset functionality
- [ ] Two-factor authentication (2FA)
- [ ] OAuth2 social login (Google, Facebook)
- [ ] Account lockout after failed attempts
- [ ] Password strength meter
- [ ] User profile management page
- [ ] Admin approval for teacher accounts

---

**Last Updated**: December 23, 2025  
**Build Status**: ✅ SUCCESS  
**Security**: Spring Security 6.2.1 + BCrypt
