<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle"    value="Quản lý sự kiện" scope="request"/>
<c:set var="topbarTitle"  value="Quản lý sự kiện" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">

        <%-- ===== PAGE HEADER ===== --%>
        <div class="page-header">
            <div>
                <h1><i class="bi bi-calendar3"></i> Quản lý sự kiện</h1>
                <div class="breadcrumb text-muted">
                    Tổng ${pagination.totalItems} sự kiện
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/admin/events/create"
               class="btn-admin-primary">
                <i class="bi bi-plus-lg"></i> Tạo sự kiện mới
            </a>
        </div>

        <%-- ===== FILTER BAR ===== --%>
        <div class="admin-card">
            <form method="get"
                  action="${pageContext.request.contextPath}/admin/events"
                  class="filter-bar">

                <input type="text" name="keyword"
                       value="${filter.keyword}"
                       class="form-control"
                       placeholder="Tìm theo tên sự kiện...">

                <select name="categoryId" class="form-select">
                    <option value="">Tất cả danh mục</option>
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat.categoryId}"
                            ${filter.categoryId == cat.categoryId ? 'selected' : ''}>
                                ${cat.categoryName}
                        </option>
                    </c:forEach>
                </select>

                <select name="status" class="form-select">
                    <option value="">Tất cả trạng thái</option>
                    <option value="DRAFT"     ${filter.status == 'DRAFT'     ? 'selected' : ''}>Nháp</option>
                    <option value="PUBLISHED" ${filter.status == 'PUBLISHED' ? 'selected' : ''}>Đang mở</option>
                    <option value="COMPLETED" ${filter.status == 'COMPLETED' ? 'selected' : ''}>Hoàn thành</option>
                    <option value="CANCELLED" ${filter.status == 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
                </select>

                <button type="submit" class="btn-admin-primary">
                    <i class="bi bi-search"></i> Lọc
                </button>

                <a href="${pageContext.request.contextPath}/admin/events"
                   class="btn-admin-secondary">
                    <i class="bi bi-arrow-clockwise"></i> Reset
                </a>
            </form>
        </div>

        <%-- ===== TABLE ===== --%>
        <div class="admin-card p-0">
            <c:choose>
                <c:when test="${empty events}">
                    <div class="admin-empty p-5">
                        <i class="bi bi-inbox"></i>
                        <h5>Không có sự kiện nào</h5>
                        <p>Nhấn "Tạo sự kiện mới" để bắt đầu</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="admin-table mb-0">
                            <thead>
                            <tr>
                                <th style="width:80px;">Ảnh</th>
                                <th>Sự kiện</th>
                                <th style="width:120px;">Danh mục</th>
                                <th style="width:140px;">Thời gian</th>
                                <th style="width:110px;">Đăng ký</th>
                                <th style="width:110px;">Trạng thái</th>
                                <th style="width:140px;">Hành động</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="event" items="${events}">
                                <tr>
                                        <%-- Ảnh --%>
                                    <td>
                                        <img src="${pageContext.request.contextPath}${event.displayImagePath}"
                                             class="event-thumb-small"
                                             onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'"
                                             alt="">
                                    </td>

                                        <%-- Tên + địa điểm --%>
                                    <td>
                                        <div class="fw-semibold">${event.title}</div>
                                        <small class="text-muted">
                                            <i class="bi bi-geo-alt"></i>
                                                ${event.location}
                                        </small>
                                        <c:if test="${event.imageSource == 'AI_GENERATED'}">
                                            <br>
                                            <small class="text-info">
                                                <i class="bi bi-robot"></i> Ảnh AI
                                            </small>
                                        </c:if>
                                    </td>

                                        <%-- Danh mục --%>
                                    <td>
                                            <span class="badge bg-secondary">
                                                    ${event.categoryName}
                                            </span>
                                    </td>

                                        <%-- Thời gian --%>
                                    <td>
                                        <small>${event.formattedStartTime}</small>
                                    </td>

                                        <%-- Đăng ký --%>
                                    <td>
                                        <strong>${event.currentRegistered}</strong>
                                        <span class="text-muted">/${event.maxParticipants}</span>
                                    </td>

                                        <%-- Trạng thái --%>
                                    <td>
                                        <c:choose>
                                            <c:when test="${event.status == 'PUBLISHED'}">
                                                    <span class="status-badge published">
                                                        <i class="bi bi-broadcast"></i> Đang mở
                                                    </span>
                                            </c:when>
                                            <c:when test="${event.status == 'DRAFT'}">
                                                    <span class="status-badge draft">
                                                        <i class="bi bi-file-earmark"></i> Nháp
                                                    </span>
                                            </c:when>
                                            <c:when test="${event.status == 'COMPLETED'}">
                                                    <span class="status-badge completed">
                                                        <i class="bi bi-check-circle"></i> Hoàn thành
                                                    </span>
                                            </c:when>
                                            <c:when test="${event.status == 'CANCELLED'}">
                                                    <span class="status-badge cancelled">
                                                        <i class="bi bi-x-circle"></i> Đã hủy
                                                    </span>
                                            </c:when>
                                        </c:choose>
                                    </td>

                                        <%-- Hành động --%>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/events/detail?id=${event.eventId}"
                                           class="btn-icon" title="Xem" target="_blank">
                                            <i class="bi bi-eye"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/admin/events/edit?id=${event.eventId}"
                                           class="btn-icon" title="Sửa">
                                            <i class="bi bi-pencil"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/admin/events/registrations?eventId=${event.eventId}"
                                           class="btn-icon" title="Người đăng ký">
                                            <i class="bi bi-people"></i>
                                        </a>
                                        <form method="post"
                                              action="${pageContext.request.contextPath}/admin/events/delete"
                                              class="d-inline confirm-form"
                                              data-confirm="Bạn chắc chắn muốn xóa sự kiện này? Nếu có người đăng ký, sự kiện sẽ bị hủy và tất cả đăng ký sẽ bị hủy theo.">
                                            <input type="hidden" name="eventId" value="${event.eventId}">
                                            <button type="submit" class="btn-icon btn-icon-danger" title="Xóa">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>

                    <%-- ===== PAGINATION ===== --%>
                    <c:if test="${pagination.totalPages > 1}">
                        <div class="p-3 border-top">
                            <nav>
                                <ul class="pagination mb-0 justify-content-center">
                                    <c:if test="${pagination.hasPrevious()}">
                                        <li class="page-item">
                                            <c:url var="prevUrl" value="/admin/events">
                                                <c:param name="page" value="${pagination.currentPage - 1}"/>
                                                <c:if test="${not empty filter.keyword}">
                                                    <c:param name="keyword" value="${filter.keyword}"/>
                                                </c:if>
                                                <c:if test="${not empty filter.categoryId}">
                                                    <c:param name="categoryId" value="${filter.categoryId}"/>
                                                </c:if>
                                                <c:if test="${not empty filter.status}">
                                                    <c:param name="status" value="${filter.status}"/>
                                                </c:if>
                                            </c:url>
                                            <a class="page-link" href="${prevUrl}">
                                                <i class="bi bi-chevron-left"></i>
                                            </a>
                                        </li>
                                    </c:if>

                                    <c:forEach var="i" begin="1" end="${pagination.totalPages}">
                                        <c:url var="pageUrl" value="/admin/events">
                                            <c:param name="page" value="${i}"/>
                                            <c:if test="${not empty filter.keyword}">
                                                <c:param name="keyword" value="${filter.keyword}"/>
                                            </c:if>
                                            <c:if test="${not empty filter.categoryId}">
                                                <c:param name="categoryId" value="${filter.categoryId}"/>
                                            </c:if>
                                            <c:if test="${not empty filter.status}">
                                                <c:param name="status" value="${filter.status}"/>
                                            </c:if>
                                        </c:url>
                                        <li class="page-item ${i == pagination.currentPage ? 'active' : ''}">
                                            <a class="page-link" href="${pageUrl}">${i}</a>
                                        </li>
                                    </c:forEach>

                                    <c:if test="${pagination.hasNext()}">
                                        <li class="page-item">
                                            <c:url var="nextUrl" value="/admin/events">
                                                <c:param name="page" value="${pagination.currentPage + 1}"/>
                                                <c:if test="${not empty filter.keyword}">
                                                    <c:param name="keyword" value="${filter.keyword}"/>
                                                </c:if>
                                                <c:if test="${not empty filter.categoryId}">
                                                    <c:param name="categoryId" value="${filter.categoryId}"/>
                                                </c:if>
                                                <c:if test="${not empty filter.status}">
                                                    <c:param name="status" value="${filter.status}"/>
                                                </c:if>
                                            </c:url>
                                            <a class="page-link" href="${nextUrl}">
                                                <i class="bi bi-chevron-right"></i>
                                            </a>
                                        </li>
                                    </c:if>
                                </ul>
                            </nav>
                        </div>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>