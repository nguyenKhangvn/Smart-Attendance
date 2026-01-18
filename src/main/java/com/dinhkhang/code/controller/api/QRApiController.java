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
            @RequestParam BigDecimal longitude) {

        // Hardcode giá trị bảo mật ở server, không nhận từ client
        double expirationMinutes = 0.35; // ~20 giây
        int maxDistanceMeters = 50; // 50 mét

        QRSessionDTO qrSession = qrService.generateQRSession(
                sessionId, latitude, longitude, expirationMinutes, maxDistanceMeters);

        return ResponseEntity.ok(qrSession);
    }
}