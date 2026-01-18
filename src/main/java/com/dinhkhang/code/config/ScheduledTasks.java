package com.dinhkhang.code.config;

import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.QRSession;
import com.dinhkhang.code.repository.QRSessionRepository;
import com.dinhkhang.code.repository.ClassSessionRepository;
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

    /**
     * Tự động chuyển trạng thái buổi học sang COMPLETED nếu đã hết thời gian
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void completeFinishedSessions() {
        try {
            LocalDateTime now = java.time.ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
            // Lấy các session đang IN_PROGRESS
            List<ClassSession> inProgressSessions = classSessionRepository
                    .findByStatus(ClassSession.SessionStatus.IN_PROGRESS);
            for (ClassSession session : inProgressSessions) {
                LocalDateTime endTime = session.getSessionDate();
                if (session.getDurationMinutes() != null) {
                    endTime = endTime.plusMinutes(session.getDurationMinutes());
                }
                // Nếu đã hết thời gian thì chuyển sang COMPLETED
                if (endTime.isBefore(now)) {
                    session.setStatus(ClassSession.SessionStatus.COMPLETED);
                }
            }
            classSessionRepository.saveAll(inProgressSessions);
        } catch (Exception e) {
            System.err.println("[SCHEDULED] Error completing sessions: " + e.getMessage());
        }
    }

    @Autowired

    private QRSessionRepository qrSessionRepository;
    @Autowired
    private ClassSessionRepository classSessionRepository;

    // Định nghĩa múi giờ chuẩn 1 lần dùng chung
    private static final java.time.ZoneId VIETNAM_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Run every minute to deactivate expired QR sessions
     * Đảm bảo QR hết hạn không thể dùng để điểm danh
     */
    /**
     * TỐI ƯU: Chỉ lấy những bản ghi cần update
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deactivateExpiredQRSessions() {
        try {
            LocalDateTime now = java.time.ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();

            // CHỈ lấy các bản ghi active mà đã hết hạn (WHERE is_active=true AND expired_at
            // < now)
            // Hiệu năng: Cực nhanh, tốn ít RAM
            List<QRSession> expiredSessions = qrSessionRepository.findByIsActiveTrueAndExpiredAtBefore(now);

            if (!expiredSessions.isEmpty()) {
                expiredSessions.forEach(qr -> qr.setIsActive(false));
                qrSessionRepository.saveAll(expiredSessions);
                System.out.println("[SCHEDULED] Deactivated " + expiredSessions.size() + " expired QR sessions");
            }
        } catch (Exception e) {
            System.err.println("[SCHEDULED] Error: " + e.getMessage());
        }
    }

    /**
     * Run every 5 minutes to log system health
     */
    /**
     * TỐI ƯU: Dùng count() của Database thay vì tải list về đếm
     * Hàm này cũng đóng vai trò "Keep-Alive" giữ kết nối Supabase
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    public void logSystemHealth() {
        try {
            LocalDateTime now = java.time.ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
            // Query: SELECT count(*) FROM ... -> Trả về 1 con số duy nhất. Cực nhẹ.
            long activeQRCount = qrSessionRepository.countByIsActiveTrueAndExpiredAtAfter(now);
            System.out.println("[HEALTH CHECK] Active QR Sessions: " + activeQRCount + " | System Alive");
        } catch (Exception e) {
            System.err.println("[HEALTH CHECK] DB Connection Error: " + e.getMessage());
        }
    }

    /**
     * Tự động chuyển trạng thái IN_PROGRESS trước 15 phút
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void activateSessions() {
        try {
            LocalDateTime now = java.time.ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
            LocalDateTime threshold = now.plusMinutes(15);

            // Chỉ lấy các session trạng thái SCHEDULED và sắp diễn ra
            List<ClassSession> pendingSessions = classSessionRepository
                    .findByStatusAndSessionDateBefore(ClassSession.SessionStatus.SCHEDULED, threshold);

            if (!pendingSessions.isEmpty()) {
                pendingSessions.forEach(s -> s.setStatus(ClassSession.SessionStatus.IN_PROGRESS));
                classSessionRepository.saveAll(pendingSessions);
                System.out.println("[SCHEDULED] Auto-started " + pendingSessions.size() + " sessions.");
            }
        } catch (Exception e) {
            System.err.println("[SCHEDULED] Error activating sessions: " + e.getMessage());
        }
    }
}
