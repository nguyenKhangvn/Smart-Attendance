package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.AttendanceResponse;
import com.dinhkhang.code.dto.CheckInRequest;
import com.dinhkhang.code.entity.AttendanceRecord;
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

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AttendanceRecord>> getSessionAttendance(@PathVariable Long sessionId) {
        List<AttendanceRecord> records = attendanceService.getAttendanceBySession(sessionId);
        return ResponseEntity.ok(records);
    }
}
