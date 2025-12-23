package com.dinhkhang.code.dto;

public class QRSessionDTO {

    private Long qrId;
    private String tokenSecret;
    private Long sessionId;
    private String sessionName;
    private String className;
    private Integer expiresInSeconds;
    private String qrCodeBase64;

    // Constructors
    public QRSessionDTO() {
    }

    public QRSessionDTO(Long qrId, String tokenSecret, Long sessionId, String sessionName,
            String className, Integer expiresInSeconds, String qrCodeBase64) {
        this.qrId = qrId;
        this.tokenSecret = tokenSecret;
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.className = className;
        this.expiresInSeconds = expiresInSeconds;
        this.qrCodeBase64 = qrCodeBase64;
    }

    // Getters and Setters
    public Long getQrId() {
        return qrId;
    }

    public void setQrId(Long qrId) {
        this.qrId = qrId;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(Integer expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }
}
