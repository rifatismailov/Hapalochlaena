function number_format(number, decimals = 0, dec_point = '.', thousands_sep = ',') {
  number = (number + '').replace(',', '').replace(' ', '');
  var n = isFinite(+number) ? +number : 0;
  var prec = Math.abs(decimals);
  var s = (prec ? (Math.round(n * Math.pow(10, prec)) / Math.pow(10, prec)).toFixed(prec) : '' + Math.round(n)).split('.');
  s[0] = s[0].replace(/\B(?=(\d{3})+(?!\d))/g, thousands_sep);
  if ((s[1] || '').length < prec) s[1] = (s[1] || '') + '0'.repeat(prec - s[1].length);
  return s.join(dec_point);
}

fetch("/api/earnings") // 🔁 Запит на сервер
    .then(response => response.json())
    .then(data => {
      const labels = data.labels;   // ["Jan", "Feb", ...]
      const values = data.values;   // [1000, 2000, ...]

      const ctx = document.getElementById("myAreaChart");
      new Chart(ctx, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [{
            label: "Documents",
            lineTension: 0.3,
            backgroundColor: "rgba(78, 115, 223, 0.05)",
            borderColor: "rgba(78, 115, 223, 1)",
            pointRadius: 3,
            pointBackgroundColor: "rgba(78, 115, 223, 1)",
            pointBorderColor: "rgba(78, 115, 223, 1)",
            pointHoverRadius: 3,
            pointHoverBackgroundColor: "rgba(78, 115, 223, 1)",
            pointHoverBorderColor: "rgba(78, 115, 223, 1)",
            pointHitRadius: 10,
            pointBorderWidth: 2,
            data: values
          }],
        },
        options: {
          maintainAspectRatio: false,
          layout: { padding: { left: 10, right: 25, top: 25, bottom: 0 } },
          scales: {
            xAxes: [{
              time: { unit: 'date' },
              gridLines: { display: false, drawBorder: false },
              ticks: { maxTicksLimit: 7 }
            }],
            yAxes: [{
              ticks: {
                maxTicksLimit: 5,
                padding: 10,
                callback: function(value) {
                  return number_format(value);
                }
              },
              gridLines: {
                color: "rgb(234, 236, 244)",
                zeroLineColor: "rgb(234, 236, 244)",
                drawBorder: false,
                borderDash: [2],
                zeroLineBorderDash: [2]
              }
            }]
          },
          legend: { display: false },
          tooltips: {
            backgroundColor: "rgb(255,255,255)",
            bodyFontColor: "#858796",
            titleMarginBottom: 10,
            titleFontColor: '#6e707e',
            titleFontSize: 14,
            borderColor: '#dddfeb',
            borderWidth: 1,
            xPadding: 15,
            yPadding: 15,
            displayColors: false,
            intersect: false,
            mode: 'index',
            caretPadding: 10,
            callbacks: {
              label: function(tooltipItem, chart) {
                return 'Documents: ' + number_format(tooltipItem.yLabel);
              }
            }
          }
        }
      });
    });
