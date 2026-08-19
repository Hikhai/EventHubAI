<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Admin Topbar
    Cần truyền attribute "topbarTitle" từ JSP page cha
--%>

<header class="admin-topbar">
    <div class="d-flex align-items-center gap-3">
        <button class="topbar-toggle" id="sidebarToggle" title="Menu">
            <i class="bi bi-list"></i>
        </button>
        <div>
            <h1 class="topbar-title">${topbarTitle != null ? topbarTitle : 'Admin'}</h1>
        </div>
    </div>

    <div class="topbar-actions">
        <%-- Dark mode toggle --%>
        <button id="darkModeToggle" class="topbar-btn" title="Đổi giao diện">
            <i id="darkModeIcon" class="bi bi-moon-fill"></i>
        </button>

        <%-- User info --%>
        <div class="dropdown">
            <a href="#" class="topbar-user dropdown-toggle text-decoration-none"
               data-bs-toggle="dropdown">
                <div class="avatar">
                    <c:out value="${fn:substring(sessionScope.loggedInUser.fullName, 0, 1)}"
                           xmlns:fn="jakarta.tags.functions"/>
                </div>
                <span class="d-none d-md-inline">
                    ${sessionScope.loggedInUser.fullName}
                </span>
            </a>
            <ul class="dropdown-menu dropdown-menu-end">
                <li>
                    <a class="dropdown-item"
                       href="${pageContext.request.contextPath}/events" target="_blank">
                        <i class="bi bi-globe"></i> Xem trang web
                    </a>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <form method="post"
                          action="${pageContext.request.contextPath}/auth/logout"
                          class="m-0">
                        <button type="submit" class="dropdown-item text-danger">
                            <i class="bi bi-box-arrow-right"></i> Đăng xuất
                        </button>
                    </form>
                </li>
            </ul>
        </div>
    </div>
</header>

<%-- ===== FLASH MESSAGES ===== --%>
<c:if test="${not empty sessionScope.successMsg}">
    <div class="alert alert-success alert-flash alert-dismissible fade show m-3" role="alert">
        <i class="bi bi-check-circle"></i> ${sessionScope.successMsg}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="successMsg" scope="session"/>
</c:if>

<c:if test="${not empty sessionScope.errorMsg}">
    <div class="alert alert-danger alert-flash alert-dismissible fade show m-3" role="alert">
        <i class="bi bi-exclamation-triangle"></i> ${sessionScope.errorMsg}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="errorMsg" scope="session"/>
</c:if>