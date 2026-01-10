package com.dinhkhang.code.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.repository.ClassSessionRepository;
import com.dinhkhang.code.repository.QRSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class QRService implements IQRService {
    // ===== TIMEZONE: LUÔN SỬ DỤNG GIỜ VIỆT NAM (UTC+7) =====
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Autowired
    private QRSessionRepository qrSessionRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    public QRSessionDTO generateQRSession(Long sessionId, BigDecimal teacherLat, BigDecimal teacherLong,
            Integer expirationMinutes, Integer maxDistanceMeters) {
        ClassSession classSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Class session not found"));

        // ===== TỰ ĐỘNG BẮT ĐẦU SESSION KHI TẠO QR =====
        // Nếu session chưa bắt đầu, chuyển sang IN_PROGRESS
        if (!classSession.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS)) {
            classSession.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
            classSession.setUpdatedAt(LocalDateTime.now());
            classSession = classSessionRepository.save(classSession);
            System.out.println("✅ Session " + sessionId + " started automatically (status: IN_PROGRESS)");
        }

        // Generate unique token BẢO MẬT - 32 characters
        String tokenSecret = java.util.UUID.randomUUID().toString().replace("-", "");

        // Create QR session
        QRSession qrSession = new QRSession();
        qrSession.setTokenSecret(tokenSecret);
        qrSession.setTeacherLatitude(teacherLat);
        qrSession.setTeacherLongitude(teacherLong);
        qrSession.setClassSession(classSession);
        qrSession.setExpiredAt(ZonedDateTime.now(VIETNAM_ZONE)
                .plusMinutes(expirationMinutes != null ? expirationMinutes : 5).toLocalDateTime());
        qrSession.setMaxDistanceMeters(maxDistanceMeters != null ? maxDistanceMeters : 50);

        qrSession = qrSessionRepository.save(qrSession);

        // QR CHỈ CHỨA: sessionId + token (KHÔNG chứa thông tin sinh viên)
        String qrContent = String.format("{\"sessionId\":%d,\"token\":\"%s\"}",
                qrSession.getClassSession().getId(),
                tokenSecret);

        String qrCodeBase64 = generateQRCodeImage(qrContent);

        // Calculate expiration time
        ZonedDateTime nowVN = ZonedDateTime.now(VIETNAM_ZONE);
        int expiresInSeconds = (int) Duration.between(nowVN, ZonedDateTime.of(qrSession.getExpiredAt(), VIETNAM_ZONE))
                .getSeconds();

        return new QRSessionDTO(
                qrSession.getId(),
                tokenSecret,
                classSession.getId(),
                classSession.getSessionName(),
                classSession.getClassEntity().getSubjectName(),
                expiresInSeconds,
                qrCodeBase64);
    }

    private String generateQRCodeImage(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public QRSession validateQRSession(Long qrId, String tokenSecret) {
        QRSession qrSession = qrSessionRepository.findById(qrId)
                .orElseThrow(() -> new RuntimeException("QR session not found"));

        if (!qrSession.getTokenSecret().equals(tokenSecret)) {
            throw new RuntimeException("Invalid token secret");
        }

        if (!qrSession.isValid()) {
            throw new RuntimeException("QR session expired or inactive");
        }

        return qrSession;
    }

    /**
     * Validate QR Session by ClassSession ID and Token (NEW METHOD)
     * This is used when QR code contains sessionId instead of qrSessionId
     */
    public QRSession validateQRSessionBySessionIdAndToken(Long sessionId, String tokenSecret) {
        QRSession qrSession = qrSessionRepository
                .findValidQRSessionBySessionIdAndToken(sessionId, tokenSecret,
                        ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime())
                .orElseThrow(() -> new RuntimeException("QR code không hợp lệ hoặc đã hết hạn"));

        return qrSession;
    }
}
