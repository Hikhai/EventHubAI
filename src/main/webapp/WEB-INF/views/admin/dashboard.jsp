<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle"    value="Dashboard" scope="request"/>
<c:set var="topbarTitle"  value="Dashboard" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">

        <%-- ===== PAGE HEADER ===== --%>
        <div class="page-header">
            <div>
                <h1><i class="bi bi-speedometer2"></i> Dashboard</h1>
                <div class="breadcrumb text-muted">
                    Tổng quan hệ thống — Cập nhật lúc
                    <%= new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                            .format(new java.util.Date()) %>
                </div>
            </div>
        </div>

        <%-- ===== KPI CARDS ===== --%>
        <div class="kpi-grid">
            <div class="kpi-card blue">
                <div class="kpi-card-icon"><i class="bi bi-calendar3"></i></div>
                <div class="kpi-card-label">Tổng sự kiện</div>
                <div class="kpi-card-value" data-count="${dashboard.totalEvents}">
                    ${dashboard.totalEvents}
                </div>
                <div class="kpi-card-hint">
                    <i class="bi bi-check-circle"></i>
                    ${dashboard.completedEvents} đã hoàn thành
                </div>
            </div>

            <div class="kpi-card green">
                <div class="kpi-card-icon"><i class="bi bi-broadcast"></i></div>
                <div class="kpi-card-label">Đang mở đăng ký</div>
                <div class="kpi-card-value" data-count="${dashboard.activeEvents}">
                    ${dashboard.activeEvents}
                </div>
                <div class="kpi-card-hint">Sắp diễn ra</div>
            </div>

            <div class="kpi-card orange">
                <div class="kpi-card-icon"><i class="bi bi-people-fill"></i></div>
                <div class="kpi-card-label">Tổng đăng ký</div>
                <div class="kpi-card-value" data-count="${dashboard.totalRegistrations}">
                    ${dashboard.totalRegistrations}
                </div>
                <div class="kpi-card-hint">
                    <i class="bi bi-person"></i>
                    ${dashboard.totalUsers} người dùng
                </div>
            </div>

            <div class="kpi-card purple">
                <div class="kpi-card-icon"><i class="bi bi-star-fill"></i></div>
                <div class="kpi-card-label">Đánh giá trung bình</div>
                <div class="kpi-card-value">
                    <fmt:formatNumber value="${dashboard.overallAvgRating}" pattern="0.0"/>
                    <span style="font-size:1rem; color:#94A3B8;">/5</span>
                </div>
                <div class="kpi-card-hint">
                    ${dashboard.totalReviews} lượt đánh giá
                </div>
            </div>
        </div>

        <%-- ===== CHARTS ===== --%>
        <div class="row g-3 mb-4">
            <div class="col-lg-7">
                <div class="admin-card h-100">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-graph-up-arrow"></i>
                            Đăng ký theo tháng
                        </h5>
                        <span class="text-muted small">12 tháng gần nhất</span>
                    </div>
                    <div class="chart-container">
                        <canvas id="registrationsChart"></canvas>
                    </div>
                </div>
            </div>

            <div class="col-lg-5">
                <div class="admin-card h-100">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-pie-chart"></i>
                            Sự kiện theo danh mục
                        </h5>
                    </div>
                    <div class="chart-container">
                        <canvas id="categoryChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <%-- ===== TABLES ===== --%>
        <div class="row g-3 mb-4">
            <div class="col-lg-8">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-trophy"></i>
                            Top sự kiện được đăng ký nhiều nhất
                        </h5>
                        <a href="${pageContext.request.contextPath}/admin/events"
                           class="text-decoration-none small">
                            Xem tất cả <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>

                    <c:choose>
                        <c:when test="${empty dashboard.topEvents}">
                            <div class="admin-empty">
                                <i class="bi bi-inbox"></i>
                                <p>Chưa có sự kiện nào</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <table class="admin-table">
                                <thead>
                                <tr>
                                    <th style="width:40px;">#</th>
                                    <th>Sự kiện</th>
                                    <th>Danh mục</th>
                                    <th style="width:120px;">Đã đăng ký</th>
                                    <th style="width:100px;">Tỷ lệ</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="event" items="${dashboard.topEvents}"
                                           varStatus="loop">
                                    <tr>
                                        <td><strong>${loop.count}</strong></td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/events/detail?id=${event.eventId}"
                                               class="text-decoration-none">
                                                    ${event.title}
                                            </a>
                                        </td>
                                        <td>
                                            <small class="text-muted">${event.categoryName}</small>
                                        </td>
                                        <td>
                                            <strong>${event.currentRegistered}</strong>
                                            <span class="text-muted">/${event.maxParticipants}</span>
                                        </td>
                                        <td>
                                            <div class="d-flex align-items-center gap-2">
                                                <div class="slot-progress flex-grow-1"
                                                     style="height:6px; margin:0;">
                                                    <div class="slot-progress-bar"
                                                         style="width:${event.fillRatePercent}%"></div>
                                                </div>
                                                <small class="text-muted">
                                                    <fmt:formatNumber value="${event.fillRatePercent}" pattern="0"/>%
                                                </small>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div class="col-lg-4">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-exclamation-triangle text-warning"></i>
                            Sắp hết chỗ
                        </h5>
                    </div>

                    <c:choose>
                        <c:when test="${empty dashboard.almostFullEvents}">
                            <div class="admin-empty">
                                <i class="bi bi-check-circle text-success"></i>
                                <p>Không có sự kiện nào sắp hết chỗ</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="event" items="${dashboard.almostFullEvents}"
                                       begin="0" end="4">
                                <div class="alert-item">
                                    <div class="alert-item-info">
                                        <div class="alert-item-title">
                                            <a href="${pageContext.request.contextPath}/events/detail?id=${event.eventId}"
                                               class="text-decoration-none text-inherit">
                                                    ${event.title}
                                            </a>
                                        </div>
                                        <div class="alert-item-meta">
                                            <i class="bi bi-calendar3"></i>
                                                ${event.formattedStartTime}
                                        </div>
                                    </div>
                                    <span class="alert-item-badge">
                                        ${event.currentRegistered}/${event.maxParticipants}
                                    </span>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- ===== RECENT ACTIVITY + TOP RATED ===== --%>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-clock-history"></i>
                            Đăng ký gần đây
                        </h5>
                    </div>

                    <c:choose>
                        <c:when test="${empty dashboard.recentRegistrations}">
                            <div class="admin-empty">
                                <i class="bi bi-people"></i>
                                <p>Chưa có đăng ký nào</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="reg" items="${dashboard.recentRegistrations}"
                                       begin="0" end="6">
                                <div class="activity-item">
                                    <div class="activity-avatar">
                                            ${fn:substring(reg.userFullName, 0, 1)}
                                    </div>
                                    <div class="activity-content">
                                        <div class="user-name">${reg.userFullName}</div>
                                        <div class="event-name">
                                            đã đăng ký <strong>${reg.eventTitle}</strong>
                                        </div>
                                        <div class="activity-time">
                                            <i class="bi bi-clock"></i>
                                                ${reg.formattedRegisteredAt}
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div class="col-lg-6">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <h5 class="admin-card-title">
                            <i class="bi bi-star-fill text-warning"></i>
                            Sự kiện được đánh giá cao
                        </h5>
                    </div>

                    <c:choose>
                        <c:when test="${empty dashboard.topRatedEvents}">
                            <div class="admin-empty">
                                <i class="bi bi-star"></i>
                                <p>Chưa có đánh giá nào</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="event" items="${dashboard.topRatedEvents}">
                                <div class="alert-item">
                                    <div class="alert-item-info">
                                        <div class="alert-item-title">
                                            <a href="${pageContext.request.contextPath}/events/detail?id=${event.eventId}"
                                               class="text-decoration-none text-inherit">
                                                    ${event.title}
                                            </a>
                                        </div>
                                        <div class="alert-item-meta">
                                            <span class="star-rating">
                                                <c:forEach var="i" begin="1" end="5">
                                                    <i class="bi ${i <= event.avgRating ? 'bi-star-fill' : 'bi-star'}"
                                                       style="color:#FCD34D; font-size:0.85rem;"></i>
                                                </c:forEach>
                                            </span>
                                            <span class="text-muted">
                                                (${event.totalReviews} đánh giá)
                                            </span>
                                        </div>
                                    </div>
                                    <span class="alert-item-badge warning">
                                        <fmt:formatNumber value="${event.avgRating}" pattern="0.0"/>
                                    </span>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

    </div>
</div>

<%-- ===== CHART.JS ===== --%>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>

<script>
    window.dashboardData = {
        registrationsByMonth: ${not empty regByMonthJson ? regByMonthJson : '[]'},
        eventsByCategory:     ${not empty byCategoryJson ? byCategoryJson : '[]'}
    };
</script>

<script src="${pageContext.request.contextPath}/assets/js/dashboard.js"></script>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>