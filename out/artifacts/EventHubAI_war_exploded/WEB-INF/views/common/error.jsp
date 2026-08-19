<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Lỗi - EventHub AI</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
          rel="stylesheet">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="container text-center py-5">
    <div class="mb-4">
        <%-- Icon và mã lỗi tùy theo status --%>
        <c:choose>
            <c:when test="${pageContext.errorData.statusCode == 404}">
                <i class="bi bi-search" style="font-size: 5rem; color: #6366F1;"></i>
                <h1 class="display-1 gradient-text">404</h1>
                <h2>Không tìm thấy trang</h2>
                <p class="lead text-muted">
                    Trang bạn tìm kiếm không tồn tại hoặc đã bị di chuyển.
                </p>
            </c:when>
            <c:when test="${pageContext.errorData.statusCode == 403}">
                <i class="bi bi-shield-lock" style="font-size: 5rem; color: #EF4444;"></i>
                <h1 class="display-1 gradient-text">403</h1>
                <h2>Không có quyền truy cập</h2>
                <p class="lead text-muted">
                    Bạn không có quyền truy cập trang này.
                </p>
            </c:when>
            <c:otherwise>
                <i class="bi bi-exclamation-triangle" style="font-size: 5rem; color: #F59E0B;"></i>
                <h1 class="display-1 gradient-text">500</h1>
                <h2>Đã có lỗi xảy ra</h2>
                <p class="lead text-muted">
                    Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.
                </p>
            </c:otherwise>
        </c:choose>
    </div>

    <a href="${pageContext.request.contextPath}/" class="btn btn-primary-gradient">
        <i class="bi bi-house-door"></i> Về trang chủ
    </a>
</div>
</body>
</html>