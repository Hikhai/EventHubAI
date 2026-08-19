/* ============================================================
   DASHBOARD - Chart.js Configuration
   ============================================================ */

document.addEventListener('DOMContentLoaded', function() {

    // ===== COUNTER ANIMATION cho KPI cards =====
    animateCounters();

    // ===== INIT CHARTS =====
    if (typeof window.dashboardData !== 'undefined') {
        initRegistrationsChart(window.dashboardData.registrationsByMonth);
        initCategoryChart(window.dashboardData.eventsByCategory);
    }
});

/**
 * Animate số trong KPI cards từ 0 → giá trị đích
 */
function animateCounters() {
    const counters = document.querySelectorAll('[data-count]');

    counters.forEach(function(counter) {
        const target = parseInt(counter.getAttribute('data-count')) || 0;
        const duration = 1200; // 1.2 giây
        const startTime = performance.now();

        function updateCounter(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            // Easing function (ease-out-cubic)
            const easeProgress = 1 - Math.pow(1 - progress, 3);
            const current = Math.floor(target * easeProgress);

            counter.textContent = current.toLocaleString('vi-VN');

            if (progress < 1) {
                requestAnimationFrame(updateCounter);
            } else {
                counter.textContent = target.toLocaleString('vi-VN');
            }
        }

        requestAnimationFrame(updateCounter);
    });
}

/**
 * Line Chart: Đăng ký theo tháng
 */
function initRegistrationsChart(data) {
    const canvas = document.getElementById('registrationsChart');
    if (!canvas) return;

    // Nếu không có data, hiển thị chart rỗng
    const labels = data.map(item => item.month || '');
    const values = data.map(item => item.count || 0);

    const ctx = canvas.getContext('2d');

    // Tạo gradient cho fill
    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
    gradient.addColorStop(1, 'rgba(99, 102, 241, 0.02)');

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels.length > 0 ? labels : ['Chưa có dữ liệu'],
            datasets: [{
                label: 'Số đăng ký',
                data: values.length > 0 ? values : [0],
                borderColor: '#6366F1',
                backgroundColor: gradient,
                borderWidth: 3,
                pointBackgroundColor: '#6366F1',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7,
                tension: 0.4, // Đường cong mượt
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(30, 41, 59, 0.95)',
                    padding: 12,
                    titleFont: { size: 13, weight: '600' },
                    bodyFont: { size: 13 },
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y + ' đăng ký';
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#94A3B8' }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: '#94A3B8',
                        precision: 0 // Số nguyên
                    },
                    grid: {
                        color: '#F1F5F9',
                        drawBorder: false
                    }
                }
            }
        }
    });
}

/**
 * Doughnut Chart: Sự kiện theo danh mục
 */
function initCategoryChart(data) {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;

    const labels = data.map(item => item.categoryName || 'Khác');
    const values = data.map(item => item.totalEvents || 0);

    // Palette màu đẹp cho các slice
    const colors = [
        '#6366F1', // Indigo
        '#8B5CF6', // Violet
        '#EC4899', // Pink
        '#F59E0B', // Amber
        '#10B981', // Emerald
        '#3B82F6', // Blue
        '#EF4444', // Red
        '#14B8A6'  // Teal
    ];

    // Nếu không có data
    const hasData = values.some(v => v > 0);

    new Chart(canvas.getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: hasData ? labels : ['Chưa có dữ liệu'],
            datasets: [{
                data: hasData ? values : [1],
                backgroundColor: hasData ? colors.slice(0, labels.length) : ['#E2E8F0'],
                borderWidth: 3,
                borderColor: '#fff',
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        usePointStyle: true,
                        pointStyle: 'circle',
                        color: '#64748B',
                        font: { size: 12 }
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(30, 41, 59, 0.95)',
                    padding: 12,
                    titleFont: { size: 13, weight: '600' },
                    bodyFont: { size: 13 },
                    callbacks: {
                        label: function(context) {
                            const value = context.parsed;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percent = total > 0
                                ? ((value / total) * 100).toFixed(1) : 0;
                            return context.label + ': '
                                + value + ' sự kiện (' + percent + '%)';
                        }
                    }
                }
            }
        }
    });
}