// Teacher Attendance Page Scripts
// Get session ID from page (will be set by Thymeleaf)
let sessionId = 0;
const contextPath = window.location.pathname.split("/")[1];

// Configuration
let autoRefreshEnabled = true;
let refreshInterval;
let soundEnabled = true;

// Initialize when page loads
document.addEventListener("DOMContentLoaded", function () {
  console.log("Page loaded. Initializing...");

  // Extract sessionId from the page context
  const sessionIdEl = document.querySelector("[data-session-id]");
  if (sessionIdEl) {
    sessionId = parseInt(sessionIdEl.dataset.sessionId);
  }

  // Load initial data from Server
  loadInitialData();

  // Start auto-refresh
  startAutoRefresh();

  // Bind events
  document
    .getElementById("toggleAutoRefresh")
    .addEventListener("click", toggleAutoRefresh);
  document.getElementById("soundToggle").addEventListener("click", toggleSound);
});

function loadInitialData() {
  // Get initial data from page
  const recordsEl = document.querySelector("[data-initial-records]");
  let attendanceRecords = [];

  if (recordsEl) {
    try {
      attendanceRecords = JSON.parse(recordsEl.dataset.initialRecords);
    } catch (e) {
      console.error("Failed to parse initial records", e);
    }
  }

  console.log("Initial Records:", attendanceRecords);
  updateStatistics(attendanceRecords);
}

function startAutoRefresh() {
  if (refreshInterval) clearInterval(refreshInterval);

  refreshInterval = setInterval(() => {
    if (autoRefreshEnabled) {
      fetchAttendanceData();
    }
  }, 3000); // Refresh every 3 seconds
}

async function fetchAttendanceData() {
  try {
    showRefreshIndicator();

    const apiUrl = contextPath
      ? `/${contextPath}/api/attendance/session/${sessionId}`
      : `/api/attendance/session/${sessionId}`;

    const response = await fetch(apiUrl);

    if (!response.ok) {
      throw new Error("Failed to fetch attendance data");
    }

    const records = await response.json();
    updateAttendanceTable(records);
    updateStatistics(records);
  } catch (error) {
    console.error("Error fetching attendance:", error);
  }
}

function updateStatistics(records) {
  const totalStudents = document.querySelectorAll(
    "#attendanceTable tbody tr"
  ).length;

  let presentCount = 0;
  if (records && Array.isArray(records)) {
    presentCount = records.filter(
      (r) => r.status === "SUCCESS" || r.status === "LATE"
    ).length;
  }

  const absentCount = totalStudents - presentCount;
  const attendanceRate =
    totalStudents > 0 ? ((presentCount / totalStudents) * 100).toFixed(1) : 0;

  document.getElementById("totalStudents").innerText = totalStudents;
  document.getElementById("presentCount").innerText = presentCount;
  document.getElementById("absentCount").innerText = absentCount;
  document.getElementById("attendanceRate").innerText = attendanceRate + "%";

  console.log(
    `Stats Updated: Total=${totalStudents}, Present=${presentCount}, Absent=${absentCount}`
  );
}

function updateAttendanceTable(records) {
  const rows = document.querySelectorAll("#attendanceTable tbody tr");

  rows.forEach((row) => {
    const studentCodeEl = row.cells[1].querySelector("strong");
    if (!studentCodeEl) return;
    const studentCode = studentCodeEl.textContent.trim();

    const record = records.find((r) => r.student.studentCode === studentCode);

    const wasAbsent = row.cells[4]
      .querySelector(".badge")
      ?.classList.contains("bg-secondary");
    const isNowPresent =
      record && (record.status === "SUCCESS" || record.status === "LATE");

    if (record) {
      row.cells[4].innerHTML = getStatusBadge(record.status);
      row.cells[5].innerHTML = formatDateTime(record.checkedInAt);
      row.cells[6].innerHTML = record.distanceMeters
        ? record.distanceMeters.toFixed(2)
        : "--";
      row.cells[7].innerHTML =
        record.failReason ||
        (record.modificationNote
          ? `<span class="text-info small fst-italic">${record.modificationNote}</span>`
          : "");

      if (wasAbsent && isNowPresent) {
        row.classList.add("new-attendance");
        setTimeout(() => row.classList.remove("new-attendance"), 2000);
        if (soundEnabled) playNotificationSound();
      }
    } else {
      row.cells[4].innerHTML =
        '<span class="badge bg-secondary status-badge"><i class="fas fa-times-circle"></i> Vắng</span>';
      row.cells[5].innerHTML = '<span class="text-muted">--:--:--</span>';
      row.cells[6].innerHTML = '<span class="text-muted">--</span>';
      row.cells[7].innerHTML = "";
    }
  });
}

function getStatusBadge(status) {
  const badges = {
    SUCCESS:
      '<span class="badge bg-success status-badge"><i class="fas fa-check-circle"></i> Có mặt</span>',
    LATE: '<span class="badge bg-warning status-badge"><i class="fas fa-clock"></i> Đi muộn</span>',
    FAILED_DISTANCE:
      '<span class="badge bg-danger status-badge"><i class="fas fa-map-marker-alt"></i> Quá xa</span>',
    FAILED_ALREADY_CHECKED:
      '<span class="badge bg-warning status-badge"><i class="fas fa-exclamation-triangle"></i> Đã điểm danh</span>',
    FAILED_DUPLICATE_DEVICE:
      '<span class="badge bg-danger status-badge"><i class="fas fa-mobile-alt"></i> Thiết bị trùng</span>',
    FAILED_INVALID_QR:
      '<span class="badge bg-danger status-badge"><i class="fas fa-qrcode"></i> QR không hợp lệ</span>',
    ABSENT:
      '<span class="badge bg-secondary status-badge"><i class="fas fa-times-circle"></i> Vắng</span>',
  };
  return badges[status] || badges["ABSENT"];
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '<span class="text-muted">--:--:--</span>';
  const date = new Date(dateTimeStr);
  return `${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes()
  ).padStart(2, "0")}:${String(date.getSeconds()).padStart(2, "0")} ${String(
    date.getDate()
  ).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function toggleAutoRefresh() {
  autoRefreshEnabled = !autoRefreshEnabled;
  const btn = document.getElementById("toggleAutoRefresh");
  const statusSpan = document.getElementById("autoRefreshStatus");
  if (autoRefreshEnabled) {
    btn.innerHTML = '<i class="fas fa-pause"></i> Tạm dừng';
    statusSpan.textContent = "Bật";
    statusSpan.parentElement.className = "badge bg-secondary";
  } else {
    btn.innerHTML = '<i class="fas fa-play"></i> Tiếp tục';
    statusSpan.textContent = "Tắt";
    statusSpan.parentElement.className = "badge bg-danger";
  }
}

function toggleSound() {
  soundEnabled = !soundEnabled;
  const icon = document.getElementById("soundIcon");
  const badge = document.getElementById("soundToggle");
  if (soundEnabled) {
    icon.className = "fas fa-volume-up";
    badge.className = "badge bg-info sound-toggle";
  } else {
    icon.className = "fas fa-volume-mute";
    badge.className = "badge bg-secondary sound-toggle";
  }
}

function showRefreshIndicator() {
  const indicator = document.getElementById("refreshIndicator");
  indicator.classList.add("show");
  setTimeout(() => indicator.classList.remove("show"), 1000);
}

function playNotificationSound() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.frequency.value = 800;
    gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.1);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.1);
  } catch (e) {}
}

function refreshNow() {
  fetchAttendanceData();
}

function exportToExcel() {
  let table = document.getElementById("attendanceTable");
  let html = table.outerHTML;
  let url = "data:application/vnd.ms-excel," + encodeURIComponent(html);
  let link = document.createElement("a");
  link.href = url;
  link.download = `attendance_${sessionId}.xls`;
  link.click();
}

window.addEventListener("beforeunload", () => {
  if (refreshInterval) clearInterval(refreshInterval);
});
