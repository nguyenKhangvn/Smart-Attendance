package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassEntity;

import java.util.List;
import java.util.Optional;

public interface IClassService {
    ClassEntity createClass(ClassEntity classEntity, Long teacherId);

    ClassEntity updateClass(Long id, ClassEntity updatedClass);

    void addStudentToClass(Long classId, Long studentId);

    void removeStudentFromClass(Long classId, Long studentId);

    Optional<ClassEntity> findById(Long id);

    List<ClassEntity> getClassesByTeacher(Long teacherId);

    List<ClassEntity> getClassesByStudent(Long studentId);

    void deleteClass(Long id);

    List<ClassEntity> getAllClasses();
}
