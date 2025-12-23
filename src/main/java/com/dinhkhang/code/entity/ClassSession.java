package com.dinhkhang.code.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "class_sessions")
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_name", nullable = false, length = 200)
    private String sessionName; // Ví dụ: "Buổi 1: Giới thiệu môn học"

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Thời lượng buổi học

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @OneToMany(mappedBy = "classSession", cascade = CascadeType.ALL)
    private Set<QRSession> qrSessions = new HashSet<>();

    @OneToMany(mappedBy = "classSession", cascade = CascadeType.ALL)
    private Set<AttendanceRecord> attendanceRecords = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public ClassSession() {
    }

    public ClassSession(Long id, String sessionName, LocalDateTime sessionDate, Integer durationMinutes,
            String notes, SessionStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
            ClassEntity classEntity, Set<QRSession> qrSessions, Set<AttendanceRecord> attendanceRecords) {
        this.id = id;
        this.sessionName = sessionName;
        this.sessionDate = sessionDate;
        this.durationMinutes = durationMinutes;
        this.notes = notes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.classEntity = classEntity;
        this.qrSessions = qrSessions;
        this.attendanceRecords = attendanceRecords;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public Set<QRSession> getQrSessions() {
        return qrSessions;
    }

    public void setQrSessions(Set<QRSession> qrSessions) {
        this.qrSessions = qrSessions;
    }

    public Set<AttendanceRecord> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(Set<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public enum SessionStatus {
        SCHEDULED, // Đã lên lịch
        IN_PROGRESS, // Đang diễn ra
        COMPLETED, // Đã kết thúc
        CANCELLED // Đã hủy
    }
}
