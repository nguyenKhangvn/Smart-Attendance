// Admin Classes Page Scripts
const contextPath =
  document.querySelector('meta[name="_context"]')?.content || "/";

function confirmDelete(button) {
  const classId = button.getAttribute("data-class-id");
  const className = button.getAttribute("data-class-name");

  document.getElementById("deleteClassName").textContent = className;
  document.getElementById("deleteForm").action =
    contextPath + "admin/classes/delete/" + classId;

  const modal = new bootstrap.Modal(document.getElementById("deleteModal"));
  modal.show();
}
