<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Sự kiện" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<%-- ===== HERO SECTION VỚI SEARCH ===== --%>
<section class="hero-section">
    <span class="hero-orb one"></span>
    <span class="hero-orb two"></span>
    <div class="container">
        <div class="hero-kicker" data-aos="fade-up">
            <i class="bi bi-stars"></i> Gợi ý thông minh với AI
        </div>
        <h1 data-aos="fade-up">Khám phá sự kiện nổi bật</h1>
        <p class="subtitle" data-aos="fade-up" data-aos-delay="80">
            Tìm, lọc và đăng ký sự kiện phù hợp — nhanh, rõ ràng, thân thiện.
        </p>

        <%-- Search form --%>
        <form method="get" action="${pageContext.request.contextPath}/events"
              class="hero-search" data-aos="fade-up" data-aos-delay="200">
            <div class="input-group">
                <input type="text" class="form-control"
                       name="keyword"
                       value="${keyword}"
                       placeholder="Tìm kiếm sự kiện..."
                       aria-label="Search">
                <button class="btn" type="submit">
                    <i class="bi bi-search"></i> Tìm
                </button>
            </div>
            <%-- Giữ categoryId khi search --%>
            <c:if test="${not empty categoryId}">
                <input type="hidden" name="categoryId" value="${categoryId}">
            </c:if>
        </form>

        <%-- Filter pills theo danh mục --%>
        <div class="filter-pills" data-aos="fade-up" data-aos-delay="300">
            <a href="${pageContext.request.contextPath}/events${not empty keyword ? '?keyword='.concat(keyword) : ''}"
               class="filter-pill ${empty categoryId ? 'active' : ''}">
                Tất cả
            </a>
            <c:forEach var="cat" items="${categories}">
                <c:url var="catUrl" value="/events">
                    <c:param name="categoryId" value="${cat.categoryId}"/>
                    <c:if test="${not empty keyword}">
                        <c:param name="keyword" value="${keyword}"/>
                    </c:if>
                </c:url>
                <a href="${catUrl}"
                   class="filter-pill ${categoryId == cat.categoryId ? 'active' : ''}">
                        ${cat.categoryName}
                </a>
            </c:forEach>
        </div>
    </div>
</section>

<%-- ===== DANH SÁCH SỰ KIỆN ===== --%>
<div class="container py-5">

    <%-- Thông tin kết quả --%>
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h4 class="mb-1">
                <c:choose>
                    <c:when test="${not empty keyword}">
                        Kết quả cho "<span class="gradient-text">${keyword}</span>"
                    </c:when>
                    <c:otherwise>
                        Sự kiện hiện có
                    </c:otherwise>
                </c:choose>
            </h4>
            <small class="text-muted">
                Tìm thấy ${pagination.totalItems} sự kiện
            </small>
        </div>
    </div>

    <%-- Grid các sự kiện --%>
    <c:choose>
        <c:when test="${empty events}">
            <%-- Không có sự kiện --%>
            <div class="empty-state">
                <i class="bi bi-calendar-x"></i>
                <h4>Không tìm thấy sự kiện nào</h4>
                <p>Thử thay đổi từ khóa hoặc bộ lọc</p>
                <a href="${pageContext.request.contextPath}/events"
                   class="btn btn-primary-gradient mt-2">
                    Xem tất cả sự kiện
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <div class="row g-4">
                <c:forEach var="event" items="${events}" varStatus="loop">
                    <div class="col-md-6 col-lg-4"
                         data-aos="fade-up"
                         data-aos-delay="${(loop.index % 3) * 100}">
                        <div class="event-card">
                                <%-- Image + badge danh mục --%>
                            <div class="event-card-image">
                                <img src="${pageContext.request.contextPath}${event.displayImagePath}"
                                     alt="${event.title}"
                                     onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'">
                                <span class="event-card-badge">
                                        ${event.categoryName}
                                </span>
                            </div>

                            <div class="event-card-body">
                                    <%-- Tiêu đề --%>
                                <h5 class="event-card-title">${event.title}</h5>

                                    <%-- Mô tả (AI summary nếu có, không thì cắt description) --%>
                                <p class="event-card-summary">
                                    <c:choose>
                                        <c:when test="${not empty event.summaryAi}">
                                            ${event.summaryAi}
                                        </c:when>
                                        <c:otherwise>
                                            ${fn:substring(event.description, 0, 150)}
                                            <c:if test="${fn:length(event.description) > 150}">...</c:if>
                                        </c:otherwise>
                                    </c:choose>
                                </p>

                                    <%-- Meta info --%>
                                <div class="event-card-meta">
                                    <i class="bi bi-geo-alt"></i>
                                    <span>${event.location}</span>
                                </div>
                                <div class="event-card-meta">
                                    <i class="bi bi-calendar3"></i>
                                    <span>${event.formattedStartTime}</span>
                                </div>

                                    <%-- Progress bar chỗ --%>
                                <div class="slot-progress">
                                    <div class="slot-progress-bar"
                                         style="width: ${event.fillRatePercent}%"></div>
                                </div>
                                <div class="d-flex justify-content-between align-items-center mt-1">
                                    <small class="text-muted">
                                            ${event.currentRegistered}/${event.maxParticipants} chỗ
                                    </small>
                                        <%-- Badge trạng thái chỗ --%>
                                    <c:choose>
                                        <c:when test="${event.full}">
                                            <span class="badge-status badge-full">
                                                Hết chỗ
                                            </span>
                                        </c:when>
                                        <c:when test="${event.fillRatePercent >= 80}">
                                            <span class="badge-status badge-almost-full">
                                                Sắp hết
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge-status badge-available">
                                                Còn chỗ
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                    <%-- Nút xem chi tiết --%>
                                <a href="${pageContext.request.contextPath}/events/detail?id=${event.eventId}"
                                   class="btn btn-primary-gradient mt-3 w-100">
                                    Xem chi tiết <i class="bi bi-arrow-right"></i>
                                </a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <%-- ===== PHÂN TRANG ===== --%>
            <c:if test="${pagination.totalPages > 1}">
                <nav aria-label="Phân trang">
                    <ul class="pagination">
                            <%-- Previous --%>
                        <c:if test="${pagination.hasPrevious()}">
                            <li class="page-item">
                                <c:url var="prevUrl" value="/events">
                                    <c:param name="page" value="${pagination.currentPage - 1}"/>
                                    <c:if test="${not empty keyword}">
                                        <c:param name="keyword" value="${keyword}"/>
                                    </c:if>
                                    <c:if test="${not empty categoryId}">
                                        <c:param name="categoryId" value="${categoryId}"/>
                                    </c:if>
                                </c:url>
                                <a class="page-link" href="${prevUrl}">
                                    <i class="bi bi-chevron-left"></i>
                                </a>
                            </li>
                        </c:if>

                            <%-- Các trang --%>
                        <c:forEach var="i" begin="1" end="${pagination.totalPages}">
                            <c:url var="pageUrl" value="/events">
                                <c:param name="page" value="${i}"/>
                                <c:if test="${not empty keyword}">
                                    <c:param name="keyword" value="${keyword}"/>
                                </c:if>
                                <c:if test="${not empty categoryId}">
                                    <c:param name="categoryId" value="${categoryId}"/>
                                </c:if>
                            </c:url>
                            <li class="page-item ${i == pagination.currentPage ? 'active' : ''}">
                                <a class="page-link" href="${pageUrl}">${i}</a>
                            </li>
                        </c:forEach>

                            <%-- Next --%>
                        <c:if test="${pagination.hasNext()}">
                            <li class="page-item">
                                <c:url var="nextUrl" value="/events">
                                    <c:param name="page" value="${pagination.currentPage + 1}"/>
                                    <c:if test="${not empty keyword}">
                                        <c:param name="keyword" value="${keyword}"/>
                                    </c:if>
                                    <c:if test="${not empty categoryId}">
                                        <c:param name="categoryId" value="${categoryId}"/>
                                    </c:if>
                                </c:url>
                                <a class="page-link" href="${nextUrl}">
                                    <i class="bi bi-chevron-right"></i>
                                </a>
                            </li>
                        </c:if>
                    </ul>
                </nav>
            </c:if>
        </c:otherwise>
    </c:choose>

    <%-- ===== GỢI Ý SỰ KIỆN ===== --%>
    <c:if test="${not empty recommendations}">
        <div class="mt-5">
            <h3 class="mb-4 section-heading">
                <i class="bi bi-lightbulb-fill text-warning"></i>
                Có thể bạn quan tâm
            </h3>
            <div class="row g-4">
                <c:forEach var="rec" items="${recommendations}" begin="0" end="2">
                    <div class="col-md-4">
                        <div class="event-card">
                            <div class="event-card-image">
                                <img src="${pageContext.request.contextPath}${rec.displayImagePath}" alt="${rec.title}"
                                     onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'">
                                <span class="event-card-badge">${rec.categoryName}</span>
                            </div>
                            <div class="event-card-body">
                                <h5 class="event-card-title">${rec.title}</h5>
                                <div class="event-card-meta">
                                    <i class="bi bi-calendar3"></i>
                                        ${rec.formattedStartTime}
                                </div>
                                <a href="${pageContext.request.contextPath}/events/detail?id=${rec.eventId}"
                                   class="btn btn-outline-primary mt-2 w-100">
                                    Xem chi tiết
                                </a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:if>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>