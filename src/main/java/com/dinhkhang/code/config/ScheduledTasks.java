package com.dinhkhang.code.config;

import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.repository.QRSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled tasks for system maintenance
 * - Auto-deactivate expired QR sessions
 * - Clean up old attendance records (optional)
 */
@Component
public class ScheduledTasks {

    @Autowired
    private QRSessionRepository qrSessionRepository;

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @PreDestroy
    public void onShutdown() {
        isShuttingDown.set(true);
        System.out.println("[SCHEDULED TASKS] Shutdown initiated, stopping scheduled tasks...");
    }

    /**
     * Run every minute to deactivate expired QR sessions
     * Đảm bảo QR hết hạn không thể dùng để điểm danh
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void deactivateExpiredQRSessions() {
        if (isShuttingDown.get()) {
            return; // Skip execution during shutdown
        }

        try {
            LocalDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();

            List<QRSession> allActive = qrSessionRepository.findAll().stream()
                    .filter(qr -> qr.getIsActive() && qr.getExpiredAt().isBefore(now))
                    .toList();

            if (!allActive.isEmpty()) {
                allActive.forEach(qr -> qr.setIsActive(false));
                qrSessionRepository.saveAll(allActive);

                System.out.println("[SCHEDULED] Deactivated " + allActive.size() + " expired QR sessions");
            }
        } catch (Exception e) {
            if (!isShuttingDown.get()) {
                System.err.println("[SCHEDULED] Error deactivating QR sessions: " + e.getMessage());
            }
        }
    }

    /**
     * Run every 5 minutes to log system health
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void logSystemHealth() {
        if (isShuttingDown.get()) {
            return; // Skip execution during shutdown
        }

        try {
            long activeQRCount = qrSessionRepository.findAll().stream()
                    .filter(qr -> qr.getIsActive() && !qr.isExpired())
                    .count();

            System.out.println("[HEALTH CHECK] Active QR Sessions: " + activeQRCount);
        } catch (Exception e) {
            if (!isShuttingDown.get()) {
                System.err.println("[HEALTH CHECK] Error: " + e.getMessage());
            }
        }
    }
}
