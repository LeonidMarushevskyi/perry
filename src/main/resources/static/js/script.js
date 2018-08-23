document.getElementById("login-form").onkeypress = function (e) {
  var key = e.charCode || e.keyCode || 0;
  if (key === 13) {
    e.preventDefault();
  }
};

var input = document.getElementById("username");
input.addEventListener('keyup', function () {
  validateLoginString()
});
input.addEventListener('keydown', function () {
  validateLoginString()
});

if (window.location.toString().indexOf("error") !== -1) {
  document.getElementById("error").innerHTML = "Invalid username or password!";
}

function validateLoginString() {
  var loginInfo = document.getElementById("username");
  var loginError = document.getElementById("login-error");
  var submitBtn = document.getElementById("submitBtn");
  if (checkIfLoginStringIsValid(loginInfo.value)) {
    submitBtn.disabled = false;
    while (loginError.firstChild) {
      loginError.removeChild(loginError.firstChild);
    }

  } else {
    submitBtn.disabled = true;
    loginError.innerHTML = "Please enter valid authorization JSON. \n";
    loginError.innerHTML += "Example: {\n"
        + "\t\"user\": \"RACFID\",\n"
        + "\t\"staffId\": \"0X5\",\n"
        + "\t\"roles\": [\n"
        + "\t\t\"CWS-admin\",\n"
        + "\t\t\"CANS-worker\",\n"
        + "\t\t\"Supervisor\"\n"
        + "\t],\n"
        + "\t\"county_code\": \"56\",\n"
        + "\t\"county_cws_code\": \"1123\",\n"
        + "\t\"county_name\": \"Ventura\",\n"
        + "\t\"privileges\": [\n"
        + "\t\t\"CWS Case Management System\",\n"
        + "\t\t\"Resource Management\",\n"
        + "\t\t\"Resource Mgmt Placement Facility Maint\",\n"
        + "\t\t\"Sealed\",\n"
        + "\t\t\"Snapshot-rollout\",\n"
        + "\t\t\"Hotline-rollout\",\n"
        + "\t\t\"Facility-search-rollout\",\n"
        + "\t\t\"RFA-rollout\",\n"
        + "\t\t\"CANS-rollout\",\n"
        + "\t\t\"development-not-in-use\"\n"
        + "\t]\n"
        + "}";
  }
}

function checkIfLoginStringIsValid(str) {
  return isNotEmpty(str) && strHasNoNonAsciiSymbols(str) && isValidJson(
      str);
}

function strHasNoNonAsciiSymbols(str) {
  var ascii = /^[ -~]+$/;
  return ascii.test(str);

}

function isValidJson(str) {
  try {
    return "user" in JSON.parse(str);
  } catch (e) {
    return false;
  }
}

function isNotEmpty(str) {
  return !(!str || 0 === str.length || /^\s*$/.test(str));
}
