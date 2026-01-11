package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.entity.QRSession;

import java.math.BigDecimal;

public interface IQRService {
    QRSessionDTO generateQRSession(Long sessionId, BigDecimal teacherLat, BigDecimal teacherLong,
            Double expirationMinutes, Integer maxDistanceMeters);

    QRSession validateQRSession(Long qrId, String tokenSecret);

    QRSession validateQRSessionBySessionIdAndToken(Long sessionId, String tokenSecret);
}
