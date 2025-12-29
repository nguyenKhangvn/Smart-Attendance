package com.dinhkhang.code.dto;

import java.time.LocalDateTime;

public class AttendanceRecordDTO {
    private Long id;
    private LocalDateTime checkedInAt;
    private String status;
    private Double distanceMeters;
    private String failReason;
    private Double studentLatitude;
    private Double studentLongitude;
    private String deviceUid;
    
    // Nested DTOs
    private StudentDTO student;

    // Constructors
    public AttendanceRecordDTO() {
    }

    public AttendanceRecordDTO(Long id, LocalDateTime checkedInAt, String status,
                              Double distanceMeters, String failReason,
                              Double studentLatitude, Double studentLongitude,
                              String deviceUid, StudentDTO student) {
        this.id = id;
        this.checkedInAt = checkedInAt;
        this.status = status;
        this.distanceMeters = distanceMeters;
        this.failReason = failReason;
        this.studentLatitude = studentLatitude;
        this.studentLongitude = studentLongitude;
        this.deviceUid = deviceUid;
        this.student = student;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Double getStudentLatitude() {
        return studentLatitude;
    }

    public void setStudentLatitude(Double studentLatitude) {
        this.studentLatitude = studentLatitude;
    }

    public Double getStudentLongitude() {
        return studentLongitude;
    }

    public void setStudentLongitude(Double studentLongitude) {
        this.studentLongitude = studentLongitude;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public StudentDTO getStudent() {
        return student;
    }

    public void setStudent(StudentDTO student) {
        this.student = student;
    }
}
