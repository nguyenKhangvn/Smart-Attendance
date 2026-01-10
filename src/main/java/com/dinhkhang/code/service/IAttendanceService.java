package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.dto.AttendanceResponse;
import com.dinhkhang.code.dto.CheckInRequest;
import com.dinhkhang.code.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IAttendanceService {
    AttendanceResponse checkIn(CheckInRequest request, String username);

    List<AttendanceRecord> getAttendanceBySession(Long sessionId);

    List<AttendanceRecord> getStudentAttendance(Long studentId, Long classId);

    Page<AttendanceRecord> getStudentAttendancePage(Long studentId, Long classId, Pageable pageable);

    // ===== CẢI TIẾN: THÊM METHOD ĐỂ DUYỆT ATTENDANCE =====
    Optional<AttendanceRecord> getAttendanceRecordById(Long recordId);

    void updateAttendanceRecord(AttendanceRecord record);
}
