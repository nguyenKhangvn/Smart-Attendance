package com.dinhkhang.code.dto;

public class AttendanceResponse {

    private boolean success;
    private String message;
    private String status;
    private Double distance;
    private Long recordId;

    // Constructors
    public AttendanceResponse() {
    }

    public AttendanceResponse(boolean success, String message, String status, Double distance, Long recordId) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.distance = distance;
        this.recordId = recordId;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public static AttendanceResponse success(String message, Double distance, Long recordId) {
        return new AttendanceResponse(true, message, "SUCCESS", distance, recordId);
    }

    public static AttendanceResponse failed(String message, String status) {
        return new AttendanceResponse(false, message, status, null, null);
    }
}
