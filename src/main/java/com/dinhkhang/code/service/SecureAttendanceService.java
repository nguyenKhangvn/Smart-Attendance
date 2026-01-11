package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.AttendanceCheckInRequest;
import com.dinhkhang.code.dto.AttendanceCheckInResponse;
import com.dinhkhang.code.entity.*;
import com.dinhkhang.code.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
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
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    @Lazy
    private SecureAttendanceService self;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_TIME_DRIFT_MS = 120_000;

    @Value("${school.network.ips}")
    private String schoolNetworkIps;

    public AttendanceCheckInResponse checkIn(AttendanceCheckInRequest request) {
        try {
            // =================================================================================
            // PHASE 1: CHEAP CHECKS (Validation cơ bản) - GIỮ NGUYÊN
            // =================================================================================
            if (request.getSelfieBase64() == null || request.getSelfieBase64().trim().isEmpty()) {
                return AttendanceCheckInResponse.failed("Vui lòng chụp ảnh selfie!", "FAILED_MISSING_SELFIE");
            }

            long serverTimeMs = System.currentTimeMillis();
            if (Math.abs(serverTimeMs - request.getTimestamp()) > MAX_TIME_DRIFT_MS) {
                return AttendanceCheckInResponse.failed("Thời gian thiết bị sai lệch. Vui lòng chỉnh lại giờ!",
                        "FAILED_TIME_SYNC");
            }

            LocalDateTime actualScannedTime = LocalDateTime
                    .ofInstant(java.time.Instant.ofEpochMilli(request.getTimestamp()), VIETNAM_ZONE);
            ZonedDateTime nowZoned = ZonedDateTime.now(VIETNAM_ZONE);
            LocalDateTime nowLocal = nowZoned.toLocalDateTime();

            ClassSession session = classSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

            if (session.getSessionDate().isAfter(nowLocal.plusMinutes(15))) {
                return AttendanceCheckInResponse.failed("Buổi học chưa bắt đầu!", "FAILED_SESSION_TOO_EARLY");
            }

            int duration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 90;
            if (session.getSessionDate().plusMinutes(duration).isBefore(nowLocal)) {
                return AttendanceCheckInResponse.failed("Buổi học đã kết thúc!", "FAILED_SESSION_TOO_LATE");
            }

            if (!session.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS) &&
                    session.getSessionDate().isBefore(nowLocal.plusMinutes(15))) {
                session.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
                session.setUpdatedAt(nowLocal);
                classSessionRepository.save(session);
            }

            Optional<QRSession> qrSessionOpt = qrSessionRepository.findValidQRSession(request.getToken(),
                    actualScannedTime);
            if (qrSessionOpt.isEmpty()) {
                return AttendanceCheckInResponse.failed("Mã QR không hợp lệ!", "FAILED_INVALID_QR");
            }
            QRSession qrSession = qrSessionOpt.get();
            if (!qrSession.getClassSession().getId().equals(session.getId())) {
                return AttendanceCheckInResponse.failed("QR Code sai lớp!", "FAILED_INVALID_QR");
            }

            User student = findUser(request.getEmail());
            ClassEntity classEntity = session.getClassEntity();

            if (!classRepository.existsByIdAndStudents_Id(classEntity.getId(), student.getId())) {
                return AttendanceCheckInResponse.failed("Bạn không có trong danh sách lớp!", "FAILED_NOT_IN_CLASS");
            }

            if (attendanceRecordRepository.existsByStudentAndClassSession(student, session)) {
                return AttendanceCheckInResponse.failed("Bạn đã điểm danh rồi!", "FAILED_ALREADY_CHECKED");
            }

            Optional<AttendanceRecord> deviceUsed = attendanceRecordRepository.findBySessionAndDeviceUid(session,
                    request.getDeviceId());
            if (deviceUsed.isPresent() && !deviceUsed.get().getStudent().getId().equals(student.getId())) {
                return AttendanceCheckInResponse.failed("Thiết bị này đã được dùng bởi sinh viên khác!",
                        "FAILED_DUPLICATE_DEVICE");
            }

            // =================================================================================
            // PHASE 2: UPLOAD ẢNH TRƯỚC (QUAN TRỌNG: Để có bằng chứng đối soát)
            // =================================================================================
            // Thay đổi chiến thuật: Upload luôn để lưu bằng chứng, kể cả khi GPS sai

            // Validate ảnh local trước cho nhanh
            if (!faceRecognitionService.validateBase64Image(request.getSelfieBase64())) {
                return AttendanceCheckInResponse.failed("Ảnh lỗi, vui lòng chụp lại!", "FAILED_INVALID_IMAGE");
            }

            String imageUrl;
            try {
                imageUrl = cloudinaryService.uploadFaceImage(
                        request.getSelfieBase64(), student.getId(), session.getId());

                // Tạo profile nếu chưa có (Optional)
                if (student.getFaceProfileUrl() == null) {
                    String pUrl = faceRecognitionService.uploadProfileFace(request.getSelfieBase64(), student.getId());
                    student.setFaceProfileUrl(pUrl);
                    userRepository.save(student);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return AttendanceCheckInResponse.failed("Lỗi upload ảnh. Kiểm tra mạng!", "FAILED_IMAGE_UPLOAD");
            }

            // =================================================================================
            // PHASE 3: LOGIC CHECKS & QUYẾT ĐỊNH TRẠNG THÁI
            // =================================================================================

            // 1. Check GPS
            boolean hasValidGPS = request.getLatitude() != null
                    && request.getLongitude() != null
                    && Math.abs(request.getLatitude().doubleValue()) > 0.000001;

            double distance = 0.0;
            if (hasValidGPS) {
                distance = calculateDistance(
                        qrSession.getTeacherLatitude().doubleValue(), qrSession.getTeacherLongitude().doubleValue(),
                        request.getLatitude().doubleValue(), request.getLongitude().doubleValue());
            }

            boolean isInSchoolNetwork = checkIpInNetwork(request.getClientIp());
            boolean isGpsPoor = request.getGpsAccuracy() != null && request.getGpsAccuracy() > 200;

            // Mặc định là thành công
            AttendanceRecord.AttendanceStatus recordStatus = AttendanceRecord.AttendanceStatus.SUCCESS;
            String failReason = null;
            String messageToUser = "Điểm danh thành công!";

            // --- BẮT ĐẦU PHÁN XÉT ---

            if (!hasValidGPS) {
                if (isInSchoolNetwork) {
                    // Không GPS nhưng đúng IP trường -> OK
                    failReason = "IP trường (Không GPS)";
                } else {
                    // Không GPS, Mạng lạ -> PENDING_REVIEW (Thay vì Failed)
                    // Để giáo viên xem ảnh: Nếu đúng là đang ngồi trong lớp thì duyệt thủ công
                    recordStatus = AttendanceRecord.AttendanceStatus.PENDING_REVIEW;
                    failReason = "Không có GPS + Mạng lạ. Chờ GV duyệt ảnh.";
                    messageToUser = "Không xác định được vị trí. Đã gửi ảnh để giáo viên duyệt thủ công.";
                }
            } else {
                // Có GPS
                if (distance > qrSession.getMaxDistanceMeters()) {
                    if (isInSchoolNetwork) {
                        // Xa nhưng IP đúng -> OK
                        System.out.println("Allowed by IP");
                    } else {
                        // Xa quá quy định -> PENDING_REVIEW (Chờ duyệt)
                        // Đây chính là chỗ giải quyết bài toán "Lỡ hệ thống lỗi"
                        recordStatus = AttendanceRecord.AttendanceStatus.PENDING_REVIEW;

                        if (isGpsPoor) {
                            failReason = String.format("GPS yếu (acc: %.0fm), kc: %.2fm. Chờ duyệt.",
                                    request.getGpsAccuracy(), distance);
                        } else {
                            failReason = String.format("Quá xa (%.2fm > %dm). Chờ GV kiểm tra ảnh.", distance,
                                    qrSession.getMaxDistanceMeters());
                        }

                        messageToUser = String.format(
                                "Vị trí quá xa (%.2fm). Đã gửi yêu cầu duyệt thủ công tới giáo viên.", distance);
                    }
                }
            }

            // =================================================================================
            // PHASE 4: LƯU DB (CÓ ẢNH DÙ FAIL HAY PASS)
            // =================================================================================

            return self.saveAttendanceRecordTransactional(
                    student, session, request, distance, imageUrl, actualScannedTime, nowLocal, recordStatus,
                    failReason, messageToUser);

        } catch (Exception e) {
            e.printStackTrace();
            return AttendanceCheckInResponse.failed("Lỗi hệ thống: " + e.getMessage(), "FAILED_SYSTEM_ERROR");
        }
    }

    // ... (Các hàm findUser, checkIpInNetwork, calculateDistance giữ nguyên) ...
    private User findUser(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean checkIpInNetwork(String clientIp) {
        if (clientIp == null || schoolNetworkIps == null)
            return false;
        for (String prefix : schoolNetworkIps.split(",")) {
            if (clientIp.trim().startsWith(prefix.trim()))
                return true;
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    @Transactional
    public AttendanceCheckInResponse saveAttendanceRecordTransactional(
            User student, ClassSession session, AttendanceCheckInRequest request,
            double distance, String imageUrl, LocalDateTime actualScannedTime,
            LocalDateTime nowLocal, AttendanceRecord.AttendanceStatus status, String reason, String successMessage) {

        try {
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(session);
            record.setStudentLatitude(request.getLatitude());
            record.setStudentLongitude(request.getLongitude());
            record.setDistanceMeters(BigDecimal.valueOf(distance));
            record.setDeviceUid(request.getDeviceId());
            record.setFaceDataUrl(imageUrl); // <--- QUAN TRỌNG: Luôn có ảnh
            record.setCheckedInAt(actualScannedTime);

            // Logic Offline Sync (Gửi trễ)
            long delayMinutes = Duration.between(actualScannedTime, nowLocal).toMinutes();
            if (delayMinutes > 30) {
                record.setStatus(AttendanceRecord.AttendanceStatus.PENDING_REVIEW);
                String lateNote = "Gửi trễ " + delayMinutes + " phút";
                record.setFailReason(reason != null ? reason + " | " + lateNote : lateNote);
                successMessage = "Điểm danh thành công (Chờ duyệt do gửi trễ).";
            } else {
                record.setStatus(status);
                record.setFailReason(reason);
            }

            record = attendanceRecordRepository.save(record);

            // Dù là PENDING_REVIEW thì vẫn trả về success=true cho Client để sinh viên yên
            // tâm
            // Nhưng message sẽ báo rõ là "Đang chờ duyệt"
            return AttendanceCheckInResponse.success(successMessage, distance, record.getId());

        } catch (DataIntegrityViolationException e) {
            return AttendanceCheckInResponse.failed("Bạn đã điểm danh rồi!", "FAILED_ALREADY_CHECKED");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}