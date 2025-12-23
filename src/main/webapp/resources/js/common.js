// Common utility functions for Smart Attendance System

// Show loading spinner
function showLoading() {
  const spinner = `
        <div class="spinner-overlay" id="loadingSpinner">
            <div class="spinner-border text-light" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    `;
  $("body").append(spinner);
}

// Hide loading spinner
function hideLoading() {
  $("#loadingSpinner").remove();
}

// Show toast notification
function showToast(message, type = "info") {
  const toastHtml = `
        <div class="position-fixed bottom-0 end-0 p-3" style="z-index: 11">
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        </div>
    `;

  $("body").append(toastHtml);
  const toastElement = $(".toast").last();
  const toast = new bootstrap.Toast(toastElement[0]);
  toast.show();

  // Remove after hidden
  toastElement.on("hidden.bs.toast", function () {
    $(this).parent().remove();
  });
}

// Confirm dialog
function confirmAction(message, callback) {
  if (confirm(message)) {
    callback();
  }
}

// Format date
function formatDate(dateString) {
  const date = new Date(dateString);
  return date.toLocaleDateString("vi-VN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// Get CSRF token (if needed)
function getCsrfToken() {
  return $('meta[name="_csrf"]').attr("content");
}

function getCsrfHeader() {
  return $('meta[name="_csrf_header"]').attr("content");
}

// Setup AJAX with CSRF token
$(document).ready(function () {
  const token = getCsrfToken();
  const header = getCsrfHeader();

  if (token && header) {
    $(document).ajaxSend(function (e, xhr, options) {
      xhr.setRequestHeader(header, token);
    });
  }
});
