package com.dinhkhang.code.repository;

import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

        List<AttendanceRecord> findByClassSession(ClassSession classSession);

        List<AttendanceRecord> findByStudent(User student);

        Optional<AttendanceRecord> findByStudentAndClassSession(User student, ClassSession classSession);

        @Query("SELECT a FROM AttendanceRecord a WHERE a.classSession = :classSession " +
                        "AND a.deviceUid = :deviceUid")
        Optional<AttendanceRecord> findBySessionAndDeviceUid(
                        @Param("classSession") ClassSession classSession,
                        @Param("deviceUid") String deviceUid);

        @Query("SELECT DISTINCT a FROM AttendanceRecord a " +
                        "JOIN FETCH a.classSession cs " +
                        "JOIN FETCH cs.classEntity c " +
                        "WHERE a.student = :student " +
                        "AND a.classSession.classEntity.id = :classId " +
                        "ORDER BY a.checkedInAt DESC")
        List<AttendanceRecord> findByStudentAndClassId(
                        @Param("student") User student,
                        @Param("classId") Long classId);

        @Query(value = "SELECT DISTINCT a FROM AttendanceRecord a " +
                        "JOIN FETCH a.classSession cs " +
                        "JOIN FETCH cs.classEntity c " +
                        "WHERE a.student = :student " +
                        "AND (:classId IS NULL OR cs.classEntity.id = :classId)", countQuery = "SELECT COUNT(a) FROM AttendanceRecord a "
                                        +
                                        "WHERE a.student = :student " +
                                        "AND (:classId IS NULL OR a.classSession.classEntity.id = :classId)")
        Page<AttendanceRecord> findByStudentAndClassId(
                        @Param("student") User student,
                        @Param("classId") Long classId,
                        Pageable pageable);

        @Query("SELECT DISTINCT a FROM AttendanceRecord a " +
                        "JOIN FETCH a.student s " +
                        "WHERE a.classSession.id = :sessionId")
        List<AttendanceRecord> findByClassSessionIdWithDetails(@Param("sessionId") Long sessionId);

        @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.classSession = :classSession " +
                        "AND a.status = com.dinhkhang.code.entity.AttendanceRecord$AttendanceStatus.SUCCESS")
        Long countSuccessfulAttendance(@Param("classSession") ClassSession classSession);

        @Query("SELECT a FROM AttendanceRecord a WHERE a.classSession.classEntity.id = :classId " +
                        "AND a.status = :status")
        List<AttendanceRecord> findByClassIdAndStatus(
                        @Param("classId") Long classId,
                        @Param("status") AttendanceRecord.AttendanceStatus status);

        boolean existsByStudentAndClassSession(User student, ClassSession classSession);
}
