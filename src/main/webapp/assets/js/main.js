document.addEventListener('DOMContentLoaded', function () {
    initDarkMode();
    initFlashMessages();
    initConfirmForms();
    initNavbarScroll();

    if (typeof AOS !== 'undefined') {
        AOS.init({ duration: 650, once: true, easing: 'ease-out-cubic', offset: 40 });
    }
});

function initNavbarScroll() {
    const navbar = document.getElementById('mainNavbar');
    if (!navbar) return;

    const onScroll = function () {
        navbar.classList.toggle('is-scrolled', window.scrollY > 8);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
}

function initDarkMode() {
    const toggleBtn = document.getElementById('darkModeToggle');
    const html = document.documentElement;
    const savedTheme = localStorage.getItem('theme') || 'light';
    html.setAttribute('data-bs-theme', savedTheme);
    updateDarkModeIcon(savedTheme);

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
            const newTheme = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            html.setAttribute('data-bs-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            updateDarkModeIcon(newTheme);
        });
    }
}

function updateDarkModeIcon(theme) {
    const icon = document.getElementById('darkModeIcon');
    if (icon) {
        icon.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-stars-fill';
    }
}

function initFlashMessages() {
    document.querySelectorAll('.alert-flash').forEach(function (msg) {
        setTimeout(function () {
            msg.style.transition = 'opacity 0.4s, transform 0.4s';
            msg.style.opacity = '0';
            msg.style.transform = 'translateX(12px)';
            setTimeout(function () { msg.remove(); }, 400);
        }, 4200);
    });
}

function initConfirmForms() {
    document.querySelectorAll('form.confirm-form').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            const message = form.dataset.confirm || 'Bạn có chắc chắn?';
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
}
