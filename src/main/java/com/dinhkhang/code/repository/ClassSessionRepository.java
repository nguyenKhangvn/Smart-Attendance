package com.dinhkhang.code.repository;

import com.dinhkhang.code.entity.ClassEntity;
import com.dinhkhang.code.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findByClassEntity(ClassEntity classEntity);

    List<ClassSession> findByClassEntityOrderBySessionDateDesc(ClassEntity classEntity);

    List<ClassSession> findByStatus(ClassSession.SessionStatus status);

    @Query("SELECT s FROM ClassSession s WHERE s.classEntity = :classEntity " +
            "AND s.sessionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.sessionDate DESC")
    List<ClassSession> findByClassAndDateRange(
            @Param("classEntity") ClassEntity classEntity,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s FROM ClassSession s WHERE s.classEntity.teacher.id = :teacherId " +
            "AND s.status = :status ORDER BY s.sessionDate DESC")
    List<ClassSession> findByTeacherIdAndStatus(
            @Param("teacherId") Long teacherId,
            @Param("status") ClassSession.SessionStatus status);

    @Query("SELECT s FROM ClassSession s " +
            "LEFT JOIN FETCH s.classEntity ce " +
            "LEFT JOIN FETCH ce.teacher " +
            "LEFT JOIN FETCH ce.students " +
            "WHERE s.id = :id")
    java.util.Optional<ClassSession> findByIdWithClassEntity(@Param("id") Long id);
}
