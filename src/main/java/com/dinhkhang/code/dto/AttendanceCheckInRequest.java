package com.dinhkhang.code.dto;

import java.math.BigDecimal;

/**
 * DTO cho request điểm danh từ sinh viên
 * QR Code CHỈ CHỨA: sessionId + token
 * Sinh viên gửi thêm: email, GPS, deviceId, selfie
 */
public class AttendanceCheckInRequest {

    // Từ QR Code
    private Long sessionId;
    private String token;

    // Từ sinh viên (tự động gửi)
    private String email; // Email sinh viên đăng nhập
    private BigDecimal latitude; // GPS hiện tại
    private BigDecimal longitude;
    private Double gpsAccuracy; // Độ chính xác GPS (meters) - Để phát hiện indoor GPS
    private String clientIp; // Public IP của client (lấy từ frontend hoặc header)
    private String deviceId; // Device fingerprint
    private String selfieBase64; // Ảnh selfie (optional)
    private Long timestamp; // Thời điểm quét QR thực tế (milliseconds) - cho Store-and-Forward

    public AttendanceCheckInRequest() {
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Double gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSelfieBase64() {
        return selfieBase64;
    }

    public void setSelfieBase64(String selfieBase64) {
        this.selfieBase64 = selfieBase64;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
