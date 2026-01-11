/* Student QR Scanner - Final Version with Smart Guide */
let html5QrCode;
let videoStream;

// ===== 1. CÁC HÀM TIỆN ÍCH (HELPER) =====

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

// Hàm phát hiện hệ điều hành để hướng dẫn đúng
function getMobileOS() {
  const ua = navigator.userAgent;
  if (/android/i.test(ua)) return "ANDROID";
  if (/iPad|iPhone|iPod/.test(ua) && !window.MSStream) return "IOS";
  return "UNKNOWN";
}

// ===== 2. KHỞI TẠO CAMERA & SELFIE =====

async function initCamera() {
  try {
    videoStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user" },
    });
    const video = document.getElementById("preview");
    video.srcObject = videoStream;
    // Fix lỗi iOS phóng to video
    video.setAttribute("playsinline", true);
    await video.play();
  } catch (error) {
    console.error("Camera error:", error);
    throw error;
  }
}

async function captureSelfie() {
    const video = document.getElementById("preview");
    const canvas = document.getElementById("canvas");

    // 1. CẤU HÌNH BẮT BUỘC CHO MOBILE (Fix lỗi màn hình đen iOS/Android)
    video.muted = true;       // Bắt buộc phải tắt tiếng mới cho autoplay
    video.playsInline = true; // Bắt buộc để không phóng to màn hình trên iOS
    
    // 2. ÉP CHẠY VIDEO NẾU ĐANG DỪNG
    if (video.paused || video.ended) {
        try {
            await video.play();
        } catch (e) {
            console.warn("Auto-play blocked, trying again...", e);
        }
    }

    // 3. LOGIC CHỜ CAMERA (Đã tối ưu)
    // Chỉ cần readyState >= 2 (HAVE_CURRENT_DATA) là đủ chụp, không cần chờ đến 4
    let waitCount = 0;
    const MAX_WAIT = 60; // 6 giây

    while (
        (video.readyState < 2 || video.videoWidth === 0) && 
        waitCount < MAX_WAIT
    ) {
        await new Promise((resolve) => setTimeout(resolve, 100)); // Đợi 100ms
        waitCount++;
        
        // Cứ mỗi 1 giây (10 lần lặp) lại thử ép play 1 lần cho chắc
        if (waitCount % 10 === 0) {
            video.play().catch(() => {});
        }
    }

    // 4. CHECK LẦN CUỐI
    if (video.videoWidth === 0 || video.videoHeight === 0) {
        // Fallback: Chờ thêm 0.5s may rủi
        await new Promise((resolve) => setTimeout(resolve, 500));
        
        if (video.videoWidth === 0) {
             throw new Error("Lỗi Camera: Không nhận được hình ảnh. Hãy kiểm tra quyền hoặc tải lại trang!");
        }
    }

    // 5. VẼ LÊN CANVAS
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    
    // Lật ảnh (Mirror) cho giống gương
    ctx.translate(canvas.width, 0);
    ctx.scale(-1, 1);
    
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
    return canvas.toDataURL("image/jpeg", 0.9);
}

// ===== 3. QR SCANNER =====
function initQRScanner() {
  html5QrCode = new Html5Qrcode("reader");
  Html5Qrcode.getCameras()
    .then((cameras) => {
      if (cameras && cameras.length) {
        const cameraId = cameras.length > 1 ? cameras[1].id : cameras[0].id;
        html5QrCode
          .start(
            cameraId,
            { fps: 10, qrbox: { width: 250, height: 250 } },
            onScanSuccess,
            onScanFailure
          )
          .catch((err) => console.error("Start failed:", err));
      } else {
        showResult("Không tìm thấy camera sau!", "danger");
      }
    })
    .catch((err) => console.error("Error getting cameras:", err));
}

// ===== 4. XỬ LÝ KHI QUÉT THÀNH CÔNG =====
function onScanSuccess(decodedText, decodedResult) {
  let qrData;
  try {
    qrData = JSON.parse(decodedText);
    if (!qrData.sessionId || !qrData.token) throw new Error();
  } catch (e) {
    showResult("Mã QR không hợp lệ!", "danger");
    return;
  }

  // 1. DỪNG CAMERA QUÉT QR - QUAN TRỌNG NHẤT LÀ DÒNG NÀY
  html5QrCode.stop().then(() => {
    $("#reader").hide(); // Ẩn khung quét QR
    $("#preview").show(); // Hiện video selfie
    
    // 2. BÂY GIỜ MỚI KHỞI ĐỘNG CAMERA SELFIE
    showResult("Đang bật camera xác thực...", "info");
    
    // Gọi hàm initCamera để bật cam trước
    initCamera().then(() => {
      // Cam trước đã lên, bắt đầu lấy GPS và gửi dữ liệu
      showResult("🛰️ Đang định vị GPS chính xác...", "info");
      getGPSWithNoiseFilter(qrData.sessionId, qrData.token, 1);
    }).catch(err => {
      showResult("Không thể bật camera selfie: " + err.message, "danger");
      setTimeout(() => location.reload(), 3000);
    });
  }).catch((err) => {
    console.error("Failed to stop QR scanner", err);
    showResult("Lỗi dừng camera quét. Vui lòng thử lại!", "danger");
    setTimeout(() => location.reload(), 3000);
  });
}

function onScanFailure(error) {
  /* Ignore */
}

// ===== 5. LẤY GPS (CÓ LỌC NHIỄU & RETRY) =====
function getGPSWithNoiseFilter(qrId, tokenSecret, attempt) {
  if (!navigator.geolocation) {
    showStrictError("Thiết bị không hỗ trợ GPS.");
    return;
  }

  const MAX_ATTEMPTS = 5;
  const TARGET_ACCURACY = 100; // Chỉ nhận nếu sai số < 100m

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const accuracy = position.coords.accuracy;
      console.log(`GPS Attempt ${attempt}: Accuracy ${accuracy}m`);

      if (accuracy <= TARGET_ACCURACY) {
        // GPS tốt -> Gửi luôn
        performCheckIn(
          qrId,
          tokenSecret,
          position.coords.latitude,
          position.coords.longitude,
          accuracy
        );
      } else if (attempt < MAX_ATTEMPTS) {
        // GPS nhiễu -> Thử lại
        showResult(
          `📡 Tín hiệu yếu (Sai số ${Math.round(
            accuracy
          )}m). Đang tinh chỉnh lần ${attempt}...`,
          "warning"
        );
        setTimeout(() => {
          getGPSWithNoiseFilter(qrId, tokenSecret, attempt + 1);
        }, 1500);
      } else {
        // Hết lượt -> Gửi đại (Backend sẽ chuyển sang Pending Review)
        console.warn("Chấp nhận GPS nhiễu sau 5 lần thử");
        performCheckIn(
          qrId,
          tokenSecret,
          position.coords.latitude,
          position.coords.longitude,
          accuracy
        );
      }
    },
    (error) => {
      // Lỗi kỹ thuật -> Retry
      if (attempt < MAX_ATTEMPTS) {
        setTimeout(
          () => getGPSWithNoiseFilter(qrId, tokenSecret, attempt + 1),
          1500
        );
      } else {
        // Hết lượt -> Hiện hướng dẫn bật lại GPS
        handleGPSError(error);
      }
    },
    { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
  );
}

// ===== 6. LOGIC CHECK QUYỀN & SHOW GUIDE (SMART GUIDE) =====

// Hàm xử lý lỗi GPS cụ thể
function handleGPSError(error) {
  if (error.code === 1) showPermissionGuide("GPS_DENIED");
  else if (error.code === 2 || error.code === 3)
    showPermissionGuide("GPS_SYSTEM_OFF");
  else showStrictError("Lỗi GPS: " + error.message);
}

// Hàm hiển thị Popup Hướng dẫn (HTML nằm trong file scan-qr.html)
function showPermissionGuide(type) {
  const os = getMobileOS();
  const guideOverlay = document.getElementById("permissionGuide");
  const guideTitle = document.getElementById("guideTitle");
  const guideSteps = document.getElementById("guideStepsContent");

  let htmlContent = "";

  // Kịch bản 1: Bị chặn quyền trang web (DENIED)
  if (type === "GPS_DENIED" || type === "CAMERA_DENIED") {
    guideTitle.innerText = "Trình duyệt đang chặn quyền!";

    if (os === "IOS") {
      htmlContent = `
                <div class="guide-step"><i class="fas fa-font icon-guide"></i> 1. Bấm vào biểu tượng <strong>"aA"</strong> hoặc <strong>"Ổ khóa"</strong> trên thanh địa chỉ.</div>
                <div class="guide-step"><i class="fas fa-cog icon-guide"></i> 2. Chọn <strong>"Cài đặt trang web"</strong>.</div>
                <div class="guide-step"><i class="fas fa-check-circle icon-guide"></i> 3. Chuyển <strong>Camera</strong> và <strong>Vị trí</strong> sang <strong>"Cho phép"</strong>.</div>`;
    } else {
      htmlContent = `
                <div class="guide-step"><i class="fas fa-lock icon-guide"></i> 1. Bấm vào biểu tượng <strong>"Ổ khóa"</strong> 🔒 trên thanh địa chỉ.</div>
                <div class="guide-step"><i class="fas fa-sliders-h icon-guide"></i> 2. Chọn <strong>"Quyền" (Permissions)</strong>.</div>
                <div class="guide-step"><i class="fas fa-toggle-on icon-guide"></i> 3. Bật công tắc cho <strong>Vị trí</strong> và <strong>Camera</strong>.</div>`;
    }
  }
  // Kịch bản 2: Chưa bật GPS hệ thống (SYSTEM OFF)
  else if (type === "GPS_SYSTEM_OFF") {
    guideTitle.innerText = "Chưa bật định vị điện thoại";
    htmlContent = `
            <div class="guide-step"><i class="fas fa-mobile-alt icon-guide"></i> 1. Vuốt thanh thông báo từ trên xuống.</div>
            <div class="guide-step"><i class="fas fa-map-marker-alt icon-guide"></i> 2. Tìm và BẬT biểu tượng <strong>Vị trí / Location / GPS</strong>.</div>
            <div class="guide-step"><i class="fas fa-wifi icon-guide"></i> 3. Thử kết nối Wifi của trường để định vị nhanh hơn.</div>`;
  }

  guideSteps.innerHTML = htmlContent;

  // Hiện Popup (Flex để căn giữa)
  guideOverlay.style.display = "flex";

  // Ẩn các phần khác để tránh rối
  $("#reader").hide();
  $("#permissionSection").hide();
}

// Hàm khởi chạy chính - Tự động check quyền khi vào trang
async function checkPermissionsAndInit() {
  try {
    // 1. Check Camera (Thử xin quyền)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      stream.getTracks().forEach((t) => t.stop()); // Tắt ngay sau khi check xong
    } catch (error) {
      console.warn("Camera check failed:", error);
      showPermissionGuide("CAMERA_DENIED");
      return;
    }

    // 2. Check GPS (Thử xin quyền)
    navigator.geolocation.getCurrentPosition(
      () => {
        // Nếu thành công -> Chạy App
        runApp();
      },
      (error) => {
        // Nếu thất bại -> Phân loại lỗi để hiện hướng dẫn
        if (error.code === 1) showPermissionGuide("GPS_DENIED");
        else if (error.code === 2) showPermissionGuide("GPS_SYSTEM_OFF");
        else {
          console.log("GPS Unknown error, trying anyway...");
          runApp();
        }
      },
      { timeout: 4000 }
    );
  } catch (e) {
    showStrictError("Lỗi khởi tạo: " + e.message);
  }
}

function runApp() {
  showResult("Đang khởi động máy quét...", "info");
  // Chỉ khởi động QR Scanner, KHÔNG mở camera selfie lúc này
  initQRScanner();
  showResult("Sẵn sàng quét mã QR", "success");
}

// ===== 7. GỬI REQUEST API =====
async function performCheckIn(
  qrId,
  tokenSecret,
  latitude,
  longitude,
  gpsAccuracy
) {
  try {
    showResult("📸 Đang chụp ảnh xác thực...", "info");
    const imageData = await captureSelfie();

    showResult("⏳ Đang gửi dữ liệu...", "info");
    const compressedImage = await compressImage(imageData, 640, 0.7);
    const deviceUid = getOrCreateDeviceUUID();

    // Lấy IP Public (Optional)
    let clientIp = null;
    try {
      const controller = new AbortController();
      setTimeout(() => controller.abort(), 2000);
      const res = await fetch("https://api.ipify.org?format=json", {
        signal: controller.signal,
      });
      const data = await res.json();
      clientIp = data.ip;
    } catch (e) {}

    const payload = {
      sessionId: qrId,
      token: tokenSecret,
      latitude: latitude,
      longitude: longitude,
      gpsAccuracy: gpsAccuracy,
      clientIp: clientIp,
      deviceId: deviceUid,
      selfieBase64: compressedImage,
      timestamp: Date.now(),
    };

    if (navigator.onLine) sendCheckInRequest(payload);
    else saveOffline(payload);
  } catch (error) {
    showResult("Lỗi xử lý: " + error.message, "danger");
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
        if (
          response.message.includes("duyệt") ||
          response.message.includes("review")
        ) {
          showResult("⚠️ " + response.message, "warning");
        } else {
          showResult("✅ " + response.message, "success");
        }
        setTimeout(
          () => (window.location.href = `/${contextPath}/student/dashboard`),
          2500
        );
      } else {
        showResult("❌ " + response.message, "danger");
        setTimeout(() => location.reload(), 4000);
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
        setTimeout(() => location.reload(), 4000);
      }
    },
  });
}

// Offline Sync
function saveOffline(payload) {
  localStorage.setItem("offline_attendance", JSON.stringify(payload));
  showResult("⚠️ Mất mạng! Dữ liệu đã lưu, sẽ tự gửi khi có mạng.", "warning");
  window.addEventListener("online", () => {
    const saved = localStorage.getItem("offline_attendance");
    if (saved) sendCheckInRequest(JSON.parse(saved));
  });
}

function showResult(msg, type) {
  const r = $("#result");
  r.removeClass()
    .addClass("alert alert-" + type)
    .html(msg)
    .show();
}

function showStrictError(msg) {
  showResult(
    msg +
      ` <br><button class="btn btn-sm btn-outline-danger mt-2" onclick="location.reload()">Thử lại</button>`,
    "danger"
  );
}

// ===== 8. START APP =====
$(document).ready(function () {
  const saved = localStorage.getItem("offline_attendance");
  if (saved && navigator.onLine) sendCheckInRequest(JSON.parse(saved));

  // Bắt đầu quy trình check quyền thông minh
  checkPermissionsAndInit();
});
