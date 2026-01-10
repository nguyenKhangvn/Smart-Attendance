package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.AttendanceCheckInRequest;
import com.dinhkhang.code.dto.AttendanceCheckInResponse;
import com.dinhkhang.code.entity.*;
import com.dinhkhang.code.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SERVICE ĐIỂM DANH MỚI - BẢO MẬT CAO
 * QR chỉ chứa: sessionId + token
 * Kiểm tra: Email trong lớp + GPS + Device + Token hợp lệ
 */
@Service
@Transactional
public class SecureAttendanceService {

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private QRSessionRepository qrSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private ClassRepository classRepository;

    /**
     * ĐIỂM DANH - LUỒNG BẢO MẬT
     *
     * CHECK 1: Session còn hiệu lực?
     * CHECK 2: Token hợp lệ?
     * CHECK 3: Email có trong lớp?
     * CHECK 4: Đã điểm danh chưa?
     * CHECK 5: GPS trong bán kính?
     * CHECK 6: Device đã dùng chưa?
     */
    public AttendanceCheckInResponse checkIn(AttendanceCheckInRequest request) {
        try {
            // CHECK 1: Session còn hiệu lực không?
            ClassSession session = classSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

            if (!session.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS)) {
                return AttendanceCheckInResponse.failed(
                        "Buổi học chưa bắt đầu hoặc đã kết thúc",
                        "FAILED_SESSION_NOT_ACTIVE");
            }

            // CHECK 2: Token hợp lệ?
            QRSession qrSession = qrSessionRepository
                    .findValidQRSession(request.getToken(), LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("QR Code không hợp lệ hoặc đã hết hạn"));

            if (!qrSession.getClassSession().getId().equals(session.getId())) {
                return AttendanceCheckInResponse.failed(
                        "QR Code không khớp với buổi học",
                        "FAILED_INVALID_QR");
            }

            // CHECK 3: Email có trong lớp không?
            User student = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));

            ClassEntity classEntity = session.getClassEntity();

            // KIỂM TRA QUAN TRỌNG: Email có trong danh sách lớp?
            boolean isInClass = classEntity.getStudents().stream()
                    .anyMatch(s -> s.getEmail().equals(request.getEmail()));

            if (!isInClass) {
                return AttendanceCheckInResponse.failed(
                        "Bạn không thuộc lớp học này. Vui lòng liên hệ giáo viên!",
                        "FAILED_NOT_IN_CLASS");
            }

            // CHECK 4: Đã điểm danh chưa?
            if (attendanceRecordRepository.existsByStudentAndClassSession(student, session)) {
                return AttendanceCheckInResponse.failed(
                        "Bạn đã điểm danh cho buổi học này rồi",
                        "FAILED_ALREADY_CHECKED");
            }

            // CHECK 5: GPS trong bán kính?
            double distance = calculateDistance(
                    qrSession.getTeacherLatitude().doubleValue(),
                    qrSession.getTeacherLongitude().doubleValue(),
                    request.getLatitude().doubleValue(),
                    request.getLongitude().doubleValue());

            if (distance > qrSession.getMaxDistanceMeters()) {
                // Lưu record thất bại
                saveFailedRecord(student, session, request, distance,
                        AttendanceRecord.AttendanceStatus.FAILED_DISTANCE,
                        String.format("Khoảng cách %.2fm vượt quá %.0fm", distance,
                                (double) qrSession.getMaxDistanceMeters()));

                return AttendanceCheckInResponse.failed(
                        String.format("Bạn ở quá xa (%.2fm). Giới hạn: %dm", distance,
                                qrSession.getMaxDistanceMeters()),
                        "FAILED_DISTANCE");
            }

            // CHECK 6: Device đã dùng chưa? (chống gian lận quét hộ)
            if (attendanceRecordRepository.findBySessionAndDeviceUid(session, request.getDeviceId()).isPresent()) {
                return AttendanceCheckInResponse.failed(
                        "Thiết bị này đã được sử dụng để điểm danh",
                        "FAILED_DUPLICATE_DEVICE");
            }

            // ✅ TẤT CẢ CHECK PASS - LƯU ĐIỂM DANH THÀNH CÔNG
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(session);
            record.setStudentLatitude(request.getLatitude());
            record.setStudentLongitude(request.getLongitude());
            record.setDistanceMeters(BigDecimal.valueOf(distance));
            record.setDeviceUid(request.getDeviceId());
            record.setFaceDataUrl(request.getSelfieBase64());
            record.setStatus(AttendanceRecord.AttendanceStatus.SUCCESS);
            record.setCheckedInAt(LocalDateTime.now());

            record = attendanceRecordRepository.save(record);

            return AttendanceCheckInResponse.success(
                    "✅ Điểm danh thành công! Khoảng cách: " + String.format("%.2fm", distance),
                    distance,
                    record.getId());

        } catch (RuntimeException e) {
            return AttendanceCheckInResponse.failed(
                    "Lỗi: " + e.getMessage(),
                    "FAILED_INVALID_QR");
        }
    }

    /**
     * Lưu record thất bại để tracking
     */
    private void saveFailedRecord(User student, ClassSession session,
            AttendanceCheckInRequest request,
            double distance,
            AttendanceRecord.AttendanceStatus status,
            String reason) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setClassSession(session);
        record.setStudentLatitude(request.getLatitude());
        record.setStudentLongitude(request.getLongitude());
        record.setDistanceMeters(BigDecimal.valueOf(distance));
        record.setDeviceUid(request.getDeviceId());
        record.setFaceDataUrl(request.getSelfieBase64());
        record.setStatus(status);
        record.setFailReason(reason);
        attendanceRecordRepository.save(record);
    }

    /**
     * Haversine formula - Tính khoảng cách GPS chính xác
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
