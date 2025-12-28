package com.dinhkhang.code.exception;

public class QRExpiredException extends AttendanceException {
    public QRExpiredException() {
        super("QR Code đã hết hạn. Vui lòng tạo mã mới.");
    }
}

