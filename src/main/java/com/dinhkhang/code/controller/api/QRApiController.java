package com.dinhkhang.code.controller.api;

import com.dinhkhang.code.dto.QRSessionDTO;
import com.dinhkhang.code.service.IQRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QRApiController {

    @Autowired
    private IQRService qrService;

    @PostMapping("/generate")
    public ResponseEntity<QRSessionDTO> generateQR(
            @RequestParam Long sessionId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "5") Integer expirationMinutes,
            @RequestParam(required = false, defaultValue = "50") Integer maxDistanceMeters) {

        QRSessionDTO qrSession = qrService.generateQRSession(
                sessionId, latitude, longitude, expirationMinutes, maxDistanceMeters);

        return ResponseEntity.ok(qrSession);
    }
}
