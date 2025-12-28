package com.dinhkhang.code.dto;

import java.time.LocalDate;

public class StudentImportDTO {
    private String fullName;
    private String email;
    private LocalDate dateOfBirth;  // THÊM MỚI
    private String studentCode;
    private String username;
    private String password;
    private String phoneNumber;

    // Constructors
    public StudentImportDTO() {
    }

    public StudentImportDTO(String studentCode, String fullName, String email, String phoneNumber, String username,
            String password, LocalDate dateOfBirth) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phonePhone) {
        this.phoneNumber = phonePhone;
    }
}
