-- Smart Attendance Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS smart_attendance 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smart_attendance;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    student_code VARCHAR(20) UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_student_code (student_code),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Classes Table
CREATE TABLE IF NOT EXISTS classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_code VARCHAR(50) UNIQUE NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    description TEXT,
    semester VARCHAR(50) NOT NULL,
    schedule_info VARCHAR(500),
    teacher_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_class_code (class_code),
    INDEX idx_teacher (teacher_id),
    INDEX idx_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Class Students (Many-to-Many)
CREATE TABLE IF NOT EXISTS class_students (
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Class Sessions Table
CREATE TABLE IF NOT EXISTS class_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    session_name VARCHAR(200) NOT NULL,
    session_date DATETIME NOT NULL,
    duration_minutes INT,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    INDEX idx_class (class_id),
    INDEX idx_session_date (session_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- QR Sessions Table
CREATE TABLE IF NOT EXISTS qr_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    token_secret VARCHAR(100) UNIQUE NOT NULL,
    teacher_latitude DOUBLE NOT NULL,
    teacher_longitude DOUBLE NOT NULL,
    max_distance_meters INT NOT NULL DEFAULT 50,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    INDEX idx_token (token_secret),
    INDEX idx_session (session_id),
    INDEX idx_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Attendance Records Table
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    student_latitude DOUBLE NOT NULL,
    student_longitude DOUBLE NOT NULL,
    distance_meters DOUBLE NOT NULL,
    device_uid VARCHAR(200) NOT NULL,
    face_data_url TEXT,
    status VARCHAR(20) NOT NULL,
    fail_reason VARCHAR(500),
    checked_in_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_manual BOOLEAN NOT NULL DEFAULT FALSE,
    modified_by BIGINT,
    modification_note VARCHAR(500),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_session (session_id),
    INDEX idx_status (status),
    INDEX idx_device (device_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert demo data
-- Admin user
INSERT INTO users (username, password, full_name, email, role, is_active) VALUES
('admin', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Administrator', 'admin@smartattendance.com', 'ADMIN', TRUE);

-- Teacher users (password: password)
INSERT INTO users (username, password, full_name, email, phone_number, role, is_active) VALUES
('teacher', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Nguyễn Văn Giảng', 'teacher@university.edu', '0901234567', 'TEACHER', TRUE),
('teacher2', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Trần Thị Hoa', 'teacher2@university.edu', '0902345678', 'TEACHER', TRUE);

-- Student users (password: password)
INSERT INTO users (username, password, full_name, email, student_code, role, is_active) VALUES
('student', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Lê Văn An', 'student1@student.edu', '20211001', 'STUDENT', TRUE),
('student2', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Phạm Thị Bình', 'student2@student.edu', '20211002', 'STUDENT', TRUE),
('student3', '$2a$10$N5vWxZ3YdZl3Kkq9xPGgPeM8cOPh9xYZ3vQhQxKxQxKxQxKxQxKxQx', 'Hoàng Văn Cường', 'student3@student.edu', '20211003', 'STUDENT', TRUE);

-- Sample classes
INSERT INTO classes (class_code, subject_name, description, semester, schedule_info, teacher_id, is_active) VALUES
('CS101', 'Lập trình hướng đối tượng', 'Môn học về OOP với Java', 'HK1 2024-2025', 'Thứ 2, 7-9 tiết, Phòng A101', 2, TRUE),
('CS102', 'Cơ sở dữ liệu', 'Thiết kế và quản trị CSDL', 'HK1 2024-2025', 'Thứ 4, 1-3 tiết, Phòng B201', 2, TRUE);

-- Enroll students
INSERT INTO class_students (class_id, student_id) VALUES
(1, 4), (1, 5), (1, 6),
(2, 4), (2, 5);

-- Sample sessions
INSERT INTO class_sessions (class_id, session_name, session_date, duration_minutes, status) VALUES
(1, 'Buổi 1: Giới thiệu về OOP', '2024-12-23 07:00:00', 90, 'SCHEDULED'),
(1, 'Buổi 2: Kế thừa và Đa hình', '2024-12-30 07:00:00', 90, 'SCHEDULED');
