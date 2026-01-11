package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.service.IQRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/qr")
public class QRApiController {

    @Autowired
    private IQRService qrService;

    @PostMapping("/generate")
    public ResponseEntity<QRSessionDTO> generateQR(
            @RequestParam Long sessionId,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            // SỬA Ở ĐÂY: Dùng Double thay vì Integer để nhận 0.35 phút
            @RequestParam(required = false, defaultValue = "5.0") Double expirationMinutes,
            @RequestParam(required = false, defaultValue = "50") Integer maxDistanceMeters) {

        // Gọi Service (Bạn cũng phải sửa tham số trong Interface IQRService nhé)
        QRSessionDTO qrSession = qrService.generateQRSession(
                sessionId, latitude, longitude, expirationMinutes, maxDistanceMeters);

        return ResponseEntity.ok(qrSession);
    }
}