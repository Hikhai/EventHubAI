<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng ký" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<div class="auth-container">

    <%-- ===== LEFT PANEL - HERO ===== --%>
    <div class="auth-hero">
        <div class="auth-hero-content">
            <h1>Tham gia EventHub</h1>
            <p>
                Tạo tài khoản miễn phí để đăng ký sự kiện và nhận gợi ý AI theo sở thích của bạn.
            </p>

            <div class="auth-stats">
                <div class="stat-card">
                    <span class="stat-number">🚀</span>
                    <span class="stat-label">Miễn phí hoàn toàn</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">🔒</span>
                    <span class="stat-label">Bảo mật an toàn</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">🤖</span>
                    <span class="stat-label">AI cá nhân hóa</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">⚡</span>
                    <span class="stat-label">Đăng ký nhanh chóng</span>
                </div>
            </div>
        </div>
    </div>

    <%-- ===== RIGHT PANEL - FORM ===== --%>
    <div class="auth-form-panel">
        <div class="auth-form">
            <h2>Tạo tài khoản</h2>
            <p class="subtitle">Điền thông tin để bắt đầu</p>

            <c:if test="${not empty errorMsg}">
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle"></i> ${errorMsg}
                </div>
            </c:if>

            <form method="post"
                  action="${pageContext.request.contextPath}/auth/register">

                <%-- Họ tên --%>
                <div class="form-floating mb-3">
                    <input type="text" class="form-control"
                           id="fullName" name="fullName"
                           placeholder="Nguyễn Văn A"
                           value="${fullName}"
                           minlength="2" maxlength="100"
                           required autofocus>
                    <label for="fullName">
                        <i class="bi bi-person"></i> Họ và tên
                    </label>
                </div>

                <%-- Email --%>
                <div class="form-floating mb-3">
                    <input type="email" class="form-control"
                           id="email" name="email"
                           placeholder="name@example.com"
                           value="${email}"
                           required>
                    <label for="email">
                        <i class="bi bi-envelope"></i> Email
                    </label>
                </div>

                <%-- Password --%>
                <div class="form-floating mb-1 password-toggle-wrapper">
                    <input type="password" class="form-control"
                           id="password" name="password"
                           placeholder="Mật khẩu"
                           minlength="8" maxlength="50"
                           required
                           oninput="checkPasswordStrength(this.value)">
                    <label for="password">
                        <i class="bi bi-lock"></i> Mật khẩu
                    </label>
                    <button type="button" class="password-toggle-btn"
                            onclick="togglePassword('password', this)">
                        <i class="bi bi-eye"></i>
                    </button>
                </div>

                <%-- Password strength indicator --%>
                <div class="password-strength">
                    <div class="password-strength-bar" id="strengthBar1"></div>
                    <div class="password-strength-bar" id="strengthBar2"></div>
                    <div class="password-strength-bar" id="strengthBar3"></div>
                </div>
                <div class="password-strength-text" id="strengthText">
                    Ít nhất 8 ký tự, có chữ hoa, chữ thường và số
                </div>

                <%-- Confirm Password --%>
                <div class="form-floating mb-3 mt-3 password-toggle-wrapper">
                    <input type="password" class="form-control"
                           id="confirmPassword" name="confirmPassword"
                           placeholder="Xác nhận mật khẩu"
                           required>
                    <label for="confirmPassword">
                        <i class="bi bi-lock-fill"></i> Xác nhận mật khẩu
                    </label>
                    <button type="button" class="password-toggle-btn"
                            onclick="togglePassword('confirmPassword', this)">
                        <i class="bi bi-eye"></i>
                    </button>
                </div>

                <button type="submit" class="btn-auth-submit">
                    <i class="bi bi-person-plus"></i> Tạo tài khoản
                </button>
            </form>

            <div class="auth-bottom-link">
                Đã có tài khoản?
                <a href="${pageContext.request.contextPath}/auth/login">
                    Đăng nhập
                </a>
            </div>
        </div>
    </div>
</div>

<%-- Scripts --%>
<script>
    function togglePassword(inputId, btn) {
        const input = document.getElementById(inputId);
        const icon = btn.querySelector('i');
        if (input.type === 'password') {
            input.type = 'text';
            icon.className = 'bi bi-eye-slash';
        } else {
            input.type = 'password';
            icon.className = 'bi bi-eye';
        }
    }

    /**
     * Kiểm tra độ mạnh mật khẩu và hiển thị indicator
     */
    function checkPasswordStrength(password) {
        const bar1 = document.getElementById('strengthBar1');
        const bar2 = document.getElementById('strengthBar2');
        const bar3 = document.getElementById('strengthBar3');
        const text = document.getElementById('strengthText');

        // Reset
        [bar1, bar2, bar3].forEach(b => b.className = 'password-strength-bar');

        let strength = 0;
        if (password.length >= 8) strength++;
        if (/[A-Z]/.test(password) && /[a-z]/.test(password)) strength++;
        if (/\d/.test(password) && password.length >= 10) strength++;

        if (strength === 0) {
            text.textContent = 'Ít nhất 8 ký tự, có chữ hoa, chữ thường và số';
            text.style.color = '#64748B';
        } else if (strength === 1) {
            bar1.classList.add('weak');
            text.textContent = 'Yếu — cần thêm chữ hoa/thường/số';
            text.style.color = '#EF4444';
        } else if (strength === 2) {
            bar1.classList.add('medium');
            bar2.classList.add('medium');
            text.textContent = 'Trung bình — thử thêm ký tự';
            text.style.color = '#F59E0B';
        } else {
            bar1.classList.add('strong');
            bar2.classList.add('strong');
            bar3.classList.add('strong');
            text.textContent = 'Mạnh! ✓';
            text.style.color = '#10B981';
        }
    }
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>