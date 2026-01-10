// Admin Students Page Scripts
function confirmDelete(btn) {
  const id = btn.getAttribute("data-id");
  const name = btn.getAttribute("data-name");

  document.getElementById("studentName").innerText = name;
  document.getElementById("deleteForm").action = "/admin/students/delete/" + id;

  new bootstrap.Modal(document.getElementById("deleteModal")).show();
}
