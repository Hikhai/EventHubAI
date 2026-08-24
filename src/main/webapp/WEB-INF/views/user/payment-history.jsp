<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Lịch sử thanh toán" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-user.jsp"/>

<div class="my-events-page">
    <div class="my-events-header">
        <div class="container">
            <h1 class="mb-2 fw-bold"><i class="bi bi-receipt"></i> Lịch sử thanh toán</h1>
            <p class="mb-0" style="opacity:.9">Theo dõi trạng thái các lần mua vé của bạn.</p>
        </div>
    </div>

    <div class="container my-events-body">
        <c:choose>
            <c:when test="${empty payments}">
                <div class="empty-state">
                    <i class="bi bi-wallet2"></i>
                    <h5>Bạn chưa có giao dịch nào</h5>
                    <a href="${pageContext.request.contextPath}/events" class="btn btn-primary-gradient mt-2">
                        Khám phá sự kiện
                    </a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="payment-history-list">
                    <c:forEach var="payment" items="${payments}">
                        <div class="payment-history-card">
                            <div class="payment-history-icon ${payment.statusCssClass}">
                                <i class="bi ${payment.paid ? 'bi-check-lg' : 'bi-credit-card'}"></i>
                            </div>
                            <div class="payment-history-main">
                                <div class="d-flex flex-wrap justify-content-between gap-2">
                                    <div>
                                        <h5>${payment.eventTitle}</h5>
                                        <div class="payment-code">${payment.paymentCode}</div>
                                    </div>
                                    <div class="text-end">
                                        <div class="payment-history-amount">${payment.formattedAmount}</div>
                                        <span class="payment-status ${payment.statusCssClass}">
                                            ${payment.statusLabel}
                                        </span>
                                    </div>
                                </div>
                                <div class="payment-history-meta">
                                    <span><i class="bi bi-building"></i> ${payment.provider}</span>
                                    <span><i class="bi bi-clock"></i> ${payment.formattedCreatedAt}</span>
                                    <c:if test="${not empty payment.bankCode}">
                                        <span><i class="bi bi-bank"></i> ${payment.bankCode}</span>
                                    </c:if>
                                </div>
                                <c:if test="${payment.pendingAndActive}">
                                    <a href="${payment.checkoutUrl}" class="btn btn-sm btn-primary mt-3">
                                        Tiếp tục thanh toán
                                    </a>
                                </c:if>
                                <c:if test="${not empty payment.failureReason}">
                                    <div class="small text-muted mt-2">${payment.failureReason}</div>
                                </c:if>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer.jsp"/>
