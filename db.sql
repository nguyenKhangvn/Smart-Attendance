
USE defaultdb;

-- 2. Xóa sạch các bảng cũ (Tắt kiểm tra khóa ngoại để xóa không lỗi)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS attendance_records;
DROP TABLE IF EXISTS qr_sessions;
DROP TABLE IF EXISTS class_sessions;
DROP TABLE IF EXISTS class_students;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ====================================
-- 3. TẠO BẢNG (CREATE TABLES)
-- ====================================

-- TABLE: users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    
    avatar_url TEXT COMMENT 'URL ảnh đại diện',
    date_of_birth DATE COMMENT 'Ngày sinh (dùng cho sinh viên)',
    
    -- Role ENUM (Khớp với Code Java)
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    
    student_code VARCHAR(20) UNIQUE COMMENT 'Mã sinh viên (chỉ cho STUDENT)',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Soft delete: TRUE = đã xóa',
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_student_code (student_code),
    INDEX idx_role (role),
    INDEX idx_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE: classes
CREATE TABLE classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_code VARCHAR(50) UNIQUE NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    description TEXT,
    semester VARCHAR(50) NOT NULL,
    room_number VARCHAR(50) COMMENT 'Phòng học',
    schedule_info VARCHAR(500) COMMENT 'Thông tin lịch học',
    teacher_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_class_code (class_code),
    INDEX idx_teacher (teacher_id),
    INDEX idx_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE: class_students
CREATE TABLE class_students (
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE: class_sessions
CREATE TABLE class_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    session_name VARCHAR(200) NOT NULL,
    session_date DATETIME NOT NULL,
    duration_minutes INT DEFAULT 90,
    notes TEXT,
    
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    INDEX idx_class (class_id),
    INDEX idx_session_date (session_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE: qr_sessions
CREATE TABLE qr_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    token_secret VARCHAR(100) UNIQUE NOT NULL,
    
    teacher_latitude DECIMAL(10, 8) NOT NULL,
    teacher_longitude DECIMAL(11, 8) NOT NULL,
    max_distance_meters INT NOT NULL DEFAULT 50,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    INDEX idx_token (token_secret),
    INDEX idx_session (session_id),
    INDEX idx_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE: attendance_records
CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    
    student_latitude DECIMAL(10, 8) NOT NULL,
    student_longitude DECIMAL(11, 8) NOT NULL,
    distance_meters DECIMAL(10, 2) NOT NULL,
    
    device_uid VARCHAR(200) NOT NULL,
    face_data_url TEXT,
    
    status ENUM('SUCCESS', 'FAILED_INVALID_QR', 'FAILED_DISTANCE', 'FAILED_DUPLICATE_DEVICE', 
                'FAILED_ALREADY_CHECKED', 'ABSENT', 'LATE') NOT NULL DEFAULT 'ABSENT',
    
    fail_reason VARCHAR(500),
    checked_in_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_manual BOOLEAN NOT NULL DEFAULT FALSE,
    modified_by BIGINT,
    modification_note VARCHAR(500),
    
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE RESTRICT,
    
    INDEX idx_student (student_id),
    INDEX idx_session (session_id),
    INDEX idx_status (status),
    
    UNIQUE KEY unique_attendance (student_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================
-- 4. INSERT DỮ LIỆU MẪU (Mật khẩu: 123456)
-- ====================================

INSERT INTO users (username, password, full_name, email, role, is_active) VALUES
('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Quản Trị Viên Hệ Thống', 'admin@smartattendance.com', 'ADMIN', TRUE);

INSERT INTO users (username, password, full_name, email, phone_number, role, is_active) VALUES
('teacher1', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Nguyễn Văn Giảng', 'teacher1@university.edu', '0901234567', 'TEACHER', TRUE),
('teacher2', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Trần Thị Hoa', 'teacher2@university.edu', '0902345678', 'TEACHER', TRUE);

INSERT INTO users (username, password, full_name, email, student_code, date_of_birth, role, is_active) VALUES
('student1', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Lê Văn An', 'student1@student.edu', '20211001', '2003-05-15', 'STUDENT', TRUE),
('student2', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Phạm Thị Bình', 'student2@student.edu', '20211002', '2003-08-20', 'STUDENT', TRUE),
('student3', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Hoàng Văn Cường', 'student3@student.edu', '20211003', '2003-02-10', 'STUDENT', TRUE),
('student4', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Nguyễn Thị Dung', 'student4@student.edu', '20211004', '2003-11-25', 'STUDENT', TRUE),
('student5', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQiy38J', 'Vũ Văn Em', 'student5@student.edu', '20211005', '2003-07-08', 'STUDENT', TRUE);

INSERT INTO classes (class_code, subject_name, description, semester, room_number, schedule_info, teacher_id, is_active) VALUES
('CS101', 'Lập trình hướng đối tượng', 'Môn học về OOP với Java', 'HK1 2024-2025', 'A101', 'Thứ 2, 7-9 tiết', 2, TRUE),
('CS102', 'Cơ sở dữ liệu', 'Thiết kế và quản trị CSDL', 'HK1 2024-2025', 'B201', 'Thứ 4, 1-3 tiết', 2, TRUE),
('CS103', 'Lập trình Web', 'Phát triển ứng dụng Web với Spring', 'HK1 2024-2025', 'C301', 'Thứ 6, 3-5 tiết', 3, TRUE);

INSERT INTO class_students (class_id, student_id) VALUES
(1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(2, 4), (2, 5), (2, 6),
(3, 5), (3, 6), (3, 7), (3, 8);

INSERT INTO class_sessions (class_id, session_name, session_date, duration_minutes, status) VALUES
(1, 'Buổi 1: Giới thiệu về OOP', '2025-01-06 07:00:00', 90, 'SCHEDULED'),
(1, 'Buổi 2: Kế thừa và Đa hình', '2025-01-13 07:00:00', 90, 'SCHEDULED'),
(1, 'Buổi 3: Abstract và Interface', '2025-01-20 07:00:00', 90, 'SCHEDULED'),
(2, 'Buổi 1: Giới thiệu CSDL', '2025-01-08 07:00:00', 90, 'SCHEDULED'),
(2, 'Buổi 2: SQL cơ bản', '2025-01-15 07:00:00', 90, 'SCHEDULED'),
(3, 'Buổi 1: Giới thiệu Spring MVC', '2025-01-10 13:00:00', 90, 'SCHEDULED'),
(3, 'Buổi 2: Spring Security', '2025-01-17 13:00:00', 90, 'SCHEDULED');

-- ====================================
-- KIỂM TRA KẾT QUẢ
-- ====================================
SELECT 'Database defaultdb updated successfully!' as Message;
SELECT COUNT(*) as 'Total Users' FROM users;
SELECT COUNT(*) as 'Total Classes' FROM classes;