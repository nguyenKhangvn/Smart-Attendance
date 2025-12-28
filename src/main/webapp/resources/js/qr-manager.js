/**
 * QR Code Manager - Advanced JavaScript for Teacher Dashboard
 * Features:
 * - Countdown timer for QR expiration
 * - Auto-refresh QR every 5 minutes
 * - Real-time attendance statistics via polling
 * - Error handling and retry logic
 */

class QRManager {
    constructor(sessionId, containerId) {
        this.sessionId = sessionId;
        this.container = document.getElementById(containerId);
        this.qrData = null;
        this.countdownInterval = null;
        this.refreshTimeout = null;
        this.statsInterval = null;
    }

    /**
     * Initialize QR generation and start monitoring
     */
    async init() {
        await this.generateQR();
        this.startCountdown();
        this.scheduleAutoRefresh();
        this.startStatsPolling();
    }

    /**
     * Generate QR Code via API
     */
    async generateQR() {
        try {
            // Get teacher's GPS location
            const position = await this.getCurrentPosition();

            const contextPath = window.location.pathname.split('/')[1];
            const url = `/${contextPath}/api/qr/generate?sessionId=${this.sessionId}&latitude=${position.latitude}&longitude=${position.longitude}&expirationMinutes=5&maxDistanceMeters=50`;

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            this.qrData = await response.json();
            this.renderQR();

        } catch (error) {
            console.error('Error generating QR:', error);
            this.renderError(error.message);
        }
    }

    /**
     * Get current GPS position with timeout
     */
    getCurrentPosition() {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Trình duyệt không hỗ trợ GPS'));
                return;
            }

            const timeout = setTimeout(() => {
                reject(new Error('Timeout getting GPS location'));
            }, 10000);

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    clearTimeout(timeout);
                    resolve({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    });
                },
                (error) => {
                    clearTimeout(timeout);
                    // Fallback to default location (for testing)
                    console.warn('GPS error, using default location:', error);
                    resolve({
                        latitude: 10.762622,
                        longitude: 106.660172
                    });
                },
                { timeout: 10000, enableHighAccuracy: true }
            );
        });
    }

    /**
     * Render QR Code to container
     */
    renderQR() {
        if (!this.qrData) return;

        this.container.innerHTML = `
            <div class="qr-display">
                <img src="${this.qrData.qrCodeBase64}" alt="QR Code" class="img-fluid border p-3 shadow-sm" style="max-width: 350px;">
                <div class="mt-3">
                    <h5 class="text-primary">${this.qrData.sessionName}</h5>
                    <p class="text-muted mb-2">${this.qrData.className}</p>
                    <div class="countdown-container">
                        <div class="progress" style="height: 30px;">
                            <div id="countdown-bar-${this.sessionId}" class="progress-bar progress-bar-striped progress-bar-animated bg-success"
                                 role="progressbar" style="width: 100%;">
                                <strong id="countdown-text-${this.sessionId}">5:00</strong>
                            </div>
                        </div>
                        <small class="text-muted mt-2 d-block">
                            <i class="fas fa-clock"></i> QR sẽ tự động làm mới sau khi hết hạn
                        </small>
                    </div>
                </div>
                <div id="stats-${this.sessionId}" class="mt-3 alert alert-info">
                    <i class="fas fa-spinner fa-spin"></i> Đang tải thống kê...
                </div>
            </div>
        `;

        // Update stats immediately
        this.updateStats();
    }

    /**
     * Render error message
     */
    renderError(message) {
        this.container.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle"></i>
                <strong>Lỗi tạo QR Code:</strong> ${message}
                <button class="btn btn-sm btn-primary mt-2" onclick="location.reload()">
                    <i class="fas fa-redo"></i> Thử lại
                </button>
            </div>
        `;
    }

    /**
     * Start countdown timer
     */
    startCountdown() {
        if (!this.qrData) return;

        let remainingSeconds = this.qrData.expiresInSeconds;
        const totalSeconds = this.qrData.expiresInSeconds;

        this.countdownInterval = setInterval(() => {
            remainingSeconds--;

            if (remainingSeconds <= 0) {
                clearInterval(this.countdownInterval);
                this.onQRExpired();
                return;
            }

            // Update UI
            const minutes = Math.floor(remainingSeconds / 60);
            const seconds = remainingSeconds % 60;
            const timeText = `${minutes}:${seconds.toString().padStart(2, '0')}`;

            const textElement = document.getElementById(`countdown-text-${this.sessionId}`);
            const barElement = document.getElementById(`countdown-bar-${this.sessionId}`);

            if (textElement) textElement.textContent = timeText;

            if (barElement) {
                const percentage = (remainingSeconds / totalSeconds) * 100;
                barElement.style.width = percentage + '%';

                // Change color based on remaining time
                if (percentage < 20) {
                    barElement.classList.remove('bg-success', 'bg-warning');
                    barElement.classList.add('bg-danger');
                } else if (percentage < 50) {
                    barElement.classList.remove('bg-success', 'bg-danger');
                    barElement.classList.add('bg-warning');
                }
            }
        }, 1000);
    }

    /**
     * Handle QR expiration
     */
    onQRExpired() {
        console.log('[QR Manager] QR expired, refreshing...');
        this.container.innerHTML = `
            <div class="alert alert-warning">
                <i class="fas fa-hourglass-end"></i> QR đã hết hạn. Đang tạo mã mới...
            </div>
        `;

        // Generate new QR immediately
        setTimeout(() => this.init(), 2000);
    }

    /**
     * Schedule auto-refresh (backup mechanism)
     */
    scheduleAutoRefresh() {
        // Refresh 10 seconds before expiration
        const refreshTime = (this.qrData?.expiresInSeconds || 300) * 1000 - 10000;

        this.refreshTimeout = setTimeout(() => {
            console.log('[QR Manager] Auto-refresh triggered');
            this.cleanup();
            this.init();
        }, refreshTime);
    }

    /**
     * Poll attendance statistics every 5 seconds
     */
    startStatsPolling() {
        this.updateStats(); // Initial update

        this.statsInterval = setInterval(() => {
            this.updateStats();
        }, 5000); // Update every 5 seconds
    }

    /**
     * Fetch and update attendance statistics
     */
    async updateStats() {
        try {
            const contextPath = window.location.pathname.split('/')[1];
            const response = await fetch(`/${contextPath}/api/attendance/session/${this.sessionId}`);

            if (!response.ok) throw new Error('Failed to fetch stats');

            const records = await response.json();

            const total = records.length;
            const success = records.filter(r => r.status === 'SUCCESS').length;
            const failed = records.filter(r => r.status !== 'SUCCESS' && r.status !== 'ABSENT').length;

            const statsElement = document.getElementById(`stats-${this.sessionId}`);
            if (statsElement) {
                statsElement.innerHTML = `
                    <div class="row text-center">
                        <div class="col-4">
                            <h3 class="text-success mb-0">${success}</h3>
                            <small>Đã điểm danh</small>
                        </div>
                        <div class="col-4">
                            <h3 class="text-danger mb-0">${failed}</h3>
                            <small>Thất bại</small>
                        </div>
                        <div class="col-4">
                            <h3 class="text-primary mb-0">${total}</h3>
                            <small>Tổng cộng</small>
                        </div>
                    </div>
                `;
            }
        } catch (error) {
            console.error('Error fetching stats:', error);
        }
    }

    /**
     * Cleanup timers and intervals
     */
    cleanup() {
        if (this.countdownInterval) clearInterval(this.countdownInterval);
        if (this.refreshTimeout) clearTimeout(this.refreshTimeout);
        if (this.statsInterval) clearInterval(this.statsInterval);
    }

    /**
     * Destroy the QR manager
     */
    destroy() {
        this.cleanup();
        this.container.innerHTML = '';
    }
}

// Export for global use
window.QRManager = QRManager;

