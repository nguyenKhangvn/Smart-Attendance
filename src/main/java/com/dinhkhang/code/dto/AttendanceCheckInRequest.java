package com.dinhkhang.code.dto;

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
    private String email;  // Email sinh viên đăng nhập
    private Double latitude;  // GPS hiện tại
    private Double longitude;
    private String deviceId;  // Device fingerprint
    private String selfieBase64;  // Ảnh selfie (optional)

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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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
}

