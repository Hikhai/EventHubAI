<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Admin HEADER - riêng biệt với User header
    Layout hoàn toàn khác: sidebar + topbar
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="context-path" content="${pageContext.request.contextPath}">

    <title>${pageTitle != null ? pageTitle : 'Admin'} · EventHub AI</title>
    <meta name="theme-color" content="#0B1224">

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
          rel="stylesheet">

    <%-- Admin CSS --%>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/admin.css">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/chatbot.css">
    <script>
        (function () {
            document.documentElement.setAttribute('data-bs-theme', localStorage.getItem('theme') || 'light');
        })();
    </script>
</head>
<body>