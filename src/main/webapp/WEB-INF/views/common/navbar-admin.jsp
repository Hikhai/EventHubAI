<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
    Admin Sidebar Navigation
--%>

<%-- Lấy URL hiện tại để highlight menu active --%>
<c:set var="currentUri" value="${requestScope['jakarta.servlet.forward.request_uri']}"/>
<c:if test="${empty currentUri}">
    <c:set var="currentUri" value="${pageContext.request.requestURI}"/>
</c:if>

<aside class="admin-sidebar" id="adminSidebar">

    <%-- ===== BRAND ===== --%>
    <div class="sidebar-brand">
        <a href="${pageContext.request.contextPath}/admin/dashboard">
            <i class="bi bi-calendar-event-fill"></i>
            <span>EventHub AI</span>
        </a>
        <span class="sidebar-badge">Admin Panel</span>
    </div>

    <%-- ===== MENU ===== --%>
    <ul class="sidebar-menu">

        <li class="sidebar-menu-heading">TỔNG QUAN</li>

        <li class="sidebar-menu-item">
            <a href="${pageContext.request.contextPath}/admin/dashboard"
               class="sidebar-menu-link ${fn:contains(currentUri, '/admin/dashboard') ? 'active' : ''}">
                <i class="bi bi-speedometer2"></i>
                <span>Dashboard</span>
            </a>
        </li>

        <li class="sidebar-menu-heading">QUẢN LÝ</li>

        <li class="sidebar-menu-item">
            <a href="${pageContext.request.contextPath}/admin/events"
               class="sidebar-menu-link ${(fn:contains(currentUri, '/admin/events') && !fn:contains(currentUri, 'registrations')) ? 'active' : ''}">
                <i class="bi bi-calendar3"></i>
                <span>Sự kiện</span>
            </a>
        </li>

        <li class="sidebar-menu-item">
            <a href="${pageContext.request.contextPath}/admin/categories"
               class="sidebar-menu-link ${fn:contains(currentUri, '/admin/categories') ? 'active' : ''}">
                <i class="bi bi-tags"></i>
                <span>Danh mục</span>
            </a>
        </li>

        <li class="sidebar-menu-heading">KHÁC</li>

        <li class="sidebar-menu-item">
            <a href="${pageContext.request.contextPath}/events"
               class="sidebar-menu-link" target="_blank">
                <i class="bi bi-globe"></i>
                <span>Xem trang web</span>
                <i class="bi bi-box-arrow-up-right ms-auto" style="font-size:0.75rem;"></i>
            </a>
        </li>
    </ul>

    <%-- ===== FOOTER ===== --%>
    <div class="sidebar-footer">
        <form method="post"
              action="${pageContext.request.contextPath}/auth/logout"
              class="m-0">
            <button type="submit" class="sidebar-menu-link border-0 w-100 text-start"
                    style="background:transparent; cursor:pointer;">
                <i class="bi bi-box-arrow-right"></i>
                <span>Đăng xuất</span>
            </button>
        </form>
    </div>
</aside>