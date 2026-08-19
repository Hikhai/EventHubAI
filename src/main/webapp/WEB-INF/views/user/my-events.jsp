<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sự kiện của tôi" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<%-- Header với gradient --%>
<div class="my-events-header">
    <div class="container">
        <h1 class="mb-2">
            <i class="bi bi-bookmark-star-fill"></i> Sự kiện của tôi
        </h1>
        <p class="mb-0" style="opacity:0.9;">
            Quản lý các sự kiện bạn đã đăng ký
        </p>
    </div>
</div>

<div class="container py-4">

    <%-- ===== TABS ===== --%>
    <div class="my-events-tabs">
        <button class="tab-btn active" data-tab="upcoming">
            <i class="bi bi-calendar-event"></i>
            Sắp diễn ra
            <span class="count-badge">${upcoming.size()}</span>
        </button>
        <button class="tab-btn" data-tab="attended">
            <i class="bi bi-check2-circle"></i>
            Đã tham gia
            <span class="count-badge">${attended.size()}</span>
        </button>
        <button class="tab-btn" data-tab="cancelled">
            <i class="bi bi-x-circle"></i>
            Đã hủy
            <span class="count-badge">${cancelled.size()}</span>
        </button>
    </div>

    <%-- ===== TAB 1: SẮP DIỄN RA ===== --%>
    <div class="tab-content active" id="tab-upcoming">
        <c:choose>
            <c:when test="${empty upcoming}">
                <div class="empty-state">
                    <i class="bi bi-calendar-x"></i>
                    <h5>Bạn chưa đăng ký sự kiện nào</h5>
                    <p>Khám phá các sự kiện thú vị đang chờ bạn!</p>
                    <a href="${pageContext.request.contextPath}/events"
                       class="btn btn-primary-gradient">
                        <i class="bi bi-search"></i> Tìm sự kiện
                    </a>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="reg" items="${upcoming}">
                    <div class="registration-list-item">
                        <img src="${pageContext.request.contextPath}/uploads/events/${reg.eventImagePath}"
                             class="event-thumb"
                             onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'"
                             alt="${reg.eventTitle}">
                        <div class="event-info">
                            <div class="event-title">${reg.eventTitle}</div>
                            <div class="event-meta-small">
                                <i class="bi bi-geo-alt"></i> ${reg.eventLocation}
                            </div>
                            <div class="event-meta-small">
                                <i class="bi bi-calendar3"></i>
                                    ${reg.formattedEventStartTime}
                            </div>
                        </div>
                        <div class="actions">
                            <a href="${pageContext.request.contextPath}/events/detail?id=${reg.eventId}"
                               class="btn btn-outline-primary btn-sm">
                                <i class="bi bi-eye"></i> Xem
                            </a>
                            <c:if test="${reg.eventUpcoming}">
                                <form method="post"
                                      action="${pageContext.request.contextPath}/user/cancel-event"
                                      class="confirm-form m-0"
                                      data-confirm="Bạn có chắc chắn muốn hủy đăng ký?">
                                    <input type="hidden" name="eventId" value="${reg.eventId}">
                                    <button type="submit" class="btn btn-outline-danger btn-sm">
                                        <i class="bi bi-x"></i> Hủy
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- ===== TAB 2: ĐÃ THAM GIA ===== --%>
    <div class="tab-content" id="tab-attended">
        <c:choose>
            <c:when test="${empty attended}">
                <div class="empty-state">
                    <i class="bi bi-emoji-neutral"></i>
                    <h5>Bạn chưa tham gia sự kiện nào</h5>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="reg" items="${attended}">
                    <div class="registration-list-item">
                        <img src="${pageContext.request.contextPath}/uploads/events/${reg.eventImagePath}"
                             class="event-thumb"
                             onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'"
                             alt="${reg.eventTitle}">
                        <div class="event-info">
                            <div class="event-title">${reg.eventTitle}</div>
                            <div class="event-meta-small">
                                <i class="bi bi-geo-alt"></i> ${reg.eventLocation}
                            </div>
                            <div class="event-meta-small">
                                <i class="bi bi-calendar3"></i>
                                    ${reg.formattedEventStartTime}
                            </div>

                                <%-- Đánh giá đã gửi hoặc form --%>
                            <c:choose>
                                <c:when test="${reviewMap[reg.eventId] != null}">
                                    <div class="mt-2">
                                        <span class="star-rating">
                                                ${reviewMap[reg.eventId].starsDisplay}
                                        </span>
                                        <small class="text-muted">- Đã đánh giá</small>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <%-- Link đến trang chi tiết để đánh giá --%>
                                    <div class="mt-2">
                                        <a href="${pageContext.request.contextPath}/events/detail?id=${reg.eventId}"
                                           class="text-warning small">
                                            <i class="bi bi-star"></i>
                                            Bạn chưa đánh giá — nhấn để đánh giá
                                        </a>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="actions">
                            <a href="${pageContext.request.contextPath}/events/detail?id=${reg.eventId}"
                               class="btn btn-outline-primary btn-sm">
                                <i class="bi bi-eye"></i> Xem
                            </a>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- ===== TAB 3: ĐÃ HỦY ===== --%>
    <div class="tab-content" id="tab-cancelled">
        <c:choose>
            <c:when test="${empty cancelled}">
                <div class="empty-state">
                    <i class="bi bi-inbox"></i>
                    <h5>Không có đăng ký nào bị hủy</h5>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach var="reg" items="${cancelled}">
                    <div class="registration-list-item">
                        <img src="${pageContext.request.contextPath}/uploads/events/${reg.eventImagePath}"
                             class="event-thumb"
                             onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'"
                             alt="${reg.eventTitle}">
                        <div class="event-info">
                            <div class="event-title">${reg.eventTitle}</div>
                            <div class="event-meta-small">
                                <i class="bi bi-geo-alt"></i> ${reg.eventLocation}
                            </div>
                            <div class="event-meta-small text-danger">
                                <i class="bi bi-x-circle"></i> Đã hủy
                            </div>
                        </div>
                        <div class="actions">
                            <a href="${pageContext.request.contextPath}/events/detail?id=${reg.eventId}"
                               class="btn btn-outline-primary btn-sm">
                                <i class="bi bi-arrow-clockwise"></i> Đăng ký lại
                            </a>
                        </div>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- ===== TAB SWITCHING SCRIPT ===== --%>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        tabBtns.forEach(function(btn) {
            btn.addEventListener('click', function() {
                const targetTab = btn.dataset.tab;

                // Xóa active khỏi tất cả
                tabBtns.forEach(b => b.classList.remove('active'));
                tabContents.forEach(c => c.classList.remove('active'));

                // Set active cho tab được click
                btn.classList.add('active');
                document.getElementById('tab-' + targetTab).classList.add('active');
            });
        });
    });
</script>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>