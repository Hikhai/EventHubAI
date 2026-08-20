<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng nhập" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<div class="auth-container">

    <%-- ===== LEFT PANEL - HERO ===== --%>
    <div class="auth-hero">
        <div class="auth-hero-content">
            <h1><i class="bi bi-calendar-event-fill"></i> EventHub AI</h1>
            <p>
                Tìm sự kiện, đăng ký nhanh và nhận gợi ý phù hợp — tất cả trong một nơi.
            </p>

            <div class="auth-stats">
                <div class="stat-card">
                    <span class="stat-number">1,200+</span>
                    <span class="stat-label">🎉 Sự kiện đã tổ chức</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">5,000+</span>
                    <span class="stat-label">👥 Sinh viên tham gia</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">50+</span>
                    <span class="stat-label">🏢 Đối tác tổ chức</span>
                </div>
                <div class="stat-card">
                    <span class="stat-number">4.8/5</span>
                    <span class="stat-label">⭐ Đánh giá trung bình</span>
                </div>
            </div>
        </div>
    </div>

    <%-- ===== RIGHT PANEL - FORM ===== --%>
    <div class="auth-form-panel">
        <div class="auth-form">
            <h2>Chào mừng trở lại</h2>
            <p class="subtitle">Đăng nhập để tiếp tục khám phá sự kiện</p>

            <%-- Hiển thị lỗi nếu có --%>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle"></i> ${errorMsg}
                </div>
            </c:if>

            <%-- Form đăng nhập --%>
            <form method="post"
                  action="${pageContext.request.contextPath}/auth/login">

                <%-- Email --%>
                <div class="form-floating mb-3">
                    <input type="email" class="form-control"
                           id="email" name="email"
                           placeholder="name@example.com"
                           value="${email}"
                           required autofocus>
                    <label for="email">
                        <i class="bi bi-envelope"></i> Email
                    </label>
                </div>

                <%-- Password + toggle --%>
                <div class="form-floating mb-3 password-toggle-wrapper">
                    <input type="password" class="form-control"
                           id="password" name="password"
                           placeholder="Mật khẩu"
                           required>
                    <label for="password">
                        <i class="bi bi-lock"></i> Mật khẩu
                    </label>
                    <button type="button" class="password-toggle-btn"
                            onclick="togglePassword('password', this)">
                        <i class="bi bi-eye"></i>
                    </button>
                </div>

                <%-- Submit --%>
                <button type="submit" class="btn-auth-submit">
                    <i class="bi bi-box-arrow-in-right"></i> Đăng nhập
                </button>
            </form>

            <%-- Link đăng ký --%>
            <div class="auth-bottom-link">
                Chưa có tài khoản?
                <a href="${pageContext.request.contextPath}/auth/register">
                    Đăng ký ngay
                </a>
            </div>

            <%-- Info accounts mẫu (chỉ dev/demo) --%>
            <div class="demo-accounts">
                <strong>Tài khoản demo</strong><br>
                Admin: <code>admin@eventhub.com</code> / <code>Admin@123</code><br>
                User: <code>an@example.com</code> / <code>User@123</code>
            </div>
        </div>
    </div>
</div>

<%-- Script toggle password --%>
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
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>