package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceCheckInRequest;
import com.dinhkhang.code.dto.AttendanceCheckInResponse;
import com.dinhkhang.code.service.SecureAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API CONTROLLER ĐIỂM DANH MỚI
 * Endpoint: POST /api/v2/attendance/check-in
 */
@RestController
@RequestMapping("/api/v2/attendance")
@CrossOrigin(origins = "*")  // Cho phép mobile app gọi
public class SecureAttendanceApiController {

    @Autowired
    private SecureAttendanceService secureAttendanceService;

    /**
     * ĐIỂM DANH QUA QR - BẢO MẬT CAO
     *
     * Request body:
     * {
     *   "sessionId": 123,
     *   "token": "abc-xyz",
     *   "email": "student@gmail.com",
     *   "latitude": 10.762622,
     *   "longitude": 106.660172,
     *   "deviceId": "device-fingerprint-123",
     *   "selfieBase64": "data:image/png;base64,..."
     * }
     */
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceCheckInResponse> checkIn(
            @RequestBody AttendanceCheckInRequest request,
            Authentication authentication) {

        // Auto-fill email từ user đăng nhập
        if (authentication != null) {
            request.setEmail(authentication.getName());
        }

        AttendanceCheckInResponse response = secureAttendanceService.checkIn(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * TEST ENDPOINT - Kiểm tra API hoạt động
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Secure Attendance API is running!");
    }
}

