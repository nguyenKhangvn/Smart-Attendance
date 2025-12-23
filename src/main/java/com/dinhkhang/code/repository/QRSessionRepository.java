package com.dinhkhang.code.repository;

import com.dinhkhang.code.entity.ClassSession;
import com.dinhkhang.code.entity.QRSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QRSessionRepository extends JpaRepository<QRSession, Long> {

    Optional<QRSession> findByTokenSecret(String tokenSecret);

    List<QRSession> findByClassSession(ClassSession classSession);

    @Query("SELECT q FROM QRSession q WHERE q.classSession = :classSession " +
            "AND q.isActive = true AND q.expiredAt > :now " +
            "ORDER BY q.createdAt DESC")
    List<QRSession> findActiveQRSessionsByClassSession(
            @Param("classSession") ClassSession classSession,
            @Param("now") LocalDateTime now);

    @Query("SELECT q FROM QRSession q WHERE q.tokenSecret = :tokenSecret " +
            "AND q.isActive = true AND q.expiredAt > :now")
    Optional<QRSession> findValidQRSession(
            @Param("tokenSecret") String tokenSecret,
            @Param("now") LocalDateTime now);
}
