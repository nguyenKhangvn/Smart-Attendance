package com.dinhkhang.code.service;

import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.entity.QRSession;

public interface IQRService {
    QRSessionDTO generateQRSession(Long sessionId, Double teacherLat, Double teacherLong,
            Integer expirationMinutes, Integer maxDistanceMeters);

    QRSession validateQRSession(Long qrId, String tokenSecret);
    
    QRSession validateQRSessionBySessionIdAndToken(Long sessionId, String tokenSecret);
}
