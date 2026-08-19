<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Navbar cho User/Guest
    Hiển thị link đăng nhập nếu chưa login, dropdown user nếu đã login
--%>

<nav class="navbar navbar-expand-lg navbar-custom">
    <div class="container">
        <%-- Logo/Brand --%>
        <a class="navbar-brand" href="${pageContext.request.contextPath}/">
            <i class="bi bi-calendar-event-fill"></i> EventHub AI
        </a>

        <%-- Mobile toggle --%>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#mainNav">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="mainNav">
            <%-- Menu chính --%>
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/events">
                        <i class="bi bi-calendar3"></i> Sự kiện
                    </a>
                </li>

                <%-- Chỉ hiển thị "My Events" nếu đã login và không phải admin --%>
                <c:if test="${sessionScope.loggedInUser != null
                              && !sessionScope.loggedInUser.admin}">
                    <li class="nav-item">
                        <a class="nav-link"
                           href="${pageContext.request.contextPath}/user/my-events">
                            <i class="bi bi-bookmark-star"></i> Sự kiện của tôi
                        </a>
                    </li>
                </c:if>
            </ul>

            <%-- Menu bên phải --%>
            <ul class="navbar-nav">
                <%-- Dark mode toggle --%>
                <li class="nav-item">
                    <button id="darkModeToggle" class="btn btn-link nav-link"
                            title="Chuyển giao diện">
                        <i id="darkModeIcon" class="bi bi-moon-fill"></i>
                    </button>
                </li>

                <c:choose>
                    <%-- Chưa đăng nhập --%>
                    <c:when test="${sessionScope.loggedInUser == null}">
                        <li class="nav-item">
                            <a class="nav-link"
                               href="${pageContext.request.contextPath}/auth/login">
                                Đăng nhập
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-primary-gradient ms-2"
                               href="${pageContext.request.contextPath}/auth/register">
                                Đăng ký
                            </a>
                        </li>
                    </c:when>

                    <%-- Đã đăng nhập --%>
                    <c:otherwise>
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle" href="#"
                               data-bs-toggle="dropdown">
                                <i class="bi bi-person-circle"></i>
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
                                        <%-- Logout dùng form POST --%>
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

<%-- ===== FLASH MESSAGES ===== --%>
<%-- Đọc từ session và XÓA ngay sau khi hiển thị --%>
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