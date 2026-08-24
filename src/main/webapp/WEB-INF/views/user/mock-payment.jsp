<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Thanh toán mô phỏng" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<div class="payment-page">
    <div class="container">
        <div class="payment-checkout-card">
            <div class="mock-gateway-header">
                <div class="mock-logo"><i class="bi bi-credit-card-2-front"></i></div>
                <div>
                    <div class="fw-bold">EventHub MockPay</div>
                    <small>Môi trường mô phỏng dành cho demo</small>
                </div>
                <span class="sandbox-badge">SANDBOX</span>
            </div>

            <div class="payment-amount-block">
                <span>Số tiền thanh toán</span>
                <strong>${payment.formattedAmount}</strong>
            </div>

            <div class="payment-order-info">
                <div><span>Mã giao dịch</span><strong>${payment.paymentCode}</strong></div>
                <div><span>Sự kiện</span><strong>${payment.eventTitle}</strong></div>
                <div><span>Hạn giữ chỗ</span><strong>${payment.formattedExpiresAt}</strong></div>
                <div><span>Trạng thái</span>
                    <strong class="payment-status ${payment.statusCssClass}">${payment.statusLabel}</strong>
                </div>
            </div>

            <c:choose>
                <c:when test="${payment.pendingAndActive}">
                    <div class="mock-notice">
                        <i class="bi bi-info-circle"></i>
                        Đây là cổng mô phỏng, không phát sinh tiền thật. Hãy chọn một kết quả để test luồng callback.
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/user/payments/mock">
                        <input type="hidden" name="code" value="${payment.paymentCode}">
                        <div class="d-grid gap-2">
                            <button type="submit" name="action" value="success" class="btn btn-success btn-lg">
                                <i class="bi bi-check-circle"></i> Giả lập thanh toán thành công
                            </button>
                            <button type="submit" name="action" value="failed" class="btn btn-outline-danger">
                                <i class="bi bi-exclamation-triangle"></i> Giả lập giao dịch thất bại
                            </button>
                            <button type="submit" name="action" value="cancel" class="btn btn-link text-muted">
                                Hủy và quay lại
                            </button>
                        </div>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-secondary mb-3">
                        Giao dịch này đã được xử lý hoặc đã hết hạn.
                    </div>
                    <a href="${pageContext.request.contextPath}/user/payments"
                       class="btn btn-primary-gradient w-100">Xem lịch sử thanh toán</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
