/* Teacher Class Detail Script */

// Advanced QR Manager with countdown, auto-refresh, and real-time stats
const qrManagers = new Map();

document.querySelectorAll(".modal").forEach((modal) => {
  modal.addEventListener("show.bs.modal", function (event) {
    const sessionId =
      this.querySelector(".qr-image").getAttribute("data-session-id");
    const containerId = "qr-container-" + sessionId;

    // Replace spinner with container
    const qrImage = this.querySelector(".qr-image");
    qrImage.innerHTML = `<div id="${containerId}"></div>`;

    // Initialize QR Manager
    const manager = new QRManager(sessionId, containerId);
    manager.init();

    // Store manager for cleanup
    qrManagers.set(sessionId, manager);
  });

  // Cleanup when modal closes
  modal.addEventListener("hide.bs.modal", function (event) {
    const sessionId =
      this.querySelector(".qr-image").getAttribute("data-session-id");
    const manager = qrManagers.get(sessionId);

    if (manager) {
      manager.destroy();
      qrManagers.delete(sessionId);
    }
  });
});
