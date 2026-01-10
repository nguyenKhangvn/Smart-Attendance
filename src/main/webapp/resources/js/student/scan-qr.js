/* Student QR Scanner Script */
let html5QrCode;
let videoStream;

// ===== CẢI TIẾN 1: DEVICE FINGERPRINT TƯƠNG TỰ UUID =====
function getOrCreateDeviceUUID() {
  let deviceUUID = localStorage.getItem("device_uuid");
  if (!deviceUUID) {
    // Tạo UUID mới dựa trên thông tin thiết bị + random
    deviceUUID =
      "device-" +
      Date.now() +
      "-" +
      Math.random().toString(36).substring(2, 15) +
      "-" +
      navigator.userAgent.substring(0, 20).replace(/\s/g, "");
    localStorage.setItem("device_uuid", deviceUUID);
    console.log("✅ Created new Device UUID:", deviceUUID);
  }
  return deviceUUID;
}

// ===== CẢI TIẾN 2: NÉN ẢNH TRƯỚC KHI UPLOAD =====
function compressImage(base64Image, maxWidth = 640, quality = 0.7) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = function () {
      const canvas = document.createElement("canvas");
      let width = img.width;
      let height = img.height;

      // Resize nếu ảnh quá lớn
      if (width > maxWidth) {
        height = (height * maxWidth) / width;
        width = maxWidth;
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, width, height);

      // Nén với quality 0.7 (70%)
      const compressed = canvas.toDataURL("image/jpeg", quality);
      console.log(
        "✅ Image compressed:",
        (base64Image.length / 1024).toFixed(1) + "KB ->",
        (compressed.length / 1024).toFixed(1) + "KB"
      );
      resolve(compressed);
    };
    img.src = base64Image;
  });
}

// Initialize camera for selfie
async function initCamera() {
  try {
    videoStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user" },
    });
    document.getElementById("preview").srcObject = videoStream;
  } catch (error) {
    console.error("Error accessing camera:", error);

    // ===== CẢI TIẾN 3: XỬ LÝ LỖI CAMERA (iOS Safari) =====
    let errorMessage = "Không thể truy cập camera. ";
    if (error.name === "NotAllowedError") {
      errorMessage += "Vui lòng cho phép truy cập camera trong cài đặt!";
    } else if (error.name === "NotFoundError") {
      errorMessage += "Không tìm thấy camera trên thiết bị!";
    } else if (error.name === "NotSupportedError") {
      errorMessage +=
        "Trình duyệt không hỗ trợ camera. Vui lòng sử dụng HTTPS!";
    } else {
      errorMessage += error.message;
    }

    showResult(errorMessage, "danger");
  }
}

// Capture selfie
function captureSelfie() {
  const video = document.getElementById("preview");
  const canvas = document.getElementById("canvas");

  // ✅ KIỂM TRA VIDEO ĐÃ SẴN SÀNG
  if (!video.videoWidth || !video.videoHeight) {
    console.error(
      "❌ Video not ready:",
      video.videoWidth,
      "x",
      video.videoHeight
    );
    throw new Error("Camera chưa sẵn sàng. Vui lòng chờ và thử lại!");
  }

  console.log(
    "📸 Capturing selfie from video:",
    video.videoWidth,
    "x",
    video.videoHeight
  );

  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;

  const ctx = canvas.getContext("2d");
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  const imageData = canvas.toDataURL("image/jpeg", 0.9);
  console.log(
    "✅ Selfie captured:",
    (imageData.length / 1024).toFixed(1) + "KB"
  );

  return imageData;
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
          position.coords.longitude,
          position.coords.accuracy // ===== CẢI TIẾN 4: Lấy GPS accuracy =====
        );
      },
      function (error) {
        showResult("Không thể lấy vị trí GPS. Vui lòng bật định vị!", "danger");
      },
      {
        enableHighAccuracy: true, // Yêu cầu GPS chính xác cao
        timeout: 10000, // Timeout 10s
        maximumAge: 0, // Không dùng cache
      }
    );
  } else {
    showResult("Trình duyệt không hỗ trợ Geolocation!", "danger");
  }
}

function onScanFailure(error) {
  // Ignore scan failures
}

async function performCheckIn(
  qrId,
  tokenSecret,
  latitude,
  longitude,
  gpsAccuracy
) {
  showResult("Đang xử lý điểm danh...", "info");

  // Capture selfie
  const imageData = captureSelfie();

  // ===== CẢI TIẾN 5: NÉN ẢNH TRƯỚC KHI GỬI =====
  const compressedImage = await compressImage(imageData, 640, 0.7);

  // ===== CẢI TIẾN 6: DEVICE UUID THAY VÌ FINGERPRINT ĐƠN GIẢN =====
  const deviceUid = getOrCreateDeviceUUID();

  // ===== CẢI TIẾN 7: LẤY PUBLIC IP (từ API bên ngoài) =====
  let clientIp = null;
  try {
    const ipResponse = await fetch("https://api.ipify.org?format=json");
    const ipData = await ipResponse.json();
    clientIp = ipData.ip;
    console.log("📍 Client Public IP:", clientIp);
  } catch (error) {
    console.warn("Cannot get public IP:", error);
    // Nếu không lấy được IP, backend sẽ lấy từ request header
  }

  // ===== STORE-AND-FORWARD: Tạo payload với timestamp =====
  const payload = {
    sessionId: qrId,
    token: tokenSecret,
    latitude: latitude,
    longitude: longitude,
    gpsAccuracy: gpsAccuracy,
    clientIp: clientIp,
    deviceId: deviceUid,
    selfieBase64: compressedImage,
    timestamp: Date.now(), // ===== QUAN TRỌNG: Thời điểm QUÉT (không phải thời điểm gửi) =====
  };

  // ===== STORE-AND-FORWARD: Kiểm tra mạng trước khi gửi =====
  if (navigator.onLine) {
    sendCheckInRequest(payload);
  } else {
    saveOffline(payload);
  }
}

// ===== STORE-AND-FORWARD: Hàm gửi request điểm danh =====
function sendCheckInRequest(payload) {
  const contextPath = window.location.pathname.split("/")[1];
  const apiUrl = `/${contextPath}/api/v2/attendance/check-in`;

  $.ajax({
    url: apiUrl,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload),
    success: function (response) {
      // Xóa offline data nếu gửi thành công
      localStorage.removeItem("offline_attendance");

      if (response.success) {
        showResult(
          "✅ Điểm danh thành công! Khoảng cách: " +
            response.distance.toFixed(2) +
            "m",
          "success"
        );
        setTimeout(function () {
          const contextPath = window.location.pathname.split("/")[1];
          window.location.href = `/${contextPath}/student/dashboard`;
        }, 2000);
      } else {
        showResult("❌ Điểm danh thất bại: " + response.message, "danger");
        setTimeout(function () {
          initQRScanner();
        }, 3000);
      }
    },
    error: function (xhr) {
      const response = xhr.responseJSON;

      // ===== DEBUG: Log chi tiết lỗi =====
      console.error("❌ Request failed:");
      console.error("   Status:", xhr.status);
      console.error("   Status Text:", xhr.statusText);
      console.error("   Response:", response);
      console.error("   Navigator Online:", navigator.onLine);

      // ===== STORE-AND-FORWARD: CHỈ lưu offline khi THỰC SỰ mất mạng =====
      // Status 0 = Network error (không có response từ server)
      // Status >= 500 = Server error (server có vấn đề)
      if (xhr.status === 0) {
        console.warn(
          "⚠️ Network error detected (status 0) - Checking connection..."
        );

        // Kiểm tra kỹ: Có thể do CORS, timeout, hoặc thực sự mất mạng
        if (!navigator.onLine) {
          console.log("📴 Offline confirmed - Saving data");
          saveOffline(payload);
          return;
        } else {
          // Có mạng nhưng status 0 -> Có thể do CORS, URL sai, hoặc server chưa chạy
          showResult(
            "❌ Không thể kết nối server. Vui lòng kiểm tra:<br>" +
              "- Server có đang chạy không?<br>" +
              "- URL API có đúng không?<br>" +
              "- CORS có được cấu hình không?",
            "danger"
          );
        }
      } else if (xhr.status >= 500) {
        // Server error - Không lưu offline vì đây là lỗi backend logic
        showResult(
          "❌ Lỗi server (500): " +
            (response ? response.message : "Server đang gặp sự cố"),
          "danger"
        );
      } else if (xhr.status === 429) {
        // ===== XỬ LÝ RATE LIMITING =====
        showResult(
          "⚠️ " +
            (response ? response.message : "Thao tác quá nhanh. Vui lòng chờ."),
          "warning"
        );
      } else if (xhr.status === 401 || xhr.status === 403) {
        // Unauthorized - Redirect to login
        showResult(
          "❌ Phiên đăng nhập hết hạn. Đang chuyển về trang đăng nhập...",
          "danger"
        );
        setTimeout(function () {
          const contextPath = window.location.pathname.split("/")[1];
          window.location.href = `/${contextPath}/login`;
        }, 2000);
        return;
      } else {
        // Client error (400, 404...) hoặc lỗi khác
        showResult(
          "❌ Lỗi: " + (response ? response.message : "Yêu cầu không hợp lệ"),
          "danger"
        );
      }

      setTimeout(function () {
        initQRScanner();
      }, 3000);
    },
  });
}

// ===== STORE-AND-FORWARD: Lưu dữ liệu offline =====
function saveOffline(payload) {
  localStorage.setItem("offline_attendance", JSON.stringify(payload));
  showResult(
    "⚠️ Mất kết nối! Dữ liệu đã được lưu. Hệ thống sẽ tự động đồng bộ khi có mạng.",
    "warning"
  );
  console.log("💾 Saved offline data:", payload);

  // Đăng ký sự kiện: Khi có mạng lại thì tự gửi
  window.addEventListener("online", syncOfflineData);

  // Thử đồng bộ lại sau 5 giây (trường hợp mạng chập chờn)
  setTimeout(syncOfflineData, 5000);
}

// ===== STORE-AND-FORWARD: Đồng bộ dữ liệu khi có mạng =====
function syncOfflineData() {
  const savedData = localStorage.getItem("offline_attendance");
  if (savedData && navigator.onLine) {
    console.log("🔄 Có mạng trở lại. Đang đồng bộ dữ liệu...");
    const payload = JSON.parse(savedData);
    showResult("🔄 Đang đồng bộ dữ liệu điểm danh...", "info");
    sendCheckInRequest(payload);
  }
}

// ===== Kiểm tra và đồng bộ dữ liệu offline khi load trang =====
$(document).ready(function () {
  // Thử đồng bộ dữ liệu offline nếu có
  syncOfflineData();
});

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
