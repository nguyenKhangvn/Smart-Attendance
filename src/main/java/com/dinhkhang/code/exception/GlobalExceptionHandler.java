package com.dinhkhang.code.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Centralized error handling for better maintainability
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QRExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleQRExpired(QRExpiredException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateAttendanceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateAttendance(DuplicateAttendanceException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DistanceExceededException.class)
    public ResponseEntity<Map<String, Object>> handleDistanceExceeded(DistanceExceededException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("distance", e.getDistance());
        response.put("maxDistance", e.getMaxDistance());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException e) {
        return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        logger.error("Unhandled exception occurred", e);
        return createErrorResponse("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }
}
