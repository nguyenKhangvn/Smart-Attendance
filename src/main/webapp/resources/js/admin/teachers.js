// Admin Teachers Page Scripts
const contextPath =
  document.querySelector('meta[name="_context"]')?.content || "/";

function confirmDelete(btn) {
  const id = btn.getAttribute("data-id");
  const name = btn.getAttribute("data-name");

  document.getElementById("teacherName").innerText = name;
  document.getElementById("deleteForm").action =
    contextPath + "admin/teachers/delete/" + id;

  new bootstrap.Modal(document.getElementById("deleteModal")).show();
}
