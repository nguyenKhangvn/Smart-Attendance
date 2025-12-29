package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.dto.AttendanceResponse;
import com.dinhkhang.code.dto.CheckInRequest;
import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.mapper.AttendanceMapper;
import com.dinhkhang.code.service.IAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceApiController {

    @Autowired
    private IAttendanceService attendanceService;
    
    @Autowired
    private AttendanceMapper attendanceMapper;

    @PostMapping("/checkin")
    public ResponseEntity<AttendanceResponse> checkIn(
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication) {

        AttendanceResponse response = attendanceService.checkIn(request, authentication.getName());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get attendance records for a specific session
     * API vẫn trả về DTO để tránh expose toàn bộ Entity ra ngoài
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AttendanceRecordDTO>> getSessionAttendance(@PathVariable Long sessionId) {
        List<AttendanceRecord> records = attendanceService.getAttendanceBySession(sessionId);
        List<AttendanceRecordDTO> dtos = attendanceMapper.toDTOList(records);
        return ResponseEntity.ok(dtos);
    }
}
