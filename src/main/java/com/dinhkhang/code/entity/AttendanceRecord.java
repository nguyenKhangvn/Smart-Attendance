package com.dinhkhang.code.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal studentLatitude;

    @Column(name = "student_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal studentLongitude;

    @Column(name = "distance_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "device_uid", nullable = false, length = 200)
    private String deviceUid; // Device fingerprint

    @Column(name = "face_data_url", columnDefinition = "TEXT")
    private String faceDataUrl; // URL ảnh selfie (có thể lưu base64 hoặc path)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "fail_reason", length = 500)
    private String failReason; // Lý do thất bại nếu có

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "is_manual", nullable = false)
    private Boolean isManual = false; // Điểm danh thủ công bởi giáo viên

    @Column(name = "modified_by")
    private Long modifiedBy; // ID của giáo viên sửa (nếu có)

    @Column(name = "modification_note", length = 500)
    private String modificationNote;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private ClassSession classSession;

    @PrePersist
    protected void onCreate() {
        if (checkedInAt == null) {
            checkedInAt = LocalDateTime.now();
        }
    }

    // Constructors
    public AttendanceRecord() {
    }

    public AttendanceRecord(
            Long id,
            BigDecimal studentLatitude,
            BigDecimal studentLongitude,
            BigDecimal distanceMeters,
            String deviceUid,
            String faceDataUrl,
            AttendanceStatus status,
            String failReason,
            LocalDateTime checkedInAt,
            Boolean isManual,
            Long modifiedBy,
            String modificationNote,
            User student,
            ClassSession classSession) {
        this.id = id;
        this.studentLatitude = studentLatitude;
        this.studentLongitude = studentLongitude;
        this.distanceMeters = distanceMeters;
        this.deviceUid = deviceUid;
        this.faceDataUrl = faceDataUrl;
        this.status = status;
        this.failReason = failReason;
        this.checkedInAt = checkedInAt;
        this.isManual = isManual;
        this.modifiedBy = modifiedBy;
        this.modificationNote = modificationNote;
        this.student = student;
        this.classSession = classSession;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(BigDecimal distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getDeviceUid() {
        return deviceUid;
    }

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public String getFaceDataUrl() {
        return faceDataUrl;
    }

    public void setFaceDataUrl(String faceDataUrl) {
        this.faceDataUrl = faceDataUrl;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Boolean getIsManual() {
        return isManual;
    }

    public void setIsManual(Boolean isManual) {
        this.isManual = isManual;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getModificationNote() {
        return modificationNote;
    }

    public void setModificationNote(String modificationNote) {
        this.modificationNote = modificationNote;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public ClassSession getClassSession() {
        return classSession;
    }

    public void setClassSession(ClassSession classSession) {
        this.classSession = classSession;
    }

    public enum AttendanceStatus {
        SUCCESS, // Điểm danh thành công
        PENDING_REVIEW, // Chờ giáo viên xác nhận (GPS sai số lớn hoặc vấn đề khác)
        FAILED_INVALID_QR, // QR không hợp lệ hoặc hết hạn
        FAILED_DISTANCE, // Ngoài phạm vi cho phép
        FAILED_DUPLICATE_DEVICE, // Thiết bị đã được sử dụng
        FAILED_ALREADY_CHECKED, // Đã điểm danh rồi
        FAILED_FACE_VERIFICATION, // Xác thực khuôn mặt thất bại
        FAILED_IMAGE_UPLOAD, // Lỗi lưu ảnh minh chứng
        GPS_POOR_SIGNAL, // GPS không ổn định (indoor) - Chờ xác nhận
        ABSENT, // Vắng
        LATE // Đi muộn
    }
}
