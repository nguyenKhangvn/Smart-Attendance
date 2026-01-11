/* Student QR Scanner Script - STRICT GPS VERSION */
let html5QrCode;
let videoStream;

// ===== HELPER: DEVICE UUID & IMAGE =====
function getOrCreateDeviceUUID() {
  let deviceUUID = localStorage.getItem("device_uuid");
  if (!deviceUUID) {
    deviceUUID =
      "device-" +
      Date.now() +
      "-" +
      Math.random().toString(36).substring(2, 15);
    localStorage.setItem("device_uuid", deviceUUID);
  }
  return deviceUUID;
}

function compressImage(base64Image, maxWidth = 640, quality = 0.7) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = function () {
      const canvas = document.createElement("canvas");
      let width = img.width;
      let height = img.height;
      if (width > maxWidth) {
        height = (height * maxWidth) / width;
        width = maxWidth;
      }
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, width, height);
      resolve(canvas.toDataURL("image/jpeg", quality));
    };
    img.src = base64Image;
  });
}

// ===== INIT CAMERA =====
async function initCamera() {
  try {
    videoStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user" },
    });
    const video = document.getElementById("preview");
    video.srcObject = videoStream;
    video.setAttribute("playsinline", true);
    await video.play();
  } catch (error) {
    console.error("Camera error:", error);
    throw error;
  }
}

// ===== CAPTURE SELFIE (FIXED FOR SLOW DEVICES) =====
async function captureSelfie() {
  const video = document.getElementById("preview");
  const canvas = document.getElementById("canvas");

  // FIX 1: Đảm bảo video đang chạy (quan trọng cho iOS)
  if (video.paused || video.ended) {
    try {
      await video.play();
    } catch (e) {
      console.warn(
        "Auto-play failed, waiting for user interaction/stream load..."
      );
    }
  }

  // FIX 2: Thay đổi logic chờ
  // Không chỉ chờ readyState, mà chờ cả kích thước video (width > 0)
  let waitCount = 0;
  const maxWait = 60; // Tăng lên 6s cho chắc chắn (60 * 100ms)

  // Điều kiện: readyState >= 2 (HAVE_CURRENT_DATA) VÀ width > 0
  while (
    (video.readyState < 2 || video.videoWidth === 0) &&
    waitCount < maxWait
  ) {
    await new Promise((resolve) => setTimeout(resolve, 100));
    waitCount++;

    // FIX 3: Nếu chờ được 1 nửa thời gian (3s) mà vẫn chưa lên, thử ép play lại
    if (waitCount === 30) {
      console.log("Camera slow start, forcing play...");
      video.play().catch(() => {});
    }
  }

  // Kiểm tra lần cuối
  if (video.videoWidth === 0 || video.videoHeight === 0) {
    // Fallback khẩn cấp: Nếu readyState ok mà size = 0, thử chờ thêm 500ms
    await new Promise((resolve) => setTimeout(resolve, 500));

    if (video.videoWidth === 0) {
      throw new Error(
        "Lỗi thiết bị: Camera không trả về hình ảnh (Width=0). Hãy thử reload hoặc đổi trình duyệt."
      );
    }
  }

  // Delay ổn định cảm biến sáng (giữ nguyên logic cũ của bạn vì nó tốt)
  await new Promise((resolve) => setTimeout(resolve, 500));

  // Vẽ lên canvas
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  const ctx = canvas.getContext("2d");
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  return canvas.toDataURL("image/jpeg", 0.9);
}

// ===== INIT SCANNER =====
function initQRScanner() {
  // SỬA Ở ĐÂY: Chữ 'H' viết hoa
  html5QrCode = new Html5Qrcode("reader");

  Html5Qrcode.getCameras()
    .then((cameras) => {
      if (cameras && cameras.length) {
        // Ưu tiên camera sau (thường là index 1 trên điện thoại)
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
          .catch((err) => console.error("Start failed:", err));
      } else {
        console.error("No cameras found.");
      }
    })
    .catch((err) => console.error("Error getting cameras:", err));
}

// ===== XỬ LÝ KHI QUÉT THÀNH CÔNG =====
function onScanSuccess(decodedText, decodedResult) {
  let qrData;
  try {
    qrData = JSON.parse(decodedText);
    if (!qrData.sessionId || !qrData.token) throw new Error();
  } catch (e) {
    showResult("Mã QR không đúng định dạng!", "danger");
    return;
  }

  // Dừng camera quét
  html5QrCode.stop();

  // STRICT: Bắt buộc lấy GPS
  showResult("📍 Đang định vị GPS (Bắt buộc)...", "info");
  getGPSStrict(qrData.sessionId, qrData.token);
}

function onScanFailure(error) {
  /* Ignore */
}

// ===== HÀM LẤY GPS NGHIÊM NGẶT =====
function getGPSStrict(qrId, tokenSecret) {
  if (!navigator.geolocation) {
    showStrictError("Thiết bị của bạn không có GPS. Không thể điểm danh!");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      console.log("GPS Success:", position.coords.accuracy + "m");
      performCheckIn(
        qrId,
        tokenSecret,
        position.coords.latitude,
        position.coords.longitude,
        position.coords.accuracy
      );
    },
    (error) => {
      console.warn("GPS Error:", error);
      // NẾU LỖI GPS -> CHẶN LUÔN
      if (error.code === 1) {
        showStrictError(
          "🛑 Bạn đã chặn quyền GPS! Vui lòng bật định vị trình duyệt để tiếp tục."
        );
      } else if (error.code === 2 || error.code === 3) {
        showStrictError(
          "📡 Không thể bắt được tín hiệu GPS. Hãy ra chỗ thoáng hoặc bật Wifi để hỗ trợ định vị."
        );
      } else {
        showStrictError("❌ Lỗi định vị: " + error.message);
      }
    },
    {
      enableHighAccuracy: true,
      timeout: 10000, // Đợi tối đa 10s
      maximumAge: 0, // Không dùng cache cũ
    }
  );
}

// Hàm hiển thị lỗi chặn đứng và cho nút thử lại
function showStrictError(msg) {
  const resultDiv = $("#result");
  resultDiv
    .removeClass("alert-success alert-info alert-warning")
    .addClass("alert-danger");
  resultDiv.html(
    `<i class="fas fa-exclamation-triangle"></i> ${msg}<br><br><button class="btn btn-sm btn-outline-danger" onclick="location.reload()">Thử lại</button>`
  );
  resultDiv.show();
}

// ===== GỬI REQUEST ĐIỂM DANH =====
async function performCheckIn(
  qrId,
  tokenSecret,
  latitude,
  longitude,
  gpsAccuracy
) {
  try {
    showResult("📸 Giữ yên máy, đang chụp ảnh...", "info");
    const imageData = await captureSelfie();

    showResult("⏳ Đang xử lý...", "info");
    const compressedImage = await compressImage(imageData, 640, 0.7);
    const deviceUid = getOrCreateDeviceUUID();

    // Lấy IP (Optional)
    let clientIp = null;
    try {
      const res = await fetch("https://api.ipify.org?format=json");
      const data = await res.json();
      clientIp = data.ip;
    } catch (e) {}

    const payload = {
      sessionId: qrId,
      token: tokenSecret,
      latitude: latitude, // Chắc chắn có giá trị
      longitude: longitude, // Chắc chắn có giá trị
      gpsAccuracy: gpsAccuracy,
      clientIp: clientIp,
      deviceId: deviceUid,
      selfieBase64: compressedImage,
      timestamp: Date.now(),
    };

    if (navigator.onLine) {
      sendCheckInRequest(payload);
    } else {
      saveOffline(payload);
    }
  } catch (error) {
    showResult("Lỗi xử lý: " + error.message, "danger");
    setTimeout(initQRScanner, 3000);
  }
}

function sendCheckInRequest(payload) {
  const contextPath = window.location.pathname.split("/")[1];
  $.ajax({
    url: `/${contextPath}/api/v2/attendance/check-in`,
    method: "POST",
    contentType: "application/json",
    data: JSON.stringify(payload),
    success: function (response) {
      localStorage.removeItem("offline_attendance");
      if (response.success) {
        showResult("✅ " + response.message, "success");
        setTimeout(
          () => (window.location.href = `/${contextPath}/student/dashboard`),
          2000
        );
      } else {
        showResult("❌ " + response.message, "danger");
        setTimeout(initQRScanner, 3000);
      }
    },
    error: function (xhr) {
      if (xhr.status === 0 && !navigator.onLine) {
        saveOffline(payload);
      } else {
        let msg =
          xhr.responseJSON && xhr.responseJSON.message
            ? xhr.responseJSON.message
            : "Lỗi server";
        showResult("❌ " + msg, "danger");
        setTimeout(initQRScanner, 3000);
      }
    },
  });
}

// ===== OFFLINE SYNC =====
function saveOffline(payload) {
  localStorage.setItem("offline_attendance", JSON.stringify(payload));
  showResult("⚠️ Mất mạng! Dữ liệu đã lưu, sẽ tự gửi khi có mạng.", "warning");
  window.addEventListener("online", syncOfflineData);
}

function syncOfflineData() {
  const saved = localStorage.getItem("offline_attendance");
  if (saved && navigator.onLine) sendCheckInRequest(JSON.parse(saved));
}

function showResult(msg, type) {
  const r = $("#result");
  r.attr("class", "alert alert-" + type)
    .html(msg)
    .show();
}

// ===== INIT LOGIC (STRICT) =====
$(document).ready(function () {
  syncOfflineData();
  checkPermissionsAndInit();
});

async function checkPermissionsAndInit() {
  try {
    // 1. Check Camera
    if (!(await checkCameraPermission())) {
      showPermissionBlock("camera");
      return;
    }

    // 2. Check GPS (STRICT)
    // Nếu không có quyền -> Chặn luôn, không hiện Scanner
    if (!(await checkGPSPermission())) {
      showPermissionBlock("gps");
      return;
    }

    // 3. Nếu đủ quyền -> Khởi động
    showResult("Đang khởi động...", "info");
    await initCamera();
    initQRScanner();
    showResult("Sẵn sàng quét mã QR", "success");
  } catch (e) {
    showResult("Lỗi khởi tạo: " + e.message, "danger");
  }
}

async function checkCameraPermission() {
  try {
    const s = await navigator.mediaDevices.getUserMedia({ video: true });
    s.getTracks().forEach((t) => t.stop());
    return true;
  } catch {
    return false;
  }
}

async function checkGPSPermission() {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve(false);
      return;
    }

    // Cố gắng lấy vị trí ngay lập tức để check quyền
    navigator.geolocation.getCurrentPosition(
      () => resolve(true), // Thành công -> Có quyền
      (err) => {
        console.warn("GPS Init Check Failed:", err);
        resolve(false); // Thất bại -> Không có quyền
      },
      { timeout: 5000 }
    );
  });
}

function showPermissionBlock(type) {
  const p = document.getElementById("permissionSection");
  document.getElementById("result").style.display = "none";
  p.style.display = "block";

  const btn = document.getElementById("requestPermissionsBtn");
  const msg = document.getElementById("permMessage") || p; // Giả sử có thẻ p để hiện text

  if (type === "camera") {
    btn.innerText = "Cấp quyền Camera";
    // msg.innerText = "Ứng dụng cần Camera để quét mã QR.";
  } else {
    btn.innerText = "Cấp quyền Vị trí (GPS)";
    // msg.innerText = "BẮT BUỘC bật GPS để điểm danh. Vui lòng cấp quyền!";
  }

  btn.onclick = async () => {
    p.style.display = "none";
    // Reload lại flow check
    checkPermissionsAndInit();
  };
}
