package com.dinhkhang.code.config;

import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.repository.QRSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for system maintenance
 * - Auto-deactivate expired QR sessions
 * - Clean up old attendance records (optional)
 */
@Component
public class ScheduledTasks {

    @Autowired
    private QRSessionRepository qrSessionRepository;

    /**
     * Run every minute to deactivate expired QR sessions
     * Đảm bảo QR hết hạn không thể dùng để điểm danh
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void deactivateExpiredQRSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<QRSession> allActive = qrSessionRepository.findAll().stream()
                .filter(qr -> qr.getIsActive() && qr.getExpiredAt().isBefore(now))
                .toList();

        if (!allActive.isEmpty()) {
            allActive.forEach(qr -> qr.setIsActive(false));
            qrSessionRepository.saveAll(allActive);

            System.out.println("[SCHEDULED] Deactivated " + allActive.size() + " expired QR sessions");
        }
    }

    /**
     * Run every 5 minutes to log system health
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logSystemHealth() {
        long activeQRCount = qrSessionRepository.findAll().stream()
                .filter(qr -> qr.getIsActive() && !qr.isExpired())
                .count();

        System.out.println("[HEALTH CHECK] Active QR Sessions: " + activeQRCount);
    }
}

