package com.dinhkhang.code.exception;

public class DuplicateDeviceException extends AttendanceException {
    public DuplicateDeviceException() {
        super("Thiết bị này đã được sử dụng để điểm danh.");
    }
}

