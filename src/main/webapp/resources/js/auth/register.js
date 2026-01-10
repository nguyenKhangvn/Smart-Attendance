// Auth Register Page Scripts
// Password confirmation validation
const password = document.getElementById("password");
const confirmPassword = document.getElementById("confirmPassword");

confirmPassword.addEventListener("input", function () {
  if (password.value !== confirmPassword.value) {
    confirmPassword.setCustomValidity("Mật khẩu không khớp");
  } else {
    confirmPassword.setCustomValidity("");
  }
});

password.addEventListener("input", function () {
  if (
    password.value !== confirmPassword.value &&
    confirmPassword.value !== ""
  ) {
    confirmPassword.setCustomValidity("Mật khẩu không khớp");
  } else {
    confirmPassword.setCustomValidity("");
  }
});
