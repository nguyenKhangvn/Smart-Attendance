package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.dto.AttendanceResponse;
import com.dinhkhang.code.dto.CheckInRequest;
import com.dinhkhang.code.entity.AttendanceRecord;

import java.util.List;

public interface IAttendanceService {
    AttendanceResponse checkIn(CheckInRequest request, String username);

    List<AttendanceRecord> getAttendanceBySession(Long sessionId);

    List<AttendanceRecord> getStudentAttendance(Long studentId, Long classId);
}
