package com.dinhkhang.code.exception;

/**
 * Custom exceptions for Smart Attendance System
 * Provides better error handling and specific error messages
 */

public class AttendanceException extends RuntimeException {
    public AttendanceException(String message) {
        super(message);
    }
}

