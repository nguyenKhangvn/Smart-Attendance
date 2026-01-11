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

    // ===== SELF-INJECTION: Để gọi @Transactional từ chính class này =====
    @Autowired
    @Lazy
    private SecureAttendanceService self;

    // Timezone cố định Việt Nam
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Cho phép lệch giờ tối đa 2 phút (120,000 ms)
    private static final long MAX_TIME_DRIFT_MS = 120_000;

    @Value("${school.network.ips}")
    private String schoolNetworkIps;

    /**
     * LOGIC CHÍNH: ĐIỂM DANH
     */
    public AttendanceCheckInResponse checkIn(AttendanceCheckInRequest request) {
        try {
            // =================================================================================
            // PHASE 1: CHEAP CHECKS (Kiểm tra nhanh - Validate Input, Time, Logic DB)
            // =================================================================================

            // 1. Validate Input cơ bản
            if (request.getSelfieBase64() == null || request.getSelfieBase64().trim().isEmpty()) {
                return AttendanceCheckInResponse.failed("Vui lòng chụp ảnh selfie!", "FAILED_MISSING_SELFIE");
            }

            // 2. Chống Replay Attack & Check Time Drift
            long serverTimeMs = System.currentTimeMillis();
            long clientTimeMs = request.getTimestamp();
            if (Math.abs(serverTimeMs - clientTimeMs) > MAX_TIME_DRIFT_MS) {
                return AttendanceCheckInResponse.failed(
                        "Thời gian thiết bị sai lệch quá lớn so với hệ thống. Vui lòng chỉnh lại giờ!",
                        "FAILED_TIME_SYNC");
            }

            // Lấy thời điểm thực tế user quét (đã tin cậy)
            LocalDateTime actualScannedTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(clientTimeMs), VIETNAM_ZONE);

            ZonedDateTime nowZoned = ZonedDateTime.now(VIETNAM_ZONE);
            LocalDateTime nowLocal = nowZoned.toLocalDateTime();

            // 3. Check Session Logic
            ClassSession session = classSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

            // Check thời gian học (Cho phép sớm 15p)
            if (session.getSessionDate().isAfter(nowLocal.plusMinutes(15))) {
                return AttendanceCheckInResponse.failed("Buổi học chưa bắt đầu!", "FAILED_SESSION_TOO_EARLY");
            }

            // Check hết giờ (Mặc định 90p nếu null)
            int duration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 90;
            if (session.getSessionDate().plusMinutes(duration).isBefore(nowLocal)) {
                return AttendanceCheckInResponse.failed("Buổi học đã kết thúc!", "FAILED_SESSION_TOO_LATE");
            }

            // Tự động chuyển trạng thái IN_PROGRESS nếu đến giờ
            if (!session.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS) &&
                    session.getSessionDate().isBefore(nowLocal.plusMinutes(15))) {
                session.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
                session.setUpdatedAt(nowLocal);
                classSessionRepository.save(session);
            }

            // 4. Validate Token QR
            Optional<QRSession> qrSessionOpt = qrSessionRepository.findValidQRSession(
                    request.getToken(), actualScannedTime);

            if (qrSessionOpt.isEmpty()) {
                return AttendanceCheckInResponse.failed("Mã QR đã hết hạn hoặc không hợp lệ!", "FAILED_INVALID_QR");
            }
            QRSession qrSession = qrSessionOpt.get();
            if (!qrSession.getClassSession().getId().equals(session.getId())) {
                return AttendanceCheckInResponse.failed("QR Code không đúng lớp học này!", "FAILED_INVALID_QR");
            }

            // 5. Xác định User & Check Lớp
            User student = findUser(request.getEmail());
            ClassEntity classEntity = session.getClassEntity();

            boolean isInClass = classRepository.existsByIdAndStudents_Id(classEntity.getId(), student.getId());
            if (!isInClass) {
                return AttendanceCheckInResponse.failed("Bạn không có tên trong danh sách lớp!", "FAILED_NOT_IN_CLASS");
            }

            // 6. Check Duplicate (Đã điểm danh chưa?)
            if (attendanceRecordRepository.existsByStudentAndClassSession(student, session)) {
                return AttendanceCheckInResponse.failed("Bạn đã điểm danh buổi này rồi!", "FAILED_ALREADY_CHECKED");
            }

            // 7. Check Device Owner (Chống 1 máy điểm danh hộ)
            Optional<AttendanceRecord> deviceUsed = attendanceRecordRepository.findBySessionAndDeviceUid(session,
                    request.getDeviceId());
            if (deviceUsed.isPresent() && !deviceUsed.get().getStudent().getId().equals(student.getId())) {
                return AttendanceCheckInResponse.failed("Thiết bị này đã được sử dụng bởi sinh viên khác!",
                        "FAILED_DUPLICATE_DEVICE");
            }

            // =================================================================================
            // PHASE 2: LOGIC CHECKS PHỨC TẠP (GPS Calculation & Network)
            // =================================================================================

            double distance = calculateDistance(
                    qrSession.getTeacherLatitude().doubleValue(), qrSession.getTeacherLongitude().doubleValue(),
                    request.getLatitude().doubleValue(), request.getLongitude().doubleValue());

            boolean isInSchoolNetwork = checkIpInNetwork(request.getClientIp());
            boolean isGpsPoor = request.getGpsAccuracy() != null && request.getGpsAccuracy() > 200; // Tăng từ 100m lên
                                                                                                    // 200m cho iOS

            AttendanceRecord.AttendanceStatus recordStatus = AttendanceRecord.AttendanceStatus.SUCCESS;
            String failReason = null;

            if (distance > qrSession.getMaxDistanceMeters()) {
                if (isInSchoolNetwork) {
                    // Logic mềm: Xa GPS nhưng đúng IP trường -> Chấp nhận luôn
                    System.out.println("DEBUG: Distance fail (" + distance + "m) but Valid IP -> ALLOWED");
                    // Không cần làm gì, tiếp tục với SUCCESS
                } else if (isGpsPoor) {
                    // Logic mềm: GPS kém (trên 200m accuracy) -> Pending Review thay vì Fail
                    recordStatus = AttendanceRecord.AttendanceStatus.GPS_POOR_SIGNAL;
                    failReason = String.format(
                            "GPS kém (độ chính xác: %.0fm), khoảng cách tính: %.2fm. Chờ giáo viên duyệt.",
                            request.getGpsAccuracy(), distance);
                    System.out
                            .println("DEBUG: GPS Poor (" + request.getGpsAccuracy() + "m accuracy) -> PENDING_REVIEW");
                } else if (distance > qrSession.getMaxDistanceMeters() * 2) {
                    // Chỉ fail nếu xa gấp đôi giới hạn (rất xa thực sự)
                    saveFailedRecord(student, session, request, distance,
                            AttendanceRecord.AttendanceStatus.FAILED_DISTANCE,
                            String.format("Quá xa (%.2fm > %dm)", distance,
                                    qrSession.getMaxDistanceMeters()));

                    return AttendanceCheckInResponse.failed(
                            String.format("Bạn ở quá xa (%.2fm). Vui lòng đến gần hơn!", distance),
                            "FAILED_DISTANCE");
                } else {
                    // Khoảng cách vừa phải + GPS OK -> Pending Review để giáo viên check
                    recordStatus = AttendanceRecord.AttendanceStatus.PENDING_REVIEW;
                    failReason = String.format("Khoảng cách %.2fm, GPS OK. Chờ giáo viên duyệt.", distance);
                    System.out.println("DEBUG: Distance slightly over (" + distance + "m) -> PENDING_REVIEW");
                }
            }

            // Validate định dạng ảnh (Local check)
            if (!faceRecognitionService.validateBase64Image(request.getSelfieBase64())) {
                return AttendanceCheckInResponse.failed("Ảnh không hợp lệ, vui lòng chụp lại!", "FAILED_INVALID_IMAGE");
            }

            // =================================================================================
            // PHASE 3: EXPENSIVE CHECKS (Upload ảnh - Tốn tài nguyên mạng/Cloud)
            // =================================================================================

            String imageUrl;
            try {
                // Upload ảnh điểm danh
                imageUrl = cloudinaryService.uploadFaceImage(
                        request.getSelfieBase64(), student.getId(), session.getId());

                // (Optional) Tạo Face Profile nếu chưa có
                if (student.getFaceProfileUrl() == null || student.getFaceProfileUrl().isEmpty()) {
                    String profileUrl = faceRecognitionService.uploadProfileFace(request.getSelfieBase64(),
                            student.getId());
                    student.setFaceProfileUrl(profileUrl);
                    userRepository.save(student);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return AttendanceCheckInResponse.failed("Lỗi upload ảnh minh chứng. Kiểm tra kết nối mạng!",
                        "FAILED_IMAGE_UPLOAD");
            }

            // =================================================================================
            // PHASE 4: TRANSACTIONAL SAVE (Lưu dữ liệu cuối cùng)
            // =================================================================================

            // Gọi qua 'self' bean để kích hoạt Proxy Transaction
            return self.saveAttendanceRecordTransactional(
                    student, session, request, distance, imageUrl, actualScannedTime, nowLocal, recordStatus,
                    failReason);

        } catch (Exception e) {
            e.printStackTrace();
            return AttendanceCheckInResponse.failed("Lỗi hệ thống: " + e.getMessage(), "FAILED_SYSTEM_ERROR");
        }
    }

    /**
     * Helper: Tìm user theo email hoặc username
     */
    private User findUser(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
    }

    /**
     * Helper: Check IP có thuộc dải mạng trường không
     */
    private boolean checkIpInNetwork(String clientIp) {
        if (clientIp == null || schoolNetworkIps == null || schoolNetworkIps.isEmpty())
            return false;

        // schoolNetworkIps định dạng ví dụ: "192.168.1., 10.0.0."
        for (String prefix : schoolNetworkIps.split(",")) {
            if (clientIp.trim().startsWith(prefix.trim()))
                return true;
        }
        return false;
    }

    /**
     * Helper: Công thức Haversine tính khoảng cách 2 điểm GPS (trả về mét)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Bán kính trái đất (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c * 1000; // Đổi ra mét
    }

    /**
     * TRANSACTIONAL METHOD: Lưu record thành công/pending vào DB
     * Được tách ra để đảm bảo Transaction hoạt động đúng sau khi các bước check ở
     * trên đã xong.
     */
    @Transactional
    public AttendanceCheckInResponse saveAttendanceRecordTransactional(
            User student, ClassSession session, AttendanceCheckInRequest request,
            double distance, String imageUrl, LocalDateTime actualScannedTime,
            LocalDateTime nowLocal, AttendanceRecord.AttendanceStatus status, String reason) {

        try {
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(session);
            record.setStudentLatitude(request.getLatitude());
            record.setStudentLongitude(request.getLongitude());
            record.setDistanceMeters(BigDecimal.valueOf(distance));
            record.setDeviceUid(request.getDeviceId());
            record.setFaceDataUrl(imageUrl);
            record.setCheckedInAt(actualScannedTime);

            // Kiểm tra gửi trễ (Offline Sync)
            long delayMinutes = Duration.between(actualScannedTime, nowLocal).toMinutes();
            if (delayMinutes > 30) {
                record.setStatus(AttendanceRecord.AttendanceStatus.PENDING_REVIEW);
                record.setFailReason(
                        reason != null ? reason + " | " : "" + "Gửi dữ liệu trễ " + delayMinutes + " phút");
            } else {
                record.setStatus(status);
                record.setFailReason(reason);
            }

            record = attendanceRecordRepository.save(record);

            String message = (record.getStatus() == AttendanceRecord.AttendanceStatus.PENDING_REVIEW)
                    ? "Điểm danh thành công (Chờ duyệt do gửi trễ hoặc GPS yếu)."
                    : "Điểm danh thành công!";

            return AttendanceCheckInResponse.success(message, distance, record.getId());

        } catch (DataIntegrityViolationException e) {
            // Catch lỗi trùng lặp nếu 2 request cùng lọt qua check ban đầu
            return AttendanceCheckInResponse.failed("Bạn đã điểm danh rồi (Dữ liệu trùng lặp)!",
                    "FAILED_ALREADY_CHECKED");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu DB: " + e.getMessage());
        }
    }

    /**
     * Lưu log khi điểm danh thất bại (Vượt quá khoảng cách, lỗi GPS...)
     * Không throw exception để không làm crash luồng chính
     */
    private void saveFailedRecord(User student, ClassSession session, AttendanceCheckInRequest request,
            double distance, AttendanceRecord.AttendanceStatus status, String reason) {
        try {
            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(session);
            record.setStudentLatitude(request.getLatitude()); // Lưu tọa độ lỗi để đối soát
            record.setStudentLongitude(request.getLongitude());
            record.setDistanceMeters(BigDecimal.valueOf(distance));
            record.setDeviceUid(request.getDeviceId());
            record.setStatus(status);
            record.setFailReason(reason);
            record.setFaceDataUrl("FAILED_NO_IMAGE"); // Không có ảnh vì chưa upload
            record.setCheckedInAt(LocalDateTime.now(VIETNAM_ZONE));

            attendanceRecordRepository.save(record);
        } catch (Exception e) {
            System.err.println("Không thể lưu failed record: " + e.getMessage());
        }
    }
}