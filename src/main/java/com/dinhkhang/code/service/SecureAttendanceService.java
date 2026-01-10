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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

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

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    // ===== TIMEZONE: LUÔN SỬ DỤNG GIỜ VIỆT NAM (UTC+7) =====
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ===== CẢI TIẾN 1: CHECK PUBLIC IP THAY VÌ WIFI SSID =====
    @Value("${school.network.ips}")
    private String schoolNetworkIps;

    @Value("${max.device.changes.per.semester:3}")
    private int maxDeviceChanges;

    /**
     * ĐIỂM DANH - LUỒNG BẢO MẬT
     *
     * CHECK 1: Session còn hiệu lực?
     * CHECK 2: Token hợp lệ?
     * CHECK 3: Email có trong lớp?
     * CHECK 4: Đã điểm danh chưa?
     * CHECK 5: GPS trong bán kính? (Soft check với WiFi SSID)
     * CHECK 6: Device đã dùng chưa?
     * CHECK 7: Face Recognition
     */
    public AttendanceCheckInResponse checkIn(AttendanceCheckInRequest request) {
        try {
            // Khai báo biến cho face recognition
            String tempFaceUrl = null;
            boolean faceVerified = false;

            // CẢNH BÁO GPS INDOOR: Nếu GPS accuracy quá kém (>100m), cảnh báo trước
            boolean isGpsPoorSignal = false;
            if (request.getGpsAccuracy() != null && request.getGpsAccuracy() > 100) {
                isGpsPoorSignal = true;
                System.out.println("⚠️ GPS Poor Signal Detected: " + request.getGpsAccuracy() + "m accuracy");
            }
            // CHECK 1: Session còn hiệu lực không?
            ClassSession session = classSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Session không tồn tại"));

            // ===== CẢI TIẾN: CHO PHÉP ĐIỂM DANH SỚM HƠN =====
            // Cho phép điểm danh 15 phút trước khi session bắt đầu
            // ===== TIMEZONE FIX: Sử dụng giờ Việt Nam =====
            ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
            LocalDateTime nowLocal = now.toLocalDateTime();
            boolean isTooEarly = session.getSessionDate().isAfter(nowLocal.plusMinutes(15));
            boolean isTooLate = session.getSessionDate().plusMinutes(session.getDurationMinutes() != null ? session.getDurationMinutes() : 90).isBefore(nowLocal);

            if (isTooEarly) {
                return AttendanceCheckInResponse.failed(
                        "Buổi học chưa bắt đầu. Vui lòng chờ đến giờ học!",
                        "FAILED_SESSION_TOO_EARLY");
            }

            if (isTooLate) {
                return AttendanceCheckInResponse.failed(
                        "Buổi học đã kết thúc!",
                        "FAILED_SESSION_TOO_LATE");
            }

            // Nếu session chưa bắt đầu nhưng đã đến giờ (trong khoảng 15 phút), tự động chuyển sang IN_PROGRESS
            if (!session.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS) &&
                session.getSessionDate().isBefore(nowLocal.plusMinutes(15))) {
                session.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
                session.setUpdatedAt(nowLocal);
                session = classSessionRepository.save(session);
                System.out.println("✅ Session " + session.getId() + " auto-started (within 15min window)");
            }

            if (!session.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS)) {
                return AttendanceCheckInResponse.failed(
                        "Buổi học chưa bắt đầu hoặc đã kết thúc",
                        "FAILED_SESSION_NOT_ACTIVE");
            }

            // CHECK 2: Token hợp lệ?
            QRSession qrSession = qrSessionRepository
                    .findValidQRSession(request.getToken(), nowLocal)
                    .orElseThrow(() -> new RuntimeException("QR Code không hợp lệ hoặc đã hết hạn"));

            if (!qrSession.getClassSession().getId().equals(session.getId())) {
                return AttendanceCheckInResponse.failed(
                        "QR Code không khớp với buổi học",
                        "FAILED_INVALID_QR");
            }

            // CHECK 3: User có tồn tại không? (tìm bằng username từ authentication)
            User student = userRepository.findByUsername(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trong hệ thống"));

            ClassEntity classEntity = session.getClassEntity();

            // KIỂM TRA QUAN TRỌNG: User có trong danh sách lớp? (So sánh ID để chính xác)
            boolean isInClass = classEntity.getStudents().stream()
                    .anyMatch(s -> s.getId().equals(student.getId()));

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

            // CHECK 5: GPS trong bán kính? (Soft check với School Network IP)
            double distance = calculateDistance(
                    qrSession.getTeacherLatitude().doubleValue(),
                    qrSession.getTeacherLongitude().doubleValue(),
                    request.getLatitude().doubleValue(),
                    request.getLongitude().doubleValue());

            // ===== CẢI TIẾN: CHECK PUBLIC IP THAY VÌ WIFI SSID =====
            // Trình duyệt không cho phép lấy WiFi SSID, thay vào đó check IP của client
            boolean isInSchoolNetwork = false;
            String clientIp = request.getClientIp(); // IP sẽ được gửi từ frontend hoặc lấy từ request
            if (clientIp != null && schoolNetworkIps != null) {
                String[] ipPrefixes = schoolNetworkIps.split(",");
                for (String prefix : ipPrefixes) {
                    if (clientIp.trim().startsWith(prefix.trim())) {
                        isInSchoolNetwork = true;
                        System.out.println("✅ Client IP in School Network: " + clientIp);
                        break;
                    }
                }
            }

            // LOGIC MỀM: Nếu GPS sai số lớn, xử lý linh hoạt
            if (distance > qrSession.getMaxDistanceMeters()) {
                // Nếu đang trong mạng trường (dựa vào Public IP) -> Chấp nhận
                if (isInSchoolNetwork) {
                    System.out.println(
                            "⚠️ GPS Distance: " + distance + "m (Exceeds limit) BUT in School Network -> ALLOW");
                    // Cho phép tiếp tục
                }
                // Nếu GPS accuracy kém (<100m) -> PENDING_REVIEW thay vì FAILED
                else if (isGpsPoorSignal) {
                    saveFailedRecord(student, session, request, distance,
                            AttendanceRecord.AttendanceStatus.GPS_POOR_SIGNAL,
                            String.format(
                                    "GPS không ổn định (accuracy: %.0fm). Khoảng cách: %.2fm. Chờ giáo viên xác nhận.",
                                    request.getGpsAccuracy(), distance));

                    return AttendanceCheckInResponse.failed(
                            String.format(
                                    "GPS không ổn định. Khoảng cách: %.2fm. Đã gửi yêu cầu xác nhận đến giáo viên.",
                                    distance),
                            "GPS_POOR_SIGNAL");
                }
                // Trường hợp GPS tốt nhưng ở xa thật -> FAILED
                else {
                    saveFailedRecord(student, session, request, distance,
                            AttendanceRecord.AttendanceStatus.FAILED_DISTANCE,
                            String.format("Khoảng cách %.2fm vượt quá %.0fm", distance,
                                    (double) qrSession.getMaxDistanceMeters()));

                    return AttendanceCheckInResponse.failed(
                            String.format("Bạn ở quá xa (%.2fm). Giới hạn: %dm", distance,
                                    qrSession.getMaxDistanceMeters()),
                            "FAILED_DISTANCE");
                }
            }

            // CHECK 6: Device đã dùng chưa? (chống gian lận quét hộ)
            // ===== CẢI TIẾN: PHÂN BIỆT GIAN LẬN VÀ ĐỔI MÁY HỢP LỆ =====
            Optional<AttendanceRecord> deviceUsed = attendanceRecordRepository.findBySessionAndDeviceUid(session,
                    request.getDeviceId());
            if (deviceUsed.isPresent()) {
                // Kiểm tra: Device này đã điểm danh cho AI?
                if (!deviceUsed.get().getStudent().getId().equals(student.getId())) {
                    // GIAN LẬN: Một thiết bị điểm danh cho NHIỀU tài khoản
                    return AttendanceCheckInResponse.failed(
                            "Thiết bị này đã được sử dụng bởi sinh viên khác!",
                            "FAILED_DUPLICATE_DEVICE");
                } else {
                    // Trùng: Sinh viên này đã điểm danh rồi (đã check ở CHECK 4)
                    return AttendanceCheckInResponse.failed(
                            "Bạn đã điểm danh cho buổi học này rồi",
                            "FAILED_ALREADY_CHECKED");
                }
            }

            // CHECK 6.1: Kiểm tra số lần đổi thiết bị trong kỳ (Chống tool spam xóa cache)
            // TODO: Implement count device changes per semester
            // long deviceChangeCount =
            // attendanceRecordRepository.countDistinctDevicesByStudentThisSemester(student.getId());
            // if (deviceChangeCount > maxDeviceChanges) {
            // return AttendanceCheckInResponse.failed(
            // "Bạn đã đổi thiết bị quá nhiều lần trong kỳ. Vui lòng liên hệ giáo viên!",
            // "FAILED_TOO_MANY_DEVICE_CHANGES");
            // }

            // CHECK 7: FACE RECOGNITION - Xác thực khuôn mặt
            if (request.getSelfieBase64() != null && !request.getSelfieBase64().trim().isEmpty()) {
                try {
                    // Kiểm tra xem sinh viên đã có face profile chưa
                    if (student.getFaceProfileUrl() == null || student.getFaceProfileUrl().trim().isEmpty()) {
                        // Lần đầu: Upload ảnh làm face profile
                        String profileFaceUrl = faceRecognitionService.uploadProfileFace(
                                request.getSelfieBase64(),
                                student.getId());

                        // Lưu face profile URL vào database
                        student.setFaceProfileUrl(profileFaceUrl);
                        userRepository.save(student);

                        System.out.println("Created face profile for student: " + student.getId());
                    } else {
                        // Đã có profile: Verify khuôn mặt
                        // Upload ảnh tạm thời để verify
                        tempFaceUrl = faceRecognitionService.uploadTempAttendanceFace(
                                request.getSelfieBase64(),
                                student.getId(),
                                session.getId());

                        // Verify khuôn mặt
                        faceVerified = faceRecognitionService.simpleFaceVerification(
                                student.getFaceProfileUrl(),
                                tempFaceUrl);

                        // Xóa ảnh tạm thời sau khi verify
                        if (tempFaceUrl != null) {
                            String tempPublicId = faceRecognitionService.extractPublicIdFromUrl(tempFaceUrl);
                            if (tempPublicId != null) {
                                faceRecognitionService.deleteTempFace(tempPublicId);
                            }
                        }

                        if (!faceVerified) {
                            // Lưu failed record với lý do face verification failed
                            saveFailedRecord(student, session, request, distance,
                                    AttendanceRecord.AttendanceStatus.FAILED_FACE_VERIFICATION,
                                    "Xác thực khuôn mặt thất bại - không khớp với profile");

                            return AttendanceCheckInResponse.failed(
                                    "Xác thực khuôn mặt thất bại. Vui lòng thử lại với ảnh rõ nét hơn!",
                                    "FAILED_FACE_VERIFICATION");
                        }
                    }
                } catch (Exception e) {
                    // Nếu face recognition lỗi, vẫn cho điểm danh nhưng ghi log
                    System.err.println("Face recognition error: " + e.getMessage());
                    // Có thể return failed hoặc continue tùy policy
                }
            }

            // TẤT CẢ CHECK PASS - LƯU ĐIỂM DANH THÀNH CÔNG

            // ===== KIỂM TRA BẮT BUỘC: PHẢI CÓ ẢNH SELFIE =====
            if (request.getSelfieBase64() == null || request.getSelfieBase64().trim().isEmpty()) {
                return AttendanceCheckInResponse.failed(
                        "Vui lòng chụp ảnh selfie để điểm danh!",
                        "FAILED_MISSING_SELFIE");
            }

            // Xử lý ảnh: Nếu đã verify face thành công, dùng tempFaceUrl, nếu không thì upload mới
            String imageUrl = null;
            try {
                System.out.println("📸 Starting image upload for student: " + student.getId() + " (Session: " + session.getId() + ")");
                
                // Kiểm tra xem đã có tempFaceUrl từ face verification chưa
                if (tempFaceUrl != null && faceVerified) {
                    // Dùng ảnh đã verify thành công
                    imageUrl = tempFaceUrl;
                    // Đánh dấu không xóa ảnh này vì sẽ lưu làm attendance image
                    tempFaceUrl = null;
                    System.out.println("✅ Using verified face image: " + imageUrl);
                } else {
                    // Upload ảnh mới (trường hợp không có face verification)
                    imageUrl = cloudinaryService.uploadFaceImage(
                            request.getSelfieBase64(),
                            student.getId(),
                            session.getId());
                    System.out.println("✅ Image uploaded successfully: " + imageUrl);
                }
                
                // KIỂM TRA BẮT BUỘC: URL phải hợp lệ
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    throw new RuntimeException("Upload thành công nhưng không nhận được URL từ Cloudinary");
                }
                
            } catch (Exception e) {
                // 🔴 KHÔNG NUỐT LỖI - Từ chối điểm danh nếu không lưu được ảnh
                System.err.println("❌ CRITICAL: Failed to upload attendance image!");
                System.err.println("   Student ID: " + student.getId());
                System.err.println("   Session ID: " + session.getId());
                System.err.println("   Error: " + e.getMessage());
                e.printStackTrace();
                
                // Lưu failed record để tracking
                saveFailedRecord(student, session, request, distance,
                        AttendanceRecord.AttendanceStatus.FAILED_IMAGE_UPLOAD,
                        "Lỗi lưu ảnh minh chứng: " + e.getMessage());
                
                return AttendanceCheckInResponse.failed(
                        "Không thể lưu ảnh điểm danh. Vui lòng kiểm tra kết nối mạng và thử lại!",
                        "FAILED_IMAGE_UPLOAD");
            }

            // Xóa ảnh tạm thời nếu còn (chỉ xóa nếu không dùng làm attendance image)
            if (tempFaceUrl != null) {
                String tempPublicId = faceRecognitionService.extractPublicIdFromUrl(tempFaceUrl);
                if (tempPublicId != null) {
                    faceRecognitionService.deleteTempFace(tempPublicId);
                }
            }

            AttendanceRecord record = new AttendanceRecord();
            record.setStudent(student);
            record.setClassSession(session);
            record.setStudentLatitude(request.getLatitude());
            record.setStudentLongitude(request.getLongitude());
            record.setDistanceMeters(BigDecimal.valueOf(distance));
            record.setDeviceUid(request.getDeviceId());
            record.setFaceDataUrl(imageUrl); // Lưu URL Cloudinary thay vì Base64
            record.setStatus(AttendanceRecord.AttendanceStatus.SUCCESS);
            // ===== STORE-AND-FORWARD: Sử dụng timestamp từ client (thời điểm quét thực tế) =====
            LocalDateTime checkedInTime = request.getTimestamp() != null 
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(request.getTimestamp()), VIETNAM_ZONE)
                : nowLocal;
            record.setCheckedInAt(checkedInTime);

            record = attendanceRecordRepository.save(record);

            return AttendanceCheckInResponse.success(
                    " Điểm danh thành công! Khoảng cách: " + String.format("%.2fm", distance),
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
