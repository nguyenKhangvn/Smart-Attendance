package com.dinhkhang.code.dto;

public class ClassSessionDTO {
    private Long id;
    private ClassEntityDTO classEntity;

    public ClassSessionDTO() {
    }

    public ClassSessionDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassEntityDTO getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntityDTO classEntity) {
        this.classEntity = classEntity;
    }
}