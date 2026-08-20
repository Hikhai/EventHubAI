<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="currentUri" value="${pageContext.request.requestURI}"/>

<nav class="navbar navbar-expand-lg navbar-custom" id="mainNavbar">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/">
            <span class="brand-mark"><i class="bi bi-calendar-event-fill"></i></span>
            EventHub AI
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#mainNav" aria-label="Mở menu">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link ${fn:contains(currentUri, '/events') ? 'active' : ''}"
                       href="${pageContext.request.contextPath}/events">
                        <i class="bi bi-calendar3"></i> Sự kiện
                    </a>
                </li>

                <c:if test="${sessionScope.loggedInUser != null && !sessionScope.loggedInUser.admin}">
                    <li class="nav-item">
                        <a class="nav-link ${fn:contains(currentUri, '/my-events') ? 'active' : ''}"
                           href="${pageContext.request.contextPath}/user/my-events">
                            <i class="bi bi-bookmark-star"></i> Sự kiện của tôi
                        </a>
                    </li>
                </c:if>
            </ul>

            <ul class="navbar-nav align-items-lg-center gap-lg-1">
                <li class="nav-item">
                    <button id="darkModeToggle" class="btn btn-link nav-link" title="Đổi giao diện" type="button">
                        <i id="darkModeIcon" class="bi bi-moon-fill"></i>
                    </button>
                </li>

                <c:choose>
                    <c:when test="${sessionScope.loggedInUser == null}">
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/auth/login">
                                Đăng nhập
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-primary-gradient ms-lg-2"
                               href="${pageContext.request.contextPath}/auth/register">
                                Đăng ký
                            </a>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle user-chip" href="#" data-bs-toggle="dropdown">
                                <span class="user-avatar">${fn:substring(sessionScope.loggedInUser.fullName, 0, 1)}</span>
                                ${sessionScope.loggedInUser.fullName}
                            </a>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <c:if test="${sessionScope.loggedInUser.admin}">
                                    <li>
                                        <a class="dropdown-item"
                                           href="${pageContext.request.contextPath}/admin/dashboard">
                                            <i class="bi bi-speedometer2"></i> Trang quản trị
                                        </a>
                                    </li>
                                    <li><hr class="dropdown-divider"></li>
                                </c:if>
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
                        </li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>

<c:if test="${not empty sessionScope.successMsg}">
    <div class="alert alert-success alert-flash alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle"></i> ${sessionScope.successMsg}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="successMsg" scope="session"/>
</c:if>

<c:if test="${not empty sessionScope.errorMsg}">
    <div class="alert alert-danger alert-flash alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle"></i> ${sessionScope.errorMsg}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="errorMsg" scope="session"/>
</c:if>
