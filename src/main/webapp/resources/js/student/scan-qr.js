/* Student QR Scanner Script */
let html5QrCode;
let videoStream;

// Initialize camera for selfie
async function initCamera() {
  try {
    videoStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user" },
    });
    document.getElementById("preview").srcObject = videoStream;
  } catch (error) {
    console.error("Error accessing camera:", error);
    showResult(
      "Không thể truy cập camera. Vui lòng cho phép truy cập camera!",
      "danger"
    );
  }
}

// Capture selfie
function captureSelfie() {
  const video = document.getElementById("preview");
  const canvas = document.getElementById("canvas");
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  canvas.getContext("2d").drawImage(video, 0, 0);
  return canvas.toDataURL("image/jpeg");
}

// Initialize QR Scanner
function initQRScanner() {
  html5QrCode = new Html5Qrcode("reader");

  Html5Qrcode.getCameras()
    .then((cameras) => {
      if (cameras && cameras.length) {
        // Use back camera if available
        const cameraId = cameras.length > 1 ? cameras[1].id : cameras[0].id;

        html5QrCode
          .start(
            cameraId,
            {
              fps: 10,
              qrbox: { width: 250, height: 250 },
            },
            onScanSuccess,
            onScanFailure
          )
          .catch((err) => {
            console.error("Unable to start scanner:", err);
          });
      }
    })
    .catch((err) => {
      console.error("Error getting cameras:", err);
    });
}

function onScanSuccess(decodedText, decodedResult) {
  // Parse QR content: JSON format {"sessionId":1,"token":"xxx"}
  let qrData;
  try {
    qrData = JSON.parse(decodedText);
    if (!qrData.sessionId || !qrData.token) {
      showResult("Mã QR không hợp lệ! Thiếu sessionId hoặc token", "danger");
      return;
    }
  } catch (e) {
    showResult("Mã QR không hợp lệ! Format không đúng", "danger");
    return;
  }

  const qrId = qrData.sessionId;
  const tokenSecret = qrData.token;

  // Stop scanning
  html5QrCode.stop();

  // Get location
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        performCheckIn(
          qrId,
          tokenSecret,
          position.coords.latitude,
          position.coords.longitude
        );
      },
      function (error) {
        showResult("Không thể lấy vị trí GPS. Vui lòng bật định vị!", "danger");
      }
    );
  } else {
    showResult("Trình duyệt không hỗ trợ Geolocation!", "danger");
  }
}

function onScanFailure(error) {
  // Ignore scan failures
}

function performCheckIn(qrId, tokenSecret, latitude, longitude) {
  showResult("Đang xử lý điểm danh...", "info");

  // Capture selfie
  const imageData = captureSelfie();

  // Get device fingerprint (simple version)
  const deviceUid =
    navigator.userAgent + "|" + screen.width + "x" + screen.height;

  // Get context path dynamically
  const contextPath = window.location.pathname.split("/")[1];
  const apiUrl = `/${contextPath}/api/attendance/checkin`;

  $.ajax({
    url: apiUrl,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify({
      sessionId: qrId, // Changed from qrId to sessionId
      tokenSecret: tokenSecret,
      studentLat: latitude,
      studentLong: longitude,
      deviceUid: deviceUid,
      imageData: imageData,
    }),
    success: function (response) {
      if (response.success) {
        showResult(
          "Điểm danh thành công! Khoảng cách: " +
            response.distance.toFixed(2) +
            "m",
          "success"
        );
        setTimeout(function () {
          const contextPath = window.location.pathname.split("/")[1];
          window.location.href = `/${contextPath}/student/dashboard`;
        }, 2000);
      } else {
        showResult("Điểm danh thất bại: " + response.message, "danger");
        setTimeout(function () {
          initQRScanner();
        }, 3000);
      }
    },
    error: function (xhr) {
      const response = xhr.responseJSON;
      showResult(
        "Lỗi: " + (response ? response.message : "Không thể kết nối server"),
        "danger"
      );
      setTimeout(function () {
        initQRScanner();
      }, 3000);
    },
  });
}

function showResult(message, type) {
  const resultDiv = $("#result");
  resultDiv.removeClass("alert-success alert-danger alert-info alert-warning");
  resultDiv.addClass("alert-" + type);
  resultDiv.html(
    '<i class="fas fa-' +
      (type === "success"
        ? "check-circle"
        : type === "danger"
        ? "exclamation-circle"
        : "info-circle") +
      '"></i> ' +
      message
  );
  resultDiv.show();
}

$(document).ready(function () {
  initCamera();
  initQRScanner();
});
