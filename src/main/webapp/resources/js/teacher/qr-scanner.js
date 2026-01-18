/* Teacher QR Scanner Script - Optimized with Map */

// Biến lưu tọa độ cache để không phải gọi GPS liên tục
let cachedLatitude = null;
let cachedLongitude = null;

// Biến Leaflet map và marker
let map = null;
let marker = null;

let autoRefreshInterval;
let progressBarInterval;
const QR_REFRESH_INTERVAL = 10; // Đổi QR mỗi 10 giây

// Khởi tạo ngay khi vào trang
$(document).ready(function () {
  // 1. Lấy GPS trước
  initGPSAndStart();

  $("#regenerateBtn").click(function () {
    // Nút này dùng để force update cả GPS lẫn QR
    stopAutoRefresh();
    initGPSAndStart();
  });
});

function initGPSAndStart() {
  $("#qrStatus").text("Đang lấy vị trí GPS...");

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        // Lưu tọa độ vào biến global
        cachedLatitude = position.coords.latitude;
        cachedLongitude = position.coords.longitude;

        console.log(" GPS Locked:", cachedLatitude, cachedLongitude);
        $("#qrStatus").text("Đã có vị trí. Đang hiển thị bản đồ...");

        // HIỂN THỊ BẢN ĐỒ ĐỂ GIÁO VIÊN XÁC NHẬN/CHỈNH VỊ TRÍ
        initMap(cachedLatitude, cachedLongitude);

        // Bắt đầu chu trình tạo QR
        generateQRCode(); // Tạo cái đầu tiên ngay
        startAutoRefresh(); // Bắt đầu lặp
      },
      function (error) {
        alert("Không thể lấy vị trí. Vui lòng bật GPS và tải lại trang!");
        $("#qrStatus").text("Lỗi GPS - Không thể tạo mã.");
      },
      { enableHighAccuracy: true }, // Lấy chính xác cao cho giáo viên
    );
  } else {
    alert("Trình duyệt không hỗ trợ Geolocation!");
  }
}

/**
 * Khởi tạo bản đồ Leaflet với marker có thể kéo thả
 */
function initMap(lat, lng) {
  // Nếu map đã tồn tại, xóa đi để tạo lại (tránh lỗi khi regenerate)
  if (map !== null) {
    map.remove();
  }

  // 1. Khởi tạo bản đồ tại vị trí GPS
  map = L.map("map").setView([lat, lng], 18); // Zoom 18 cho rõ chi tiết

  // 2. Thêm tile layer từ OpenStreetMap (miễn phí)
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution:
      '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 19,
  }).addTo(map);

  // 3. Tạo marker có thể kéo thả (draggable: true)
  marker = L.marker([lat, lng], {
    draggable: true,
    title: "Kéo thả để chỉnh vị trí",
  }).addTo(map);

  // 4. Thêm popup thông báo cho marker
  marker
    .bindPopup(
      "<b>Vị trí điểm danh</b><br>" +
        "Kéo thả ghim này nếu vị trí không chính xác.<br>" +
        "<small>Tọa độ: " +
        lat.toFixed(6) +
        ", " +
        lng.toFixed(6) +
        "</small>",
    )
    .openPopup();

  // 5. Sự kiện khi giáo viên kéo marker xong
  marker.on("dragend", function (event) {
    const position = marker.getLatLng();

    // CẬP NHẬT LẠI TỌA ĐỘ
    cachedLatitude = position.lat;
    cachedLongitude = position.lng;

    console.log(" Giáo viên đã chỉnh vị trí:", cachedLatitude, cachedLongitude);

    // Cập nhật popup với tọa độ mới
    marker.setPopupContent(
      "<b>Vị trí điểm danh (Đã chỉnh)</b><br>" +
        "<small>Tọa độ: " +
        cachedLatitude.toFixed(6) +
        ", " +
        cachedLongitude.toFixed(6) +
        "</small>",
    );

    // Thông báo cho giáo viên
    $("#qrStatus").html(
      '<i class="fas fa-check-circle text-success"></i> ' +
        "Đã cập nhật vị trí mới! Mã QR sẽ dùng vị trí này.",
    );

    // Tùy chọn: Tự động tạo lại QR ngay khi kéo xong
    // stopAutoRefresh();
    // generateQRCode();
    // startAutoRefresh();
  });

  // 6. Thêm circle hiển thị bán kính cho phép (50m)
  L.circle([lat, lng], {
    color: "blue",
    fillColor: "#30f",
    fillOpacity: 0.1,
    radius: 50, // 50 mét
  })
    .addTo(map)
    .bindPopup("Bán kính cho phép: 50m");
}

function generateQRCode() {
  // Nếu chưa có GPS thì không làm gì cả
  if (cachedLatitude === null || cachedLongitude === null) return;

  // Get context path dynamically
  const contextPath = window.location.pathname.split("/")[1];
  const apiUrl = `/${contextPath}/api/qr/generate`;

  $.ajax({
    url: apiUrl,
    method: "POST",
    data: {
      sessionId: sessionId, // Biến này từ Thymeleaf
      latitude: cachedLatitude, // Dùng tọa độ đã cache
      longitude: cachedLongitude,
    },
    success: function (response) {
      $("#qrCode").html(
        `<img src="${response.qrCodeBase64}" class="img-fluid" alt="QR Code" style="max-height: 400px;">`,
      );

      // Reset thanh thời gian
      runProgressBar();
    },
    error: function (error) {
      console.error("QR Gen Error:", error);
      // Không alert liên tục vì đang auto-refresh, chỉ log thôi
      $("#qrStatus").text("Mất kết nối server...");
    },
  });
}

function startAutoRefresh() {
  if (autoRefreshInterval) clearInterval(autoRefreshInterval);

  // Tạo QR mới mỗi 10 giây
  autoRefreshInterval = setInterval(function () {
    generateQRCode();
  }, QR_REFRESH_INTERVAL * 1000);
}

function stopAutoRefresh() {
  if (autoRefreshInterval) clearInterval(autoRefreshInterval);
  if (progressBarInterval) clearInterval(progressBarInterval);
}

// Tạo hiệu ứng thanh thời gian trôi (UX tốt hơn đồng hồ số)
function runProgressBar() {
  if (progressBarInterval) clearInterval(progressBarInterval);

  let width = 100;
  const step = 100 / (QR_REFRESH_INTERVAL * 10); // Update mỗi 100ms

  // Giả sử bạn có 1 div id="qrProgressBar" trong HTML
  $("#qrProgressBar")
    .css("width", "100%")
    .removeClass("bg-danger")
    .addClass("bg-success");

  progressBarInterval = setInterval(function () {
    width -= step;
    $("#qrProgressBar").css("width", width + "%");

    if (width < 30) {
      $("#qrProgressBar").removeClass("bg-success").addClass("bg-danger");
    }

    if (width <= 0) {
      clearInterval(progressBarInterval);
    }
  }, 100);
}
