package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.ClassEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IClassService {
    ClassEntity createClass(ClassEntity classEntity, Long teacherId);

    ClassEntity updateClass(Long id, ClassEntity updatedClass);

    ClassEntity updateClass(Long id, ClassEntity updatedClass, Long teacherId);

    void addStudentToClass(Long classId, Long studentId);

    void removeStudentFromClass(Long classId, Long studentId);

    Optional<ClassEntity> findById(Long id);

    List<ClassEntity> getClassesByTeacher(Long teacherId);

    List<ClassEntity> getClassesByStudent(Long studentId);

    void deleteClass(Long id);

    void deleteClass(Long id, Long teacherId);

    List<ClassEntity> getAllClasses();

    Page<ClassEntity> getAllClasses(Pageable pageable);

    ClassEntity getClassDetail(Long id);

    void activateClass(Long id);

    void deactivateClass(Long id);

    Optional<ClassEntity> findByIdWithTeacher(Long id);

    Page<ClassEntity> findAllWithTeacherOnly(Pageable pageable);

    long countStudentsInClass(Long classId);
}
