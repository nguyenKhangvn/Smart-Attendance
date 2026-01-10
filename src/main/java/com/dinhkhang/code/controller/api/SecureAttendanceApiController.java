package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceCheckInRequest;
import com.dinhkhang.code.dto.AttendanceCheckInResponse;
import com.dinhkhang.code.service.SecureAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // ===== CẢI TIẾN: RATE LIMITING - CHỐNG SPAM REQUEST =====
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

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
            @RequestBody AttendanceCheckInRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // Auto-fill email từ user đăng nhập
        if (authentication != null) {
            request.setEmail(authentication.getName());
        }

        // ===== CẢI TIẾN: RATE LIMITING - CHỐNG SPAM =====
        long now = System.currentTimeMillis();
        String userKey = request.getEmail();
        Long lastTime = lastRequestTime.get(userKey);

        if (lastTime != null && (now - lastTime) < rateLimitInterval) {
            AttendanceCheckInResponse response = AttendanceCheckInResponse.failed(
                    "Thao tác quá nhanh. Vui lòng chờ " + (rateLimitInterval / 1000) + " giây.",
                    "RATE_LIMIT_EXCEEDED");
            return ResponseEntity.status(429).body(response);
        }

        lastRequestTime.put(userKey, now);

        // ===== CẢI TIẾN: LẤY PUBLIC IP TỪ REQUEST =====
        String clientIp = getClientIpAddress(httpRequest);
        request.setClientIp(clientIp);
        System.out.println("📍 Client IP: " + clientIp);

        AttendanceCheckInResponse response = secureAttendanceService.checkIn(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
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

        // Nếu có nhiều IP (qua nhiều proxy), lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * TEST ENDPOINT - Kiểm tra API hoạt động
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(" Secure Attendance API is running!");
    }
}
