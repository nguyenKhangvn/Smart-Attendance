package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceCheckInRequest;
import com.dinhkhang.code.dto.AttendanceCheckInResponse;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.service.SecureAttendanceService;
import com.dinhkhang.code.service.IUserService;
import com.dinhkhang.code.service.FaceRecognitionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API CONTROLLER ĐIỂM DANH MỚI
 * Endpoint: POST /api/v2/attendance/check-in
 */
@RestController
@RequestMapping("/api/v2/attendance")
@CrossOrigin(origins = "*") // Cho phép mobile app gọi
public class SecureAttendanceApiController {

    @Autowired
    private SecureAttendanceService secureAttendanceService;

    @Autowired
    private IUserService userService;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    // ===== FIX: Dùng Cache tự động xóa sau 5 giây - TRÁNH MEMORY LEAK =====
    private final Cache<String, Long> requestRateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS) // Tự động xóa key sau 5s
            .maximumSize(10000) // Giới hạn tối đa 10k user để tránh tràn RAM
            .build();

    @Value("${rate.limit.check.in.interval:3000}")
    private long rateLimitInterval;

    /**
     * ĐIỂM DANH QUA QR - BẢO MẬT CAO
     *
     * Request body:
     * {
     * "sessionId": 123,
     * "token": "abc-xyz",
     * "email": "student@gmail.com",
     * "latitude": 10.762622,
     * "longitude": 106.660172,
     * "deviceId": "device-fingerprint-123",
     * "selfieBase64": "data:image/png;base64,..."
     * }
     */
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceCheckInResponse> checkIn(
            @Valid @RequestBody AttendanceCheckInRequest request, // Thêm @Valid để validate request
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // 1. Validate Email & Authentication
        if (authentication != null) {
            // Sinh viên đăng nhập bằng username, cần lấy email từ database
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user != null && user.getEmail() != null) {
                request.setEmail(user.getEmail());
            }
        }

        // Chặn lỗi NullPointerException nếu email null
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    AttendanceCheckInResponse.failed("Email is required", "INVALID_REQUEST"));
        }

        // 2. RATE LIMITING (Dùng Cache - FIX Race Condition)
        String userKey = request.getEmail();
        // Kiểm tra xem key có tồn tại trong cache không (atomic operation)
        boolean isSpam = requestRateLimitCache.asMap().containsKey(userKey);

        if (isSpam) {
            return ResponseEntity.status(429).body(
                    AttendanceCheckInResponse.failed(
                            "Thao tác quá nhanh. Vui lòng chờ vài giây.",
                            "RATE_LIMIT_EXCEEDED"));
        }

        // Lưu vào cache (tự động xóa sau 5s)
        requestRateLimitCache.put(userKey, System.currentTimeMillis());

        // 3. Lấy IP
        String clientIp = getClientIpAddress(httpRequest);
        request.setClientIp(clientIp);
        System.out.println("📍 Client IP: " + clientIp);

        // 4. Call Service
        AttendanceCheckInResponse response = secureAttendanceService.checkIn(request);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /**
     * Lấy IP thực của client (xử lý cả trường hợp qua Proxy/Nginx)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 1. Xử lý IPv6 Localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // 2. Xử lý nếu có chuỗi nhiều IP (lấy cái đầu tiên)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 3. Đảm bảo không trả về null (tránh lỗi NullPointerException ở tầng Service)
        return (ip == null || ip.isEmpty()) ? "unknown" : ip;
    }

    /**
     * TEST ENDPOINT - Kiểm tra API hoạt động
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(" Secure Attendance API is running!");
    }

    /**
     * DEBUG ENDPOINT - Phân tích ảnh Base64
     * Dùng để debug vấn đề ảnh đen
     */
    @PostMapping("/debug-image")
    public ResponseEntity<String> debugImage(@RequestBody Map<String, String> payload) {
        try {
            String base64Image = payload.get("base64Image");
            if (base64Image == null || base64Image.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Missing base64Image parameter");
            }

            // Phân tích ảnh
            String analysis = faceRecognitionService.analyzeBase64Image(base64Image);
            boolean isValid = faceRecognitionService.validateBase64Image(base64Image);

            String result = String.format("Valid: %s\nAnalysis: %s", isValid, analysis);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
