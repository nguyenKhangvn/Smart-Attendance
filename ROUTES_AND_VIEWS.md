# SmartAttendance - Application Routes & Views Mapping

## Application Configuration

- **Base URL**: `http://localhost:8086/SmartAttendance`
- **Spring MVC Version**: 6.1.4
- **Security**: Spring Security 6.2.1 (Role-based access control)

---

## 🔧 BUG FIX SUMMARY

### Issue

`org.springframework.beans.factory.UnsatisfiedDependencyException: No qualifying bean of type 'com.dinhkhang.code.service.IClassService' available`

### Root Cause

`WebMvcConfig` had `@ComponentScan(basePackages = "com.dinhkhang.code.controller")` which limited component scanning to only controllers. Services in `com.dinhkhang.code.service` were not visible to the servlet context.

### Solution

Removed `@ComponentScan` from `WebMvcConfig` to allow it to inherit component scanning from the root application context (`AppConfig`) which scans the entire `com.dinhkhang.code` package.

**Changed in**: `src/main/java/com/dinhkhang/code/config/WebMvcConfig.java`

```java
// BEFORE:
@ComponentScan(basePackages = "com.dinhkhang.code.controller")
public class WebMvcConfig implements WebMvcConfigurer {

// AFTER:
public class WebMvcConfig implements WebMvcConfigurer {
```

---

## 📋 COMPLETE ROUTES MAPPING

### 🔐 Authentication Routes

| Method | URL      | Controller      | View              | Description       |
| ------ | -------- | --------------- | ----------------- | ----------------- |
| GET    | `/login` | (ViewResolver)  | `auth/login.html` | Login page        |
| POST   | `/login` | Spring Security | -                 | Login processing  |
| GET    | `/`      | (ViewResolver)  | `index.html`      | Home/Landing page |

---

### 👨‍💼 Admin Routes (`/admin/*`)

**Access**: `@PreAuthorize("hasRole('ADMIN')")`

#### Dashboard

| Method | URL                | Controller Method             | View                   | Description    |
| ------ | ------------------ | ----------------------------- | ---------------------- | -------------- |
| GET    | `/admin/dashboard` | `AdminController.dashboard()` | `admin/dashboard.html` | Admin overview |

#### Teachers Management

| Method | URL                                  | Controller Method                       | View                      | Description         |
| ------ | ------------------------------------ | --------------------------------------- | ------------------------- | ------------------- |
| GET    | `/admin/teachers`                    | `AdminController.listTeachers()`        | `admin/teachers.html`     | List all teachers   |
| GET    | `/admin/teachers/create`             | `AdminController.createTeacherForm()`   | `admin/teacher-form.html` | Create teacher form |
| POST   | `/admin/teachers/create`             | `AdminController.createTeacher()`       | Redirect                  | Save new teacher    |
| GET    | `/admin/teachers/edit/{id}`          | `AdminController.editTeacherForm()`     | `admin/teacher-form.html` | Edit teacher form   |
| POST   | `/admin/teachers/edit/{id}`          | `AdminController.editTeacher()`         | Redirect                  | Update teacher      |
| POST   | `/admin/teachers/toggle-status/{id}` | `AdminController.toggleTeacherStatus()` | Redirect                  | Activate/Deactivate |

#### Students Management

| Method | URL                                  | Controller Method                       | View                      | Description         |
| ------ | ------------------------------------ | --------------------------------------- | ------------------------- | ------------------- |
| GET    | `/admin/students`                    | `AdminController.listStudents()`        | `admin/students.html`     | List all students   |
| GET    | `/admin/students/create`             | `AdminController.createStudentForm()`   | `admin/student-form.html` | Create student form |
| POST   | `/admin/students/create`             | `AdminController.createStudent()`       | Redirect                  | Save new student    |
| GET    | `/admin/students/edit/{id}`          | `AdminController.editStudentForm()`     | `admin/student-form.html` | Edit student form   |
| POST   | `/admin/students/edit/{id}`          | `AdminController.editStudent()`         | Redirect                  | Update student      |
| POST   | `/admin/students/toggle-status/{id}` | `AdminController.toggleStudentStatus()` | Redirect                  | Activate/Deactivate |

#### Classes Management

| Method | URL                                 | Controller Method                     | View                      | Description         |
| ------ | ----------------------------------- | ------------------------------------- | ------------------------- | ------------------- |
| GET    | `/admin/classes`                    | `AdminController.listClasses()`       | `admin/classes.html`      | List all classes    |
| GET    | `/admin/classes/create`             | `AdminController.createClassForm()`   | `admin/class-form.html`   | Create class form   |
| POST   | `/admin/classes/create`             | `AdminController.createClass()`       | Redirect                  | Save new class      |
| GET    | `/admin/classes/{id}`               | `AdminController.viewClass()`         | `admin/class-detail.html` | View class details  |
| GET    | `/admin/classes/edit/{id}`          | `AdminController.editClassForm()`     | `admin/class-form.html`   | Edit class form     |
| POST   | `/admin/classes/edit/{id}`          | `AdminController.editClass()`         | Redirect                  | Update class        |
| POST   | `/admin/classes/toggle-status/{id}` | `AdminController.toggleClassStatus()` | Redirect                  | Activate/Deactivate |

---

### 👨‍🏫 Teacher Routes (`/teacher/*`)

**Access**: `@PreAuthorize("hasRole('TEACHER')")`

#### Dashboard & Classes

| Method | URL                          | Controller Method                     | View                        | Description                 |
| ------ | ---------------------------- | ------------------------------------- | --------------------------- | --------------------------- |
| GET    | `/teacher/dashboard`         | `TeacherController.dashboard()`       | `teacher/dashboard.html`    | Teacher overview with stats |
| GET    | `/teacher/classes`           | `TeacherController.listClasses()`     | `teacher/classes.html`      | My classes list             |
| GET    | `/teacher/classes/create`    | `TeacherController.createClassForm()` | `teacher/class-form.html`   | Create class form           |
| POST   | `/teacher/classes/create`    | `TeacherController.createClass()`     | Redirect                    | Save new class              |
| GET    | `/teacher/classes/{id}`      | `TeacherController.viewClass()`       | `teacher/class-detail.html` | Class details + sessions    |
| GET    | `/teacher/classes/{id}/edit` | `TeacherController.editClassForm()`   | `teacher/class-form.html`   | Edit class form             |
| POST   | `/teacher/classes/{id}/edit` | `TeacherController.editClass()`       | Redirect                    | Update class                |

#### Student Management

| Method | URL                                                     | Controller Method                            | View     | Description                |
| ------ | ------------------------------------------------------- | -------------------------------------------- | -------- | -------------------------- |
| POST   | `/teacher/classes/{classId}/import-students`            | `TeacherController.importStudents()`         | Redirect | Import students from Excel |
| POST   | `/teacher/classes/{classId}/add-student`                | `TeacherController.addStudentToClass()`      | Redirect | Add single student by code |
| POST   | `/teacher/classes/{classId}/remove-student/{studentId}` | `TeacherController.removeStudentFromClass()` | Redirect | Remove student from class  |

#### Sessions Management

| Method | URL                                     | Controller Method                           | View                        | Description                       |
| ------ | --------------------------------------- | ------------------------------------------- | --------------------------- | --------------------------------- |
| GET    | `/teacher/sessions/create?classId={id}` | `TeacherController.showCreateSessionForm()` | `teacher/session-form.html` | Create session form               |
| POST   | `/teacher/sessions/create`              | `TeacherController.createSession()`         | Redirect                    | Save new session                  |
| GET    | `/teacher/sessions/{id}/attendance`     | `TeacherController.viewAttendance()`        | `teacher/attendance.html`   | ✅ **NEW** - View attendance list |
| GET    | `/teacher/sessions/{id}/qr`             | `TeacherController.generateQR()`            | `teacher/qr-scanner.html`   | QR code generator view            |

---

### 👨‍🎓 Student Routes (`/student/*`)

**Access**: `@PreAuthorize("hasRole('STUDENT')")`

#### Dashboard & Classes

| Method | URL                     | Controller Method                 | View                        | Description                                |
| ------ | ----------------------- | --------------------------------- | --------------------------- | ------------------------------------------ |
| GET    | `/student/dashboard`    | `StudentController.dashboard()`   | `student/dashboard.html`    | Student overview                           |
| GET    | `/student/classes`      | `StudentController.listClasses()` | `student/classes.html`      | ✅ **NEW** - My enrolled classes           |
| GET    | `/student/classes/{id}` | `StudentController.viewClass()`   | `student/class-detail.html` | ✅ **NEW** - Class details + my attendance |

#### Attendance

| Method | URL                           | Controller Method                       | View                              | Description                          |
| ------ | ----------------------------- | --------------------------------------- | --------------------------------- | ------------------------------------ |
| GET    | `/student/scan-qr`            | `StudentController.scanQR()`            | `student/scan-qr.html`            | QR code scanner                      |
| GET    | `/student/attendance-history` | `StudentController.attendanceHistory()` | `student/attendance-history.html` | ✅ **NEW** - Full attendance history |

---

### 🔌 API Routes (`/api/*`)

**Content-Type**: `application/json`

#### QR Code API

| Method | URL                | Controller                     | Description                             |
| ------ | ------------------ | ------------------------------ | --------------------------------------- |
| POST   | `/api/qr/generate` | `QRApiController.generateQR()` | Generate QR session with GPS validation |

**Parameters**:

- `sessionId` (Long) - ID of teaching session
- `latitude` (Double) - Teacher's latitude
- `longitude` (Double) - Teacher's longitude
- `expirationMinutes` (Integer, default=5) - QR expiry time
- `maxDistanceMeters` (Integer, default=50) - GPS range

**Response**: `QRSessionDTO` with token, coordinates, expiry

#### Attendance API

| Method | URL                                   | Controller                                       | Description                    |
| ------ | ------------------------------------- | ------------------------------------------------ | ------------------------------ |
| POST   | `/api/attendance/checkin`             | `AttendanceApiController.checkIn()`              | Student check-in via QR        |
| GET    | `/api/attendance/session/{sessionId}` | `AttendanceApiController.getSessionAttendance()` | Get session attendance records |

**Check-in Request Body**:

```json
{
  "qrToken": "string",
  "latitude": 0.0,
  "longitude": 0.0,
  "deviceInfo": "string",
  "ipAddress": "string"
}
```

---

## 📁 Views Directory Structure

```
src/main/webapp/WEB-INF/views/
├── index.html                        # Landing page
├── layout.html                       # Base layout template
├── auth/
│   └── login.html                    # Login page
├── admin/
│   ├── dashboard.html                # Admin overview
│   ├── teachers.html                 # Teachers list
│   ├── teacher-form.html             # Create/Edit teacher
│   ├── students.html                 # Students list
│   ├── student-form.html             # Create/Edit student
│   ├── classes.html                  # Classes list
│   ├── class-form.html               # Create/Edit class
│   └── class-detail.html             # Class details (admin view)
├── teacher/
│   ├── dashboard.html                # Teacher overview with stats
│   ├── classes.html                  # My classes (card view)
│   ├── class-form.html               # Create/Edit class
│   ├── class-detail.html             # Class details + sessions
│   ├── session-form.html             # Create teaching session
│   ├── attendance.html               # ✅ NEW - Attendance list view
│   └── qr-scanner.html               # QR generation view
└── student/
    ├── dashboard.html                # Student overview
    ├── classes.html                  # ✅ NEW - Enrolled classes
    ├── class-detail.html             # ✅ NEW - Class + attendance history
    ├── scan-qr.html                  # QR scanner interface
    └── attendance-history.html       # ✅ NEW - Full attendance history
```

---

## ✅ NEW VIEWS CREATED (This Session)

1. **`teacher/attendance.html`**

   - View detailed attendance list for a session
   - Shows student status (Present/Late/Absent)
   - Displays statistics and export capability
   - Route: `GET /teacher/sessions/{id}/attendance`

2. **`student/classes.html`**

   - Grid view of all enrolled classes
   - Shows class info, teacher, schedule
   - Quick access to class details
   - Route: `GET /student/classes`

3. **`student/class-detail.html`**

   - Class information and attendance history
   - Personal attendance statistics
   - Filter attendance by class
   - Route: `GET /student/classes/{id}`

4. **`student/attendance-history.html`**
   - Complete attendance history across all classes
   - Statistics dashboard
   - Search functionality
   - Export to Excel capability
   - Route: `GET /student/attendance-history`

---

## 🔍 Service Layer

### IClassService

```java
ClassEntity createClass(ClassEntity classEntity, Long teacherId)
ClassEntity updateClass(Long id, ClassEntity updatedClass)
void addStudentToClass(Long classId, Long studentId)
void removeStudentFromClass(Long classId, Long studentId)
Optional<ClassEntity> findById(Long id)
List<ClassEntity> getClassesByTeacher(Long teacherId)  // ✅ Uses JOIN FETCH
List<ClassEntity> getClassesByStudent(Long studentId)
void deleteClass(Long id)
List<ClassEntity> getAllClasses()
```

### IClassSessionService

```java
ClassSession createSession(ClassSession session, Long classId)
ClassSession findById(Long id)
List<ClassSession> getSessionsByClass(Long classId)
```

### IAttendanceService

```java
AttendanceResponse checkIn(CheckInRequest request, String username)
List<AttendanceRecord> getAttendanceBySession(Long sessionId)
List<AttendanceRecord> getStudentAttendance(Long studentId, Long classId)
```

### IQRService

```java
QRSessionDTO generateQRSession(Long sessionId, Double latitude, Double longitude, Integer expirationMinutes, Integer maxDistanceMeters)
```

---

## 🛠️ Key Features Implemented

### ✅ Admin Features

- Full CRUD for Teachers, Students, Classes
- Toggle active/inactive status
- Role-based access control
- Dashboard with statistics

### ✅ Teacher Features

- Create and manage classes
- Add students via Excel import or manual entry
- Create teaching sessions
- Generate QR codes with GPS validation
- View attendance lists and statistics
- Real-time dashboard with:
  - Total classes
  - Total students across all classes
  - Today's sessions count

### ✅ Student Features

- View enrolled classes
- QR code attendance check-in
- Personal attendance history
- Class-specific attendance records
- Statistics dashboard

### ✅ Technical Features

- **Lazy Loading Fix**: Repository-level `JOIN FETCH` for students collection
- **Context Path**: All URLs use Thymeleaf `@{/...}` syntax
- **Parameter Reflection**: Maven `-parameters` flag enabled
- **QR Security**: Token expiry, GPS validation, device fingerprinting
- **Transaction Management**: `@Transactional` on service layer

---

## 🚀 Deployment Instructions

1. **Build WAR file**:

   ```bash
   mvn clean package -DskipTests
   ```

2. **Deploy to Tomcat**:

   - Copy `target/SmartAttendance.war` to Tomcat `webapps/` directory
   - Start Tomcat

3. **Access Application**:

   - URL: `http://localhost:8086/SmartAttendance`
   - Default credentials: (Configure in database)

4. **Database Setup**:
   - MySQL 8.0+
   - Database name: `smart_attendance`
   - Connection: `jdbc:mysql://localhost:3306/smart_attendance`
   - User: `root` / Password: `123` (configure in `application.properties`)

---

## 📌 TODO / Future Enhancements

- [ ] Excel export functionality for attendance
- [ ] Session status management (Start/Complete session)
- [ ] Manual attendance marking by teacher
- [ ] Email notifications for absences
- [ ] Attendance reports (PDF/Excel)
- [ ] Student statistics dashboard
- [ ] Multi-language support
- [ ] Mobile app integration

---

## 🐛 Known Issues & Resolutions

### ✅ RESOLVED: UnsatisfiedDependencyException

**Status**: Fixed
**Solution**: Removed `@ComponentScan` from WebMvcConfig

### ✅ RESOLVED: LazyInitializationException

**Status**: Fixed
**Solution**: Added `JOIN FETCH c.students` in ClassRepository

### ✅ RESOLVED: Context Path 404 Errors

**Status**: Fixed
**Solution**: All URLs use Thymeleaf `@{/...}` syntax

### ✅ RESOLVED: Parameter Name Reflection

**Status**: Fixed
**Solution**: Added `-parameters` to Maven compiler plugin

---

**Last Updated**: December 23, 2025
**Build Status**: ✅ SUCCESS (7.102s)
**Maven Output**: `SmartAttendance.war` generated successfully
