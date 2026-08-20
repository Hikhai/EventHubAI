<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lỗi - EventHub AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <script>
        (function () {
            document.documentElement.setAttribute('data-bs-theme', localStorage.getItem('theme') || 'light');
        })();
    </script>
</head>
<body>
<div class="error-page">
    <div class="error-card text-center">
        <c:choose>
            <c:when test="${pageContext.errorData.statusCode == 404}">
                <i class="bi bi-compass" style="font-size: 3.6rem; color: #4F46E5;"></i>
                <h1 class="display-4 fw-bold mt-3">404</h1>
                <h2 class="h4 mb-2">Không tìm thấy trang</h2>
                <p class="text-muted mb-4">Trang bạn tìm không tồn tại hoặc đã được chuyển đi.</p>
            </c:when>
            <c:when test="${pageContext.errorData.statusCode == 403}">
                <i class="bi bi-shield-lock" style="font-size: 3.6rem; color: #EF4444;"></i>
                <h1 class="display-4 fw-bold mt-3">403</h1>
                <h2 class="h4 mb-2">Không có quyền truy cập</h2>
                <p class="text-muted mb-4">Bạn cần quyền phù hợp để xem trang này.</p>
            </c:when>
            <c:otherwise>
                <i class="bi bi-exclamation-triangle" style="font-size: 3.6rem; color: #F59E0B;"></i>
                <h1 class="display-4 fw-bold mt-3">500</h1>
                <h2 class="h4 mb-2">Đã có lỗi xảy ra</h2>
                <p class="text-muted mb-4">Xin lỗi, hệ thống gặp sự cố. Vui lòng thử lại sau.</p>
            </c:otherwise>
        </c:choose>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary-gradient">
            <i class="bi bi-house-door"></i> Về trang chủ
        </a>
    </div>
</div>
</body>
</html>
