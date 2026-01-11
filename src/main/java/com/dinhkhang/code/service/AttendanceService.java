package com.dinhkhang.code.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.repository.AttendanceRecordRepository;
import com.dinhkhang.code.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class AttendanceService implements IAttendanceService {

        @Autowired
        private AttendanceRecordRepository attendanceRecordRepository;

        @Autowired
        private UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public List<AttendanceRecord> getAttendanceBySession(Long sessionId) {
                return attendanceRecordRepository.findByClassSessionIdWithDetails(sessionId);
        }

        @Override
        public List<AttendanceRecord> getStudentAttendance(Long studentId, Long classId) {
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found"));
                return attendanceRecordRepository.findByStudentAndClassId(student, classId);
        }

        @Override
        public Page<AttendanceRecord> getStudentAttendancePage(Long studentId, Long classId, Pageable pageable) {
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found"));
                return attendanceRecordRepository.findByStudentAndClassId(student, classId, pageable);
        }

        @Override
        public Optional<AttendanceRecord> getAttendanceRecordById(Long recordId) {
                return attendanceRecordRepository.findById(recordId);
        }

        @Override
        public void updateAttendanceRecord(AttendanceRecord record) {
                attendanceRecordRepository.save(record);
        }
}