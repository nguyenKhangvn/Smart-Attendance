package com.dinhkhang.code.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_sessions")
public class QRSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_secret", unique = true, nullable = false, length = 100)
    private String tokenSecret; // Mã bí mật để xác thực

    @Column(name = "teacher_latitude", nullable = false)
    private Double teacherLatitude;

    @Column(name = "teacher_longitude", nullable = false)
    private Double teacherLongitude;

    @Column(name = "max_distance_meters", nullable = false)
    private Integer maxDistanceMeters = 50; // Khoảng cách tối đa cho phép (mặc định 50m)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession classSession;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public QRSession() {
    }

    public QRSession(Long id, String tokenSecret, Double teacherLatitude, Double teacherLongitude,
            Integer maxDistanceMeters, LocalDateTime createdAt, LocalDateTime expiredAt,
            Boolean isActive, ClassSession classSession) {
        this.id = id;
        this.tokenSecret = tokenSecret;
        this.teacherLatitude = teacherLatitude;
        this.teacherLongitude = teacherLongitude;
        this.maxDistanceMeters = maxDistanceMeters;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.isActive = isActive;
        this.classSession = classSession;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Double getTeacherLatitude() {
        return teacherLatitude;
    }

    public void setTeacherLatitude(Double teacherLatitude) {
        this.teacherLatitude = teacherLatitude;
    }

    public Double getTeacherLongitude() {
        return teacherLongitude;
    }

    public void setTeacherLongitude(Double teacherLongitude) {
        this.teacherLongitude = teacherLongitude;
    }

    public Integer getMaxDistanceMeters() {
        return maxDistanceMeters;
    }

    public void setMaxDistanceMeters(Integer maxDistanceMeters) {
        this.maxDistanceMeters = maxDistanceMeters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public ClassSession getClassSession() {
        return classSession;
    }

    public void setClassSession(ClassSession classSession) {
        this.classSession = classSession;
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }
}
