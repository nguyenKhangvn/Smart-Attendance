package com.dinhkhang.code.exception;

public class DuplicateAttendanceException extends AttendanceException {
    public DuplicateAttendanceException() {
        super("Bạn đã điểm danh cho buổi học này rồi.");
    }
}

