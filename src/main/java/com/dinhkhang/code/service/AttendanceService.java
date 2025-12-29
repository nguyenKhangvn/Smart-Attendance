package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.AttendanceRecordDTO;
import com.dinhkhang.code.dto.AttendanceResponse;
import com.dinhkhang.code.dto.CheckInRequest;
import com.dinhkhang.code.entity.AttendanceRecord;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.entity.User;
import com.dinhkhang.code.mapper.AttendanceMapper;
import com.dinhkhang.code.repository.AttendanceRecordRepository;
import com.dinhkhang.code.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AttendanceService implements IAttendanceService {

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IQRService qrService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    public AttendanceResponse checkIn(CheckInRequest request, String username) {
        try {
            // 1. Validate QR session
            QRSession qrSession = qrService.validateQRSessionBySessionIdAndToken(
                    request.getSessionId(),
                    request.getTokenSecret()
            );
            ClassSession classSession = qrSession.getClassSession();

            // 2. Get student
            User student = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // 3. Check if already checked in
            if (attendanceRecordRepository.existsByStudentAndClassSession(student, classSession)) {
                return AttendanceResponse.failed("Bạn đã điểm danh cho buổi học này rồi",
                        AttendanceRecord.AttendanceStatus.FAILED_ALREADY_CHECKED.name());
            }

            // 4. Check device duplication
            if (attendanceRecordRepository.findBySessionAndDeviceUid(classSession, request.getDeviceUid())
                    .isPresent()) {
                return AttendanceResponse.failed("Thiết bị này đã được sử dụng để điểm danh",
                        AttendanceRecord.AttendanceStatus.FAILED_DUPLICATE_DEVICE.name());
            }

            // 5. Calculate distance
            double distance = calculateDistance(
                    qrSession.getTeacherLatitude(), qrSession.getTeacherLongitude(),
                    request.getStudentLat(), request.getStudentLong());

            // 6. Check distance constraint
            if (distance > qrSession.getMaxDistanceMeters()) {
                saveFailedRecord(student, classSession, request, distance,
                        AttendanceRecord.AttendanceStatus.FAILED_DISTANCE,
                        String.format("Khoảng cách %.2fm vượt quá giới hạn %dm",
                                distance, qrSession.getMaxDistanceMeters()));

                return AttendanceResponse.failed(
                        String.format("Bạn đang ở quá xa (%.2fm). Giới hạn: %dm",
                                distance, qrSession.getMaxDistanceMeters()),
                        AttendanceRecord.AttendanceStatus.FAILED_DISTANCE.name());
            }

            // 7. Save successful attendance record
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(classSession);
            record.setStudentLatitude(request.getStudentLat());
            record.setStudentLongitude(request.getStudentLong());
            record.setDistanceMeters(distance);
            record.setDeviceUid(request.getDeviceUid());
            record.setFaceDataUrl(request.getImageData());
            record.setStatus(AttendanceRecord.AttendanceStatus.SUCCESS);

            record = attendanceRecordRepository.save(record);

            return AttendanceResponse.success("Điểm danh thành công!", distance, record.getId());

        } catch (RuntimeException e) {
            return AttendanceResponse.failed(e.getMessage(),
                    AttendanceRecord.AttendanceStatus.FAILED_INVALID_QR.name());
        }
    }

    private AttendanceRecord saveFailedRecord(User student, ClassSession classSession,
                                              CheckInRequest request, double distance,
                                              AttendanceRecord.AttendanceStatus status, String reason) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setClassSession(classSession);
        record.setStudentLatitude(request.getStudentLat());
        record.setStudentLongitude(request.getStudentLong());
        record.setDistanceMeters(distance);
        record.setDeviceUid(request.getDeviceUid());
        record.setFaceDataUrl(request.getImageData());
        record.setStatus(status);
        record.setFailReason(reason);

        return attendanceRecordRepository.save(record);
    }

    // --- FIX 1: HOÀN THIỆN HÀM TÍNH KHOẢNG CÁCH ---
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Đã thêm return
    } // Đã thêm đóng ngoặc

    // --- FIX 2: SỬA LỖI CÚ PHÁP HÀM LẤY DANH SÁCH ---
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordDTO> getAttendanceBySession(Long sessionId) {
        // Cách 1: Dùng Custom Query (nếu repository của bạn có hàm này)
        List<AttendanceRecord> records = attendanceRecordRepository.findByClassSessionIdWithDetails(sessionId);
        return attendanceMapper.toDTOList(records);

        // Cách 2: Nếu chưa có hàm trên, dùng cách mặc định này (Bỏ comment nếu cần dùng):
        /*
        ClassSession classSession = new ClassSession();
        classSession.setId(sessionId);
        List<AttendanceRecord> records = attendanceRecordRepository.findByClassSession(classSession);
        return attendanceMapper.toDTOList(records);
        */
    }

    @Override // Thêm Override nếu có trong interface
    public List<AttendanceRecord> getStudentAttendance(Long studentId, Long classId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return attendanceRecordRepository.findByStudentAndClassId(student, classId);
    }
}