fetch('/api/sources')
    .then(res => res.json())
    .then(data => {
      const ctx = document.getElementById("myPieChart");
      new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: data.labels, // ["Direct", "Referral", "Social"]
          datasets: [{
            data: data.values,  // [55, 30, 15]
            backgroundColor: ['#4e73df', '#1cc88a', '#36b9cc'],
            hoverBackgroundColor: ['#2e59d9', '#17a673', '#2c9faf'],
            hoverBorderColor: "rgba(234, 236, 244, 1)",
          }]
        },
        options: {
          maintainAspectRatio: false,
          tooltips: {
            backgroundColor: "rgb(255,255,255)",
            bodyFontColor: "#858796",
            borderColor: '#dddfeb',
            borderWidth: 1,
            xPadding: 15,
            yPadding: 15,
            displayColors: false,
            caretPadding: 10,
          },
          legend: { display: true },
          cutoutPercentage: 80
        }
      });
    });
