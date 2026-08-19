<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="${event.title}" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<%-- ===== HERO BANNER ===== --%>
<div class="event-hero-banner">
    <img src="${event.displayImagePath}" alt="${event.title}"
         onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'">
    <div class="event-hero-overlay"></div>
    <div class="event-hero-content">
        <div class="container">
            <span class="badge bg-primary">${event.categoryName}</span>
            <h1>${event.title}</h1>
            <c:if test="${event.totalReviews > 0}">
                <div class="mt-2">
                    <span class="star-rating">
                        <c:forEach var="i" begin="1" end="5">
                            <i class="bi ${i <= event.avgRating ? 'bi-star-fill' : 'bi-star'}"></i>
                        </c:forEach>
                    </span>
                    <span class="ms-2">
                        ${event.avgRating}/5 (${event.totalReviews} đánh giá)
                    </span>
                </div>
            </c:if>
        </div>
    </div>
</div>

<div class="container">

    <%-- Breadcrumb --%>
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/">Trang chủ</a>
            </li>
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/events">Sự kiện</a>
            </li>
            <li class="breadcrumb-item active">${event.title}</li>
        </ol>
    </nav>

    <div class="row">

        <%-- ===== CỘT TRÁI - Nội dung sự kiện ===== --%>
        <div class="col-lg-8">

            <%-- Meta info bar --%>
            <div class="event-meta-bar">
                <div class="row">
                    <div class="col-md-6">
                        <div class="event-meta-item">
                            <i class="bi bi-geo-alt-fill"></i>
                            <div>
                                <span class="meta-label">Địa điểm</span>
                                <span class="meta-value">${event.location}</span>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="event-meta-item">
                            <i class="bi bi-calendar-event-fill"></i>
                            <div>
                                <span class="meta-label">Thời gian</span>
                                <span class="meta-value">${event.formattedStartTime}</span>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="event-meta-item">
                            <i class="bi bi-clock-fill"></i>
                            <div>
                                <span class="meta-label">Kết thúc</span>
                                <span class="meta-value">${event.formattedEndTime}</span>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="event-meta-item">
                            <i class="bi bi-hourglass-split"></i>
                            <div>
                                <span class="meta-label">Hạn đăng ký</span>
                                <span class="meta-value">${event.formattedDeadline}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <%-- AI Summary (nếu có) --%>
            <c:if test="${not empty event.summaryAi}">
                <div class="ai-summary-box">
                    <div class="ai-label">
                        <i class="bi bi-robot"></i> TÓM TẮT BỞI GEMINI AI
                    </div>
                    <p class="mb-0">${event.summaryAi}</p>
                </div>
            </c:if>

            <%-- Mô tả chi tiết --%>
            <div class="event-description">
                <h3>📋 Về sự kiện</h3>
                <p style="white-space: pre-line;">${event.description}</p>
            </div>

            <%-- ===== REVIEWS SECTION ===== --%>
            <div class="reviews-section">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h3 class="mb-0">
                        <i class="bi bi-chat-square-text"></i> Đánh giá
                        <c:if test="${event.totalReviews > 0}">
                            <span class="text-muted fs-6">
                                (${event.totalReviews})
                            </span>
                        </c:if>
                    </h3>
                    <c:if test="${event.totalReviews > 0}">
                        <div>
                            <span class="star-rating">
                                <c:forEach var="i" begin="1" end="5">
                                    <i class="bi ${i <= event.avgRating ? 'bi-star-fill' : 'bi-star'}"></i>
                                </c:forEach>
                            </span>
                            <strong class="ms-2">${event.avgRating}/5</strong>
                        </div>
                    </c:if>
                </div>

                <%-- Form đánh giá (nếu user đủ điều kiện) --%>
                <c:if test="${sessionScope.loggedInUser != null
                              && !sessionScope.loggedInUser.admin
                              && event.ended
                              && userRegistration != null
                              && userRegistration.status == 'REGISTERED'
                              && userReview == null}">
                    <div class="alert alert-info">
                        <strong>✨ Bạn đã tham gia sự kiện này!</strong> Chia sẻ đánh giá của bạn:
                    </div>
                    <form method="post"
                          action="${pageContext.request.contextPath}/user/submit-review"
                          class="mb-4">
                        <input type="hidden" name="eventId" value="${event.eventId}">

                            <%-- Star rating input --%>
                        <div class="mb-3 text-center">
                            <div class="star-input">
                                <input type="radio" id="star5" name="rating" value="5" required>
                                <label for="star5">★</label>
                                <input type="radio" id="star4" name="rating" value="4">
                                <label for="star4">★</label>
                                <input type="radio" id="star3" name="rating" value="3">
                                <label for="star3">★</label>
                                <input type="radio" id="star2" name="rating" value="2">
                                <label for="star2">★</label>
                                <input type="radio" id="star1" name="rating" value="1">
                                <label for="star1">★</label>
                            </div>
                        </div>

                        <div class="mb-3">
                            <textarea class="form-control" name="comment" rows="3"
                                      placeholder="Chia sẻ trải nghiệm của bạn (10-500 ký tự, tùy chọn)..."
                                      minlength="10" maxlength="500"></textarea>
                        </div>

                        <button type="submit" class="btn btn-primary-gradient">
                            <i class="bi bi-send"></i> Gửi đánh giá
                        </button>
                    </form>
                    <hr>
                </c:if>

                <%-- Đã đánh giá rồi --%>
                <c:if test="${userReview != null}">
                    <div class="alert alert-success">
                        <strong>Bạn đã đánh giá sự kiện này:</strong>
                        <div class="mt-2">
                            <span class="star-rating">${userReview.starsDisplay}</span>
                        </div>
                        <c:if test="${not empty userReview.comment}">
                            <p class="mt-2 mb-0">"${userReview.comment}"</p>
                        </c:if>
                    </div>
                    <hr>
                </c:if>

                <%-- Danh sách reviews --%>
                <c:choose>
                    <c:when test="${empty reviews}">
                        <div class="empty-state">
                            <i class="bi bi-chat-square"></i>
                            <p>Chưa có đánh giá nào cho sự kiện này</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="review" items="${reviews}">
                            <div class="review-item">
                                <div class="review-header">
                                    <div>
                                        <div class="review-author">
                                            <i class="bi bi-person-circle"></i>
                                                ${review.userFullName}
                                        </div>
                                        <span class="star-rating">${review.starsDisplay}</span>
                                    </div>
                                    <span class="review-date">
                                            ${review.formattedCreatedAt}
                                    </span>
                                </div>
                                <c:if test="${not empty review.comment}">
                                    <p class="review-comment">${review.comment}</p>
                                </c:if>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- ===== CỘT PHẢI - SIDEBAR ĐĂNG KÝ ===== --%>
        <div class="col-lg-4">
            <div class="registration-card">
                <h4>🎫 Đăng ký tham gia</h4>

                <%-- Progress bar --%>
                <div class="slot-progress" style="height: 10px; margin: 1rem 0;">
                    <div class="slot-progress-bar"
                         style="width: ${event.fillRatePercent}%"></div>
                </div>

                <div class="registration-info">
                    <span class="label">Đã đăng ký</span>
                    <span class="value">${event.currentRegistered}/${event.maxParticipants}</span>
                </div>
                <div class="registration-info">
                    <span class="label">Còn lại</span>
                    <span class="value text-success">${event.availableSlots} chỗ</span>
                </div>
                <div class="registration-info">
                    <span class="label">Hạn đăng ký</span>
                    <span class="value">${event.formattedDeadline}</span>
                </div>

                <%-- ===== 8 CASE HIỂN THỊ NÚT ACTION ===== --%>
                <c:choose>
                    <%-- CASE 1: Guest chưa đăng nhập --%>
                    <c:when test="${sessionScope.loggedInUser == null}">
                        <a href="${pageContext.request.contextPath}/auth/login"
                           class="btn-register d-block text-center text-decoration-none">
                            <i class="bi bi-box-arrow-in-right"></i>
                            Đăng nhập để đăng ký
                        </a>
                    </c:when>

                    <%-- CASE 8: Admin xem trang --%>
                    <c:when test="${sessionScope.loggedInUser.admin}">
                        <a href="${pageContext.request.contextPath}/admin/events/edit?id=${event.eventId}"
                           class="btn-register d-block text-center text-decoration-none">
                            <i class="bi bi-pencil"></i> Chỉnh sửa sự kiện
                        </a>
                    </c:when>

                    <%-- CASE 5-6: User đã REGISTERED --%>
                    <c:when test="${userRegistration != null
                                    && userRegistration.status == 'REGISTERED'}">
                        <c:choose>
                            <c:when test="${event.ended}">
                                <%-- CASE 6: Đã kết thúc --%>
                                <div class="status-message attended">
                                    <i class="bi bi-check-circle-fill"></i>
                                    Bạn đã tham gia sự kiện này
                                </div>
                            </c:when>
                            <c:otherwise>
                                <%-- CASE 5: Chưa bắt đầu --%>
                                <div class="status-message registered">
                                    <i class="bi bi-check-circle-fill"></i>
                                    Bạn đã đăng ký sự kiện này
                                </div>
                                <c:if test="${event.upcoming}">
                                    <form method="post"
                                          action="${pageContext.request.contextPath}/user/cancel-event"
                                          class="confirm-form"
                                          data-confirm="Bạn có chắc chắn muốn hủy đăng ký?">
                                        <input type="hidden" name="eventId" value="${event.eventId}">
                                        <button type="submit" class="btn-cancel">
                                            <i class="bi bi-x-circle"></i> Hủy đăng ký
                                        </button>
                                    </form>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </c:when>

                    <%-- CASE 3: Hết chỗ --%>
                    <c:when test="${event.full}">
                        <div class="status-message full">
                            <i class="bi bi-exclamation-circle-fill"></i>
                            Sự kiện đã đủ người
                        </div>
                    </c:when>

                    <%-- CASE 4: Hết hạn --%>
                    <c:when test="${!event.registrationOpen}">
                        <div class="status-message expired">
                            <i class="bi bi-clock-history"></i>
                            Đã hết hạn đăng ký
                        </div>
                    </c:when>

                    <%-- CASE 2 & 7: User có thể đăng ký (mới hoặc đăng ký lại) --%>
                    <c:otherwise>
                        <form method="post"
                              action="${pageContext.request.contextPath}/user/register-event">
                            <input type="hidden" name="eventId" value="${event.eventId}">
                            <button type="submit" class="btn-register">
                                <c:choose>
                                    <c:when test="${userRegistration != null
                                                    && userRegistration.status == 'CANCELLED'}">
                                        <i class="bi bi-arrow-clockwise"></i> Đăng ký lại
                                    </c:when>
                                    <c:otherwise>
                                        <i class="bi bi-check-circle"></i> Đăng ký tham gia
                                    </c:otherwise>
                                </c:choose>
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <%-- ===== SỰ KIỆN TƯƠNG TỰ (full width, dưới cùng) ===== --%>
    <c:if test="${not empty similarEvents}">
        <div class="similar-events">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <h3 class="mb-0">
                    <i class="bi bi-collection"></i> Sự kiện tương tự
                </h3>
                <a href="${pageContext.request.contextPath}/events?categoryId=${event.categoryId}"
                   class="text-decoration-none">
                    Xem thêm <i class="bi bi-arrow-right"></i>
                </a>
            </div>
            <div class="row g-4">
                <c:forEach var="sim" items="${similarEvents}">
                    <div class="col-md-4">
                        <div class="event-card">
                            <div class="event-card-image">
                                <img src="${sim.displayImagePath}" alt="${sim.title}"
                                     onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'">
                                <span class="event-card-badge">${sim.categoryName}</span>
                            </div>
                            <div class="event-card-body">
                                <h5 class="event-card-title">${sim.title}</h5>
                                <div class="event-card-meta">
                                    <i class="bi bi-geo-alt"></i> ${sim.location}
                                </div>
                                <div class="event-card-meta">
                                    <i class="bi bi-calendar3"></i>
                                        ${sim.formattedStartTime}
                                </div>
                                <a href="${pageContext.request.contextPath}/events/detail?id=${sim.eventId}"
                                   class="btn btn-outline-primary mt-3 w-100">
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