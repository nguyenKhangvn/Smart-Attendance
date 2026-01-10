// Student Attendance History Page Scripts
// Export to Excel functionality
function exportToExcel() {
  const table = document.getElementById("attendanceTable");
  const rows = table.querySelectorAll("tr");

  let csvContent = "";
  rows.forEach((row) => {
    const cols = row.querySelectorAll("td, th");
    const rowData = Array.from(cols)
      .map((col) => col.textContent.trim())
      .join(",");
    csvContent += rowData + "\n";
  });

  const blob = new Blob(["\ufeff" + csvContent], {
    type: "text/csv;charset=utf-8;",
  });
  const link = document.createElement("a");
  const url = URL.createObjectURL(blob);

  link.setAttribute("href", url);
  link.setAttribute(
    "download",
    "lich-su-diem-danh-" + new Date().toISOString().split("T")[0] + ".csv"
  );
  link.style.visibility = "hidden";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// Search functionality
const searchInput = document.getElementById("searchInput");
if (searchInput) {
  searchInput.addEventListener("keyup", function () {
    const filter = this.value.toLowerCase();
    const table = document.getElementById("attendanceTable");
    const rows = table.getElementsByTagName("tr");

    for (let i = 1; i < rows.length; i++) {
      const row = rows[i];
      const cells = row.getElementsByTagName("td");
      let found = false;

      for (let j = 0; j < cells.length; j++) {
        const cell = cells[j];
        if (cell.textContent.toLowerCase().indexOf(filter) > -1) {
          found = true;
          break;
        }
      }

      row.style.display = found ? "" : "none";
    }
  });
}
