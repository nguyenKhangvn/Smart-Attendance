# Luồng xử lý xem danh sách điểm danh buổi học

## Endpoint

```
GET /teacher/sessions/{id}/attendance
```

## Mô tả

Hiển thị danh sách điểm danh chi tiết của một buổi học, bao gồm thống kê và trạng thái điểm danh của từng sinh viên.

---

## 1. CONTROLLER LAYER

**File:** `TeacherController.java`

### Phương thức: `viewAttendance`

```java
@GetMapping("/sessions/{id}/attendance")
public String viewAttendance(@PathVariable Long id, Model model, Authentication authentication)
```

### Chức năng:

1. **Xác thực giáo viên:**

   - Lấy thông tin giáo viên từ `Authentication`
   - Kiểm tra xem giáo viên có tồn tại không

2. **Lấy thông tin buổi học:**

   - Gọi `classSessionService.findById(id)` để lấy thông tin buổi học
   - Kiểm tra quyền sở hữu: Buổi học phải thuộc về lớp của giáo viên đang đăng nhập

3. **Lấy dữ liệu điểm danh:**

   - Gọi `attendanceService.getAttendanceBySession(id)` để lấy tất cả records điểm danh
   - Tạo `attendanceMap` để tra cứu nhanh record theo student ID

4. **Tính toán thống kê:**

   - `totalStudents`: Tổng số sinh viên trong lớp
   - `presentCount`: Số sinh viên đã điểm danh thành công (status = SUCCESS)
   - `absentCount`: Số sinh viên vắng mặt
   - `attendanceRate`: Tỷ lệ điểm danh (%)

5. **Đưa dữ liệu vào Model:**

   ```java
   model.addAttribute("attendanceSession", session);
   model.addAttribute("classEntity", session.getClassEntity());
   model.addAttribute("attendanceRecords", attendanceRecords);
   model.addAttribute("attendanceMap", attendanceMap);
   model.addAttribute("totalStudents", totalStudents);
   model.addAttribute("presentCount", presentCount);
   model.addAttribute("absentCount", absentCount);
   model.addAttribute("attendanceRate", attendanceRate);
   ```

6. **Return view:** `teacher/attendance`

### Dependencies được inject:

```java
@Autowired
private IClassSessionService classSessionService;

@Autowired
private IAttendanceService attendanceService;

@Autowired
private IUserService userService;
```

---

## 2. SERVICE LAYER

### A. AttendanceService

**File:** `AttendanceService.java`

#### Phương thức: `getAttendanceBySession`

```java
@Override
public List<AttendanceRecord> getAttendanceBySession(Long sessionId)
```

**Chức năng:**

- Tạo đối tượng `ClassSession` với ID
- Gọi repository để lấy tất cả records điểm danh của buổi học
- Return danh sách `AttendanceRecord`

**Dependency:**

```java
@Autowired
private AttendanceRecordRepository attendanceRecordRepository;
```

### B. ClassSessionService

**File:** `ClassSessionService.java`

#### Phương thức: `findById`

```java
@Override
public ClassSession findById(Long id)
```

**Chức năng:**

- Tìm buổi học theo ID
- Throw exception nếu không tìm thấy
- Return `ClassSession` với đầy đủ relationships (ClassEntity, AttendanceRecords)

---

## 3. REPOSITORY LAYER

### A. AttendanceRecordRepository

**File:** `AttendanceRecordRepository.java`

```java
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>
```

#### Query method: `findByClassSession`

```java
List<AttendanceRecord> findByClassSession(ClassSession classSession);
```

**Chức năng:**

- JPA tự động generate query để lấy tất cả records theo buổi học
- SQL tương đương:
  ```sql
  SELECT * FROM attendance_records WHERE session_id = ?
  ```

### B. ClassSessionRepository

**File:** `ClassSessionRepository.java`

```java
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long>
```

#### Phương thức kế thừa: `findById`

- Lấy buổi học theo ID
- Eager/Lazy load relationships theo cấu hình entity

---

## 4. ENTITY LAYER

### A. ClassSession

**File:** `ClassSession.java`

```java
@Entity
@Table(name = "class_sessions")
public class ClassSession
```

**Attributes:**

- `id`: Primary key
- `sessionName`: Tên buổi học
- `sessionDate`: Ngày giờ diễn ra
- `durationMinutes`: Thời lượng
- `status`: SCHEDULED | IN_PROGRESS | COMPLETED
- `notes`: Ghi chú

**Relationships:**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "class_id")
private ClassEntity classEntity;

@OneToMany(mappedBy = "classSession", cascade = CascadeType.ALL)
private Set<AttendanceRecord> attendanceRecords;
```

### B. AttendanceRecord

**File:** `AttendanceRecord.java`

```java
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord
```

**Attributes:**

- `id`: Primary key
- `studentLatitude`, `studentLongitude`: Tọa độ sinh viên
- `distanceMeters`: Khoảng cách tính được
- `deviceUid`: Device fingerprint
- `faceDataUrl`: URL ảnh selfie
- `status`: AttendanceStatus enum
- `failReason`: Lý do thất bại
- `checkedInAt`: Thời gian check-in
- `isManual`: Điểm danh thủ công hay không
- `modifiedBy`, `modificationNote`: Thông tin sửa đổi

**AttendanceStatus enum:**

```java
public enum AttendanceStatus {
    SUCCESS,                // Điểm danh thành công
    FAILED_INVALID_QR,     // QR không hợp lệ
    FAILED_DISTANCE,       // Ngoài phạm vi
    FAILED_DUPLICATE_DEVICE, // Thiết bị đã dùng
    FAILED_ALREADY_CHECKED,  // Đã điểm danh
    ABSENT,                // Vắng
    LATE                   // Đi muộn
}
```

**Relationships:**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "student_id")
private User student;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "session_id")
private ClassSession classSession;
```

### C. ClassEntity

**File:** `ClassEntity.java`

**Relationships:**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "teacher_id")
private User teacher;

@ManyToMany
@JoinTable(
    name = "class_students",
    joinColumns = @JoinColumn(name = "class_id"),
    inverseJoinColumns = @JoinColumn(name = "student_id")
)
private Set<User> students;

@OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
private Set<ClassSession> sessions;
```

---

## 5. VIEW LAYER

**File:** `attendance.html`

### Cấu trúc giao diện:

#### A. Header Section

- Tiêu đề trang
- Breadcrumb navigation
- Nút "Quay lại"

#### B. Session Info Card

Hiển thị thông tin buổi học:

- Tên lớp và môn học
- Tên buổi học
- Thời gian diễn ra
- Thời lượng
- Trạng thái (Đã lên lịch / Đang diễn ra / Hoàn thành)
- Ghi chú (nếu có)

#### C. Statistics Cards

4 thẻ thống kê:

1. **Tổng số sinh viên** (Primary - Blue)
2. **Đã điểm danh** (Success - Green)
3. **Vắng mặt** (Danger - Red)
4. **Tỷ lệ điểm danh %** (Info - Cyan)

#### D. Attendance Table

Bảng chi tiết danh sách:

**Columns:**

- STT
- Mã SV
- Họ và tên
- Email
- Trạng thái (với màu sắc và icon phù hợp)
- Thời gian điểm danh
- Khoảng cách (m)
- Ghi chú

**Trạng thái hiển thị:**

```html
<!-- SUCCESS: Có mặt - Green -->
<span class="badge bg-success">
  <i class="fas fa-check-circle"></i> Có mặt
</span>

<!-- LATE: Đi muộn - Yellow -->
<span class="badge bg-warning"> <i class="fas fa-clock"></i> Đi muộn </span>

<!-- FAILED_DISTANCE: Quá xa - Red -->
<span class="badge bg-danger">
  <i class="fas fa-map-marker-alt"></i> Quá xa
</span>

<!-- ABSENT: Vắng - Gray -->
<span class="badge bg-secondary">
  <i class="fas fa-times-circle"></i> Vắng
</span>
```

#### E. Action Buttons

- **Xuất Excel**: Export bảng điểm danh
- **In**: In trang hiện tại

### Thymeleaf Logic:

```html
<!-- Lặp qua tất cả sinh viên trong lớp -->
<tr th:each="student, iterStat : ${classEntity.students}">
  <!-- Lookup attendance record từ map -->
  <th:block th:with="record=${attendanceMap.get(student.id)}">
    <!-- Hiển thị trạng thái dựa vào record -->
    <span
      th:if="${record != null && record.status.name() == 'SUCCESS'}"
      class="badge bg-success"
    >
      <i class="fas fa-check-circle"></i> Có mặt
    </span>

    <span th:if="${record == null}" class="badge bg-secondary">
      <i class="fas fa-times-circle"></i> Vắng
    </span>
  </th:block>
</tr>
```

---

## 6. LUỒNG DỮ LIỆU (Data Flow)

```
HTTP Request
    ↓
[TeacherController]
    ↓
    ├─→ UserService.findByUsername()
    │       ↓
    │   UserRepository.findByUsername()
    │       ↓
    │   Return: User (Teacher)
    │
    ├─→ ClassSessionService.findById()
    │       ↓
    │   ClassSessionRepository.findById()
    │       ↓
    │   Return: ClassSession (with ClassEntity, AttendanceRecords)
    │
    ├─→ Check ownership: session.teacher == currentTeacher
    │
    ├─→ AttendanceService.getAttendanceBySession()
    │       ↓
    │   AttendanceRecordRepository.findByClassSession()
    │       ↓
    │   SQL: SELECT * FROM attendance_records WHERE session_id = ?
    │       ↓
    │   Return: List<AttendanceRecord>
    │
    ├─→ Build attendanceMap: Map<studentId, AttendanceRecord>
    │
    ├─→ Calculate statistics
    │   • totalStudents = classEntity.students.size()
    │   • presentCount = count(status == SUCCESS)
    │   • absentCount = totalStudents - attendanceRecords.size()
    │   • attendanceRate = (presentCount / totalStudents) * 100
    │
    ↓
[Model] - Add all data
    ↓
Return "teacher/attendance"
    ↓
[Thymeleaf Template Engine]
    ↓
    ├─→ Render session info
    ├─→ Render statistics cards
    └─→ Render attendance table
        ├─→ Loop through students
        ├─→ Lookup record in attendanceMap
        └─→ Display status with colors & icons
    ↓
HTML Response to Browser
```

---

## 7. DATABASE SCHEMA

### Bảng: `class_sessions`

```sql
CREATE TABLE class_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_name VARCHAR(200) NOT NULL,
    session_date DATETIME NOT NULL,
    duration_minutes INT,
    notes TEXT,
    status VARCHAR(20) NOT NULL,
    class_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (class_id) REFERENCES classes(id)
);
```

### Bảng: `attendance_records`

```sql
CREATE TABLE attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    student_latitude DOUBLE NOT NULL,
    student_longitude DOUBLE NOT NULL,
    distance_meters DOUBLE NOT NULL,
    device_uid VARCHAR(200) NOT NULL,
    face_data_url TEXT,
    status VARCHAR(20) NOT NULL,
    fail_reason VARCHAR(500),
    checked_in_at DATETIME NOT NULL,
    is_manual BOOLEAN DEFAULT FALSE,
    modified_by BIGINT,
    modification_note VARCHAR(500),
    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (session_id) REFERENCES class_sessions(id)
);
```

### Bảng: `classes`

```sql
CREATE TABLE classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_code VARCHAR(50) UNIQUE NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    semester VARCHAR(50),
    teacher_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);
```

### Bảng: `class_students` (Many-to-Many)

```sql
CREATE TABLE class_students (
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (student_id) REFERENCES users(id)
);
```

---

## 8. SECURITY & VALIDATION

### Authorization Check:

```java
// Verify teacher owns this class
if (!session.getClassEntity().getTeacher().getId().equals(teacher.getId())) {
    throw new RuntimeException("Unauthorized access to this session");
}
```

### Access Control:

- Chỉ giáo viên sở hữu lớp học mới được xem điểm danh
- Sử dụng Spring Security Authentication để xác thực
- Role: `TEACHER`

---

## 9. FEATURES & FUNCTIONS

### Đã implement:

✅ Xem danh sách điểm danh theo buổi học  
✅ Hiển thị thống kê tổng quan  
✅ Hiển thị chi tiết từng sinh viên  
✅ Phân biệt trạng thái điểm danh bằng màu sắc  
✅ Hiển thị thời gian và khoảng cách  
✅ Xuất Excel đơn giản  
✅ Chức năng in  
✅ Responsive design

### Có thể mở rộng:

🔲 Xuất Excel nâng cao (với format đẹp)  
🔲 Xuất PDF  
🔲 Filter theo trạng thái  
🔲 Tìm kiếm sinh viên  
🔲 Sắp xếp theo các cột  
🔲 Chỉnh sửa trạng thái điểm danh (manual override)  
🔲 Xem ảnh selfie điểm danh  
🔲 Xem vị trí trên bản đồ  
🔲 Gửi email thông báo cho sinh viên vắng  
🔲 Biểu đồ thống kê

---

## 10. ERROR HANDLING

### Các trường hợp lỗi:

1. **Session không tồn tại:**

   ```java
   classSessionService.findById(id)
   // Throws: RuntimeException("Session not found")
   ```

2. **Teacher không tồn tại:**

   ```java
   userService.findByUsername(username)
   // Throws: RuntimeException("Teacher not found")
   ```

3. **Unauthorized access:**

   ```java
   if (!session.teacher.equals(currentTeacher))
   // Throws: RuntimeException("Unauthorized access")
   ```

4. **Database connection error:**
   - Handled by Spring exception handlers
   - Show error page

---

## 11. TESTING

### Test cases cần kiểm tra:

#### Unit Tests:

- `AttendanceService.getAttendanceBySession()` với session ID hợp lệ
- `AttendanceService.getAttendanceBySession()` với session ID không tồn tại
- Tính toán statistics chính xác

#### Integration Tests:

- GET `/teacher/sessions/{id}/attendance` với teacher đúng
- GET `/teacher/sessions/{id}/attendance` với teacher sai (unauthorized)
- GET `/teacher/sessions/{id}/attendance` với session không tồn tại

#### UI Tests:

- Hiển thị đúng số lượng sinh viên
- Hiển thị đúng trạng thái điểm danh
- Màu sắc và icon hiển thị chính xác
- Responsive trên mobile/tablet

---

## 12. PERFORMANCE OPTIMIZATION

### Đã optimize:

- Sử dụng `attendanceMap` để tra cứu O(1) thay vì O(n)
- Lazy loading cho relationships không cần thiết
- Index trên foreign keys (session_id, student_id)

### Có thể cải thiện:

- Cache buổi học thường xuyên truy cập
- Pagination nếu số sinh viên lớn (>100)
- Eager fetch chỉ các fields cần thiết
- Database connection pooling

---

## SUMMARY

Đây là một luồng hoàn chỉnh để xem danh sách điểm danh buổi học, bao gồm:

- **Controller** xử lý request và chuẩn bị dữ liệu
- **Service** xử lý business logic
- **Repository** truy vấn database
- **Entity** mapping với database tables
- **View** hiển thị dữ liệu với Thymeleaf

Luồng này tuân thủ kiến trúc MVC chuẩn, tách biệt concerns, dễ maintain và mở rộng.
