package com.dinhkhang.code.repository;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    Optional<ClassEntity> findByClassCode(String classCode);

    List<ClassEntity> findByTeacher(User teacher);

    @Query("SELECT DISTINCT c FROM ClassEntity c LEFT JOIN FETCH c.students WHERE c.teacher = :teacher AND c.isActive = :isActive")
    List<ClassEntity> findByTeacherAndIsActive(@Param("teacher") User teacher, @Param("isActive") Boolean isActive);

    @Query("SELECT DISTINCT c FROM ClassEntity c LEFT JOIN FETCH c.teacher WHERE :student MEMBER OF c.students")
    List<ClassEntity> findClassesByStudent(@Param("student") User student);

    @Query("SELECT DISTINCT c FROM ClassEntity c LEFT JOIN FETCH c.teacher WHERE :student MEMBER OF c.students AND c.isActive = true")
    List<ClassEntity> findActiveClassesByStudent(@Param("student") User student);

    @Query("SELECT c FROM ClassEntity c WHERE c.teacher = :teacher AND c.semester = :semester AND c.isActive = true")
    List<ClassEntity> findByTeacherAndSemester(@Param("teacher") User teacher, @Param("semester") String semester);

    boolean existsByClassCode(String classCode);
}
