package com.dinhkhang.code.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", unique = true, nullable = false, length = 50)
    private String classCode;

    @Column(name = "subject_name", nullable = false, length = 200)
    private String subjectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String semester; // Ví dụ: "HK1 2024-2025"

    @Column(name = "room_number", length = 50)
    private String roomNumber; // Phòng học

    @Column(name = "schedule_info", length = 500)
    private String scheduleInfo; // Ví dụ: "Thứ 2, 7-9 tiết"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToMany
    @JoinTable(name = "class_students", joinColumns = @JoinColumn(name = "class_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
    private Set<User> students = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
    private Set<ClassSession> sessions = new HashSet<>();

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
    public ClassEntity() {
    }

    public ClassEntity(Long id, String classCode, String subjectName, String description, String semester,
            String roomNumber, String scheduleInfo, LocalDateTime createdAt, LocalDateTime updatedAt, Boolean isActive,
            User teacher, Set<User> students, Set<ClassSession> sessions) {
        this.id = id;
        this.classCode = classCode;
        this.subjectName = subjectName;
        this.description = description;
        this.semester = semester;
        this.roomNumber = roomNumber;
        this.scheduleInfo = scheduleInfo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.teacher = teacher;
        this.students = students;
        this.sessions = sessions;
    }

    // Getters and Setters
    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getScheduleInfo() {
        return scheduleInfo;
    }

    public void setScheduleInfo(String scheduleInfo) {
        this.scheduleInfo = scheduleInfo;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public User getTeacher() {
        return teacher;
    }

    public void setTeacher(User teacher) {
        this.teacher = teacher;
    }

    public Set<User> getStudents() {
        return students;
    }

    public void setStudents(Set<User> students) {
        this.students = students;
    }

    public Set<ClassSession> getSessions() {
        return sessions;
    }

    public void setSessions(Set<ClassSession> sessions) {
        this.sessions = sessions;
    }
}
