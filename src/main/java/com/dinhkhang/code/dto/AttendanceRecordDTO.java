package com.dinhkhang.code.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendanceRecordDTO {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime checkedInAt;

    private String status;
    private BigDecimal distanceMeters;
    private String failReason;
    private BigDecimal studentLatitude;
    private BigDecimal studentLongitude;
    private String deviceUid;

    // Nested DTOs
    private StudentDTO student;

    // Thêm trường session cho Thymeleaf truy cập
    private ClassSessionDTO session;

    public ClassSessionDTO getSession() {
        return session;
    }

    public void setSession(ClassSessionDTO session) {
        this.session = session;
    }

    // Constructors
    public AttendanceRecordDTO() {
    }

    public AttendanceRecordDTO(Long id, LocalDateTime checkedInAt, String status,
            BigDecimal distanceMeters, String failReason,
            BigDecimal studentLatitude, BigDecimal studentLongitude,
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

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(BigDecimal distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public BigDecimal getStudentLatitude() {
        return studentLatitude;
    }

    public void setStudentLatitude(BigDecimal studentLatitude) {
        this.studentLatitude = studentLatitude;
    }

    public BigDecimal getStudentLongitude() {
        return studentLongitude;
    }

    public void setStudentLongitude(BigDecimal studentLongitude) {
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
