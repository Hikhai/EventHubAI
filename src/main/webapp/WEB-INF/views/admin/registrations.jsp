<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<c:set var="pageTitle"   value="Danh sách đăng ký" scope="request"/>
<c:set var="topbarTitle" value="Danh sách đăng ký" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">

        <%-- PAGE HEADER --%>
        <div class="page-header">
            <div>
                <h1><i class="bi bi-people-fill"></i> Danh sách người đăng ký</h1>
                <div class="breadcrumb text-muted">
                    Sự kiện: <strong>${event.title}</strong>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/admin/events" class="btn-admin-secondary">
                <i class="bi bi-arrow-left"></i> Quay lại sự kiện
            </a>
        </div>

        <%-- THÔNG TIN TÓM TẮT SỰ KIỆN --%>
        <div class="admin-card mb-4">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h5 class="fw-bold mb-2">${event.title}</h5>
                    <p class="text-muted mb-1"><i class="bi bi-geo-alt"></i> ${event.location}</p>
                    <p class="text-muted mb-0"><i class="bi bi-calendar3"></i> ${event.formattedStartTime}</p>
                </div>
                <div class="col-md-4 text-md-end mt-3 mt-md-0">
                    <div class="fs-4 fw-bold text-primary">
                        ${event.currentRegistered} / ${event.maxParticipants}
                    </div>
                    <small class="text-muted">Chỗ đã đăng ký</small>
                </div>
            </div>
        </div>

        <%-- BẢNG DANH SÁCH NGƯỜI ĐĂNG KÝ --%>
        <div class="admin-card p-0">
            <c:choose>
                <c:when test="${empty registrations}">
                    <div class="admin-empty p-5">
                        <i class="bi bi-people"></i>
                        <h5>Chưa có người dùng nào đăng ký sự kiện này</h5>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="admin-table mb-0">
                            <thead>
                            <tr>
                                <th style="width: 50px;">#</th>
                                <th>Họ và tên</th>
                                <th>Email</th>
                                <th>Thời gian đăng ký</th>
                                <th style="width: 140px;">Trạng thái</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="reg" items="${registrations}" varStatus="loop">
                                <tr>
                                    <td><strong>${loop.count}</strong></td>
                                    <td>
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="activity-avatar" style="width:32px; height:32px; font-size:0.8rem;">
                                                    ${fn:substring(reg.userFullName, 0, 1)}
                                            </div>
                                            <span class="fw-semibold">${reg.userFullName}</span>
                                        </div>
                                    </td>
                                    <td>${reg.userEmail}</td>
                                    <td>
                                        <i class="bi bi-clock text-muted"></i>
                                            ${reg.formattedRegisteredAt}
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${reg.status == 'REGISTERED'}">
                                                <span class="status-badge published">Đã đăng ký</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="status-badge cancelled">Đã hủy</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>