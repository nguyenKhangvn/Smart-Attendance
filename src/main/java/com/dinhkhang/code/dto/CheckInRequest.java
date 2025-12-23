package com.dinhkhang.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CheckInRequest {

    @NotNull(message = "QR ID is required")
    private Long qrId;

    @NotBlank(message = "Token secret is required")
    private String tokenSecret;

    @NotNull(message = "Student latitude is required")
    private Double studentLat;

    @NotNull(message = "Student longitude is required")
    private Double studentLong;

    @NotBlank(message = "Device UID is required")
    private String deviceUid;

    private String imageData; // Base64 encoded image

    // Constructors
    public CheckInRequest() {
    }

    public CheckInRequest(Long qrId, String tokenSecret, Double studentLat, Double studentLong,
            String deviceUid, String imageData) {
        this.qrId = qrId;
        this.tokenSecret = tokenSecret;
        this.studentLat = studentLat;
        this.studentLong = studentLong;
        this.deviceUid = deviceUid;
        this.imageData = imageData;
    }

    // Getters and Setters
    public Long getQrId() {
        return qrId;
    }

    public void setQrId(Long qrId) {
        this.qrId = qrId;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Double getStudentLat() {
        return studentLat;
    }

    public void setStudentLat(Double studentLat) {
        this.studentLat = studentLat;
    }

    public Double getStudentLong() {
        return studentLong;
    }

    public void setStudentLong(Double studentLong) {
        this.studentLong = studentLong;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
