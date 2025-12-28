package com.dinhkhang.code.dto;

/**
 * Response trả về sau khi điểm danh
 */
public class AttendanceCheckInResponse {
    private boolean success;
    private String message;
    private String status;  // SUCCESS, FAILED_INVALID_SESSION, FAILED_NOT_IN_CLASS, etc.
    private Double distance;  // Khoảng cách tính được (meters)
    private Long recordId;

    public AttendanceCheckInResponse() {
    }

    public AttendanceCheckInResponse(boolean success, String message, String status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    public static AttendanceCheckInResponse success(String message, Double distance, Long recordId) {
        AttendanceCheckInResponse response = new AttendanceCheckInResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setStatus("SUCCESS");
        response.setDistance(distance);
        response.setRecordId(recordId);
        return response;
    }

    public static AttendanceCheckInResponse failed(String message, String status) {
        AttendanceCheckInResponse response = new AttendanceCheckInResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatus(status);
        return response;
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
}
