/* Teacher QR Scanner Script */

// Get sessionId from inline script (will be set by Thymeleaf)
// const sessionId is set in the HTML file by Thymeleaf

let timerInterval;
let expirationTime;
let autoRefreshInterval; // ===== CẢI TIẾN: AUTO-REFRESH QR =====
const QR_REFRESH_INTERVAL = 10; // seconds - Dynamic QR tự động thay đổi mỗi 10 giây

function generateQRCode() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;

        // Get context path dynamically
        const contextPath = window.location.pathname.split("/")[1];
        const apiUrl = `/${contextPath}/api/qr/generate`;

        $.ajax({
          url: apiUrl,
          method: "POST",
          data: {
            sessionId: sessionId,
            latitude: latitude,
            longitude: longitude,
            expirationMinutes: 5,
            maxDistanceMeters: 50,
          },
          success: function (response) {
            $("#qrCode").html(
              '<img src="' +
                response.qrCodeBase64 +
                '" class="img-fluid" alt="QR Code">'
            );

            expirationTime = response.expiresInSeconds;
            startTimer(expirationTime);
          },
          error: function (error) {
            alert("Không thể tạo mã QR: " + error.responseText);
          },
        });
      },
      function (error) {
        alert("Không thể lấy vị trí GPS. Vui lòng bật định vị!");
      }
    );
  } else {
    alert("Trình duyệt không hỗ trợ Geolocation!");
  }
}

// ===== CẢI TIẾN: DYNAMIC QR - TỰ ĐỘNG REFRESH MỖI 10 GIÂY =====
function startAutoRefresh() {
  // Xóa interval cũ nếu có
  if (autoRefreshInterval) {
    clearInterval(autoRefreshInterval);
  }

  // Tạo QR mới mỗi 10 giây
  autoRefreshInterval = setInterval(function () {
    console.log("🔄 Auto-refreshing QR code...");
    generateQRCode();
  }, QR_REFRESH_INTERVAL * 1000);

  console.log(
    " Dynamic QR enabled: Refresh every " + QR_REFRESH_INTERVAL + "s"
  );
}

function stopAutoRefresh() {
  if (autoRefreshInterval) {
    clearInterval(autoRefreshInterval);
    console.log("⏹️ Dynamic QR stopped");
  }
}

function startTimer(seconds) {
  clearInterval(timerInterval);

  timerInterval = setInterval(function () {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;

    $("#timer").text(
      String(minutes).padStart(2, "0") + ":" + String(secs).padStart(2, "0")
    );

    if (seconds <= 60) {
      $("#timer").removeClass("warning").addClass("danger");
    } else if (seconds <= 120) {
      $("#timer").addClass("warning").removeClass("danger");
    }

    if (seconds <= 0) {
      clearInterval(timerInterval);
      stopAutoRefresh(); // Dừng auto-refresh khi hết thời gian
      $("#qrCode").html(
        '<p class="text-danger">Mã QR đã hết hạn. Vui lòng tạo mã mới!</p>'
      );
    }

    seconds--;
  }, 1000);
}

$(document).ready(function () {
  generateQRCode();

  // ===== BẬT DYNAMIC QR AUTO-REFRESH =====
  startAutoRefresh();

  $("#regenerateBtn").click(function () {
    generateQRCode();
    // Restart auto-refresh
    startAutoRefresh();
  });
});
