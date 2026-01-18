package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.repository.ClassSessionRepository;
import com.dinhkhang.code.repository.QRSessionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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

@Service
@Transactional
public class QRService implements IQRService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private QRSessionRepository qrSessionRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Override
    public QRSessionDTO generateQRSession(Long sessionId, BigDecimal teacherLat, BigDecimal teacherLong,
            double expirationMinutes, int maxDistanceMeters) {

        ClassSession classSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Class session not found"));

        // Auto-start session
        if (!classSession.getStatus().equals(ClassSession.SessionStatus.IN_PROGRESS)) {
            classSession.setStatus(ClassSession.SessionStatus.IN_PROGRESS);
            classSession.setUpdatedAt(LocalDateTime.now());
            classSessionRepository.save(classSession);
        }

        // Generate Token
        String tokenSecret = java.util.UUID.randomUUID().toString().replace("-", "");

        // Create QRSession
        QRSession qrSession = new QRSession();
        qrSession.setTokenSecret(tokenSecret);
        qrSession.setTeacherLatitude(teacherLat);
        qrSession.setTeacherLongitude(teacherLong);
        qrSession.setClassSession(classSession);

        // ===== LOGIC TÍNH THỜI GIAN MỚI (Hỗ trợ số lẻ 0.35 phút) =====

        // Đổi ra giây: expirationMinutes * 60
        long secondsToAdd = (long) (expirationMinutes * 60);

        // Cộng thời gian vào thời điểm hiện tại
        ZonedDateTime expiredTimeZoned = ZonedDateTime.now(VIETNAM_ZONE).plusSeconds(secondsToAdd);

        // Lưu vào Entity
        qrSession.setExpiredAt(expiredTimeZoned.toLocalDateTime());
        qrSession.setMaxDistanceMeters(maxDistanceMeters);

        qrSession = qrSessionRepository.save(qrSession);

        // QR Content: JSON
        String qrContent = String.format("{\"sessionId\":%d,\"token\":\"%s\"}",
                qrSession.getClassSession().getId(),
                tokenSecret);

        String qrCodeBase64 = generateQRCodeImage(qrContent);

        // Calculate expiration in seconds (for Frontend display)
        int expiresInSeconds = (int) Duration.between(ZonedDateTime.now(VIETNAM_ZONE), expiredTimeZoned).getSeconds();

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
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public QRSession validateQRSession(Long qrId, String tokenSecret) {
        QRSession qrSession = qrSessionRepository.findById(qrId)
                .orElseThrow(() -> new RuntimeException("QR session not found"));

        if (!qrSession.getTokenSecret().equals(tokenSecret)) {
            throw new RuntimeException("Invalid token secret");
        }

        // Kiểm tra hết hạn
        if (qrSession.getExpiredAt().isBefore(LocalDateTime.now(VIETNAM_ZONE))) {
            throw new RuntimeException("QR code đã hết hạn");
        }

        return qrSession;
    }

    @Override
    public QRSession validateQRSessionBySessionIdAndToken(Long sessionId, String tokenSecret) {
        return qrSessionRepository.findValidQRSessionBySessionIdAndToken(sessionId, tokenSecret,
                ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime())
                .orElseThrow(() -> new RuntimeException("QR code không hợp lệ hoặc đã hết hạn"));
    }
}