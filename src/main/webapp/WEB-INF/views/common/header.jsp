<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Common HEADER - Include ở đầu mọi trang User
    Chứa: meta, title, CSS/JS imports
    Cách dùng: <jsp:include page="/WEB-INF/views/common/header.jsp"/>
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%-- Meta chứa context path để JS đọc --%>
    <meta name="context-path" content="${pageContext.request.contextPath}">

    <title>${pageTitle != null ? pageTitle : 'EventHub AI'} - EventHub AI</title>

    <%-- Bootstrap 5.3.3 CSS --%>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">

    <%-- Bootstrap Icons --%>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
          rel="stylesheet">

    <%-- AOS Animation --%>
    <link href="https://cdn.jsdelivr.net/npm/aos@2.3.4/dist/aos.css"
          rel="stylesheet">

    <%-- Custom CSS --%>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/style.css">
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/chatbot.css">
</head>
<body>