package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.mapper.AttendanceMapper;
import com.dinhkhang.code.service.IAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceApiController {

    @Autowired
    private IAttendanceService attendanceService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AttendanceRecordDTO>> getSessionAttendance(@PathVariable Long sessionId) {
        List<AttendanceRecord> records = attendanceService.getAttendanceBySession(sessionId);
        List<AttendanceRecordDTO> dtos = attendanceMapper.toDTOList(records);
        return ResponseEntity.ok(dtos);
    }
}
