function loadDashboardData() {
  fetch('/api/dashboard')
      .then(response => response.json())
      .then(data => {
        document.getElementById("verified-docs").textContent = data.verified;
        document.getElementById("templates").textContent = data.templates;
        document.getElementById("requests").textContent = data.requests;
        document.getElementById("errors").textContent = data.errors;
      })
      .catch(error => {
        console.error('Error loading dashboard data:', error);
      });
}

// Завантаження при старті
loadDashboardData();

// Оновлення кожні 10 секунд (не обов'язково)
setInterval(loadDashboardData, 10000);