<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Kết quả thanh toán" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<div class="payment-page">
    <div class="container">
        <div class="payment-result-card">
            <c:choose>
                <c:when test="${payment != null && payment.paid}">
                    <div class="result-icon success"><i class="bi bi-check-lg"></i></div>
                    <h2>Thanh toán thành công</h2>
                </c:when>
                <c:otherwise>
                    <div class="result-icon failed"><i class="bi bi-x-lg"></i></div>
                    <h2>Chưa thể xác nhận thanh toán</h2>
                </c:otherwise>
            </c:choose>
            <p class="text-muted">${resultMessage}</p>

            <c:if test="${payment != null}">
                <div class="payment-order-info text-start mt-4">
                    <div><span>Mã giao dịch</span><strong>${payment.paymentCode}</strong></div>
                    <div><span>Sự kiện</span><strong>${payment.eventTitle}</strong></div>
                    <div><span>Số tiền</span><strong>${payment.formattedAmount}</strong></div>
                    <div><span>Trạng thái</span>
                        <strong class="payment-status ${payment.statusCssClass}">${payment.statusLabel}</strong>
                    </div>
                </div>
            </c:if>

            <div class="d-flex gap-2 justify-content-center mt-4">
                <a href="${pageContext.request.contextPath}/events" class="btn btn-outline-primary">Sự kiện</a>
                <c:if test="${sessionScope.loggedInUser != null && !sessionScope.loggedInUser.admin}">
                    <a href="${pageContext.request.contextPath}/user/payments" class="btn btn-primary-gradient">
                        Lịch sử thanh toán
                    </a>
                </c:if>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
