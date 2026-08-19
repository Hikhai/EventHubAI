/* ============================================================
   MAIN JAVASCRIPT
   ============================================================ */

// Chạy khi DOM ready
document.addEventListener('DOMContentLoaded', function() {

    // ===== DARK MODE TOGGLE =====
    initDarkMode();

    // ===== FLASH MESSAGE AUTO-HIDE =====
    initFlashMessages();

    // ===== CONFIRM DIALOGS =====
    initConfirmForms();

    // ===== AOS INIT (nếu có) =====
    if (typeof AOS !== 'undefined') {
        AOS.init({ duration: 600, once: true });
    }
});

/**
 * Khởi tạo dark mode với localStorage
 */
function initDarkMode() {
    const toggleBtn = document.getElementById('darkModeToggle');
    const html = document.documentElement;

    // Load preference từ localStorage
    const savedTheme = localStorage.getItem('theme') || 'light';
    html.setAttribute('data-bs-theme', savedTheme);
    updateDarkModeIcon(savedTheme);

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function() {
            const currentTheme = html.getAttribute('data-bs-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-bs-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            updateDarkModeIcon(newTheme);
        });
    }
}

function updateDarkModeIcon(theme) {
    const icon = document.getElementById('darkModeIcon');
    if (icon) {
        icon.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
    }
}

/**
 * Auto-hide flash messages sau 4 giây
 */
function initFlashMessages() {
    const flashMessages = document.querySelectorAll('.alert-flash');
    flashMessages.forEach(function(msg) {
        setTimeout(function() {
            msg.style.transition = 'opacity 0.5s';
            msg.style.opacity = '0';
            setTimeout(() => msg.remove(), 500);
        }, 4000);
    });
}

/**
 * Confirm dialog cho các form có class "confirm-form"
 */
function initConfirmForms() {
    const forms = document.querySelectorAll('form.confirm-form');
    forms.forEach(function(form) {
        form.addEventListener('submit', function(e) {
            const message = form.dataset.confirm || 'Bạn có chắc chắn?';
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
}