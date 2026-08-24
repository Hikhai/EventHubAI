<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý thanh toán" scope="request"/>
<c:set var="topbarTitle" value="Quản lý thanh toán" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>
    <div class="admin-content">
        <div class="page-header">
            <div>
                <h1><i class="bi bi-credit-card-2-front"></i> Quản lý thanh toán</h1>
                <div class="breadcrumb text-muted">Đối soát giao dịch và hoàn tiền</div>
            </div>
        </div>

        <div class="kpi-grid payment-kpis">
            <div class="kpi-card blue">
                <div class="kpi-card-icon"><i class="bi bi-receipt"></i></div>
                <div class="kpi-card-label">Tổng giao dịch</div>
                <div class="kpi-card-value">${totalPayments}</div>
            </div>
            <div class="kpi-card green">
                <div class="kpi-card-icon"><i class="bi bi-cash-stack"></i></div>
                <div class="kpi-card-label">Doanh thu đã thanh toán</div>
                <div class="kpi-card-value payment-revenue">${paidRevenue}</div>
                <div class="kpi-card-hint">${paidCount} giao dịch thành công</div>
            </div>
            <div class="kpi-card orange">
                <div class="kpi-card-icon"><i class="bi bi-hourglass-split"></i></div>
                <div class="kpi-card-label">Đang chờ</div>
                <div class="kpi-card-value">${pendingCount}</div>
            </div>
            <div class="kpi-card purple">
                <div class="kpi-card-icon"><i class="bi bi-arrow-counterclockwise"></i></div>
                <div class="kpi-card-label">Chờ hoàn tiền</div>
                <div class="kpi-card-value">${refundCount}</div>
            </div>
        </div>

        <div class="admin-card">
            <form method="get" action="${pageContext.request.contextPath}/admin/payments" class="filter-bar mb-0">
                <select name="status" class="form-select">
                    <option value="">Tất cả trạng thái</option>
                    <option value="PENDING" ${selectedStatus == 'PENDING' ? 'selected' : ''}>Chờ thanh toán</option>
                    <option value="PAID" ${selectedStatus == 'PAID' ? 'selected' : ''}>Đã thanh toán</option>
                    <option value="FAILED" ${selectedStatus == 'FAILED' ? 'selected' : ''}>Thất bại</option>
                    <option value="CANCELLED" ${selectedStatus == 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
                    <option value="EXPIRED" ${selectedStatus == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
                    <option value="REFUND_PENDING" ${selectedStatus == 'REFUND_PENDING' ? 'selected' : ''}>Chờ hoàn tiền</option>
                    <option value="REFUNDED" ${selectedStatus == 'REFUNDED' ? 'selected' : ''}>Đã hoàn tiền</option>
                </select>
                <button class="btn-admin-primary" type="submit"><i class="bi bi-funnel"></i> Lọc</button>
                <a class="btn-admin-secondary" href="${pageContext.request.contextPath}/admin/payments">Reset</a>
            </form>
        </div>

        <div class="admin-card p-0">
            <c:choose>
                <c:when test="${empty payments}">
                    <div class="admin-empty p-5"><i class="bi bi-inbox"></i><h5>Không có giao dịch</h5></div>
                </c:when>
                <c:otherwise>
                    <div class="table-responsive">
                        <table class="admin-table mb-0 payment-admin-table">
                            <thead><tr>
                                <th>Mã giao dịch</th>
                                <th>Người mua</th>
                                <th>Sự kiện</th>
                                <th>Số tiền</th>
                                <th>Cổng</th>
                                <th>Trạng thái</th>
                                <th>Thời gian</th>
                                <th>Thao tác</th>
                            </tr></thead>
                            <tbody>
                            <c:forEach var="payment" items="${payments}">
                                <tr>
                                    <td>
                                        <code>${payment.paymentCode}</code>
                                        <c:if test="${not empty payment.providerTransactionId}">
                                            <small class="d-block text-muted">${payment.providerTransactionId}</small>
                                        </c:if>
                                    </td>
                                    <td><strong>${payment.userFullName}</strong><small class="d-block text-muted">${payment.userEmail}</small></td>
                                    <td>${payment.eventTitle}</td>
                                    <td><strong>${payment.formattedAmount}</strong></td>
                                    <td>${payment.provider}<c:if test="${not empty payment.bankCode}"><small class="d-block text-muted">${payment.bankCode}</small></c:if></td>
                                    <td><span class="payment-status ${payment.statusCssClass}">${payment.statusLabel}</span></td>
                                    <td><small>${payment.formattedCreatedAt}</small></td>
                                    <td>
                                        <c:if test="${payment.status == 'REFUND_PENDING'}">
                                            <form method="post"
                                                  action="${pageContext.request.contextPath}/admin/payments"
                                                  class="confirm-form"
                                                  data-confirm="Xác nhận bạn đã hoàn tiền thủ công cho giao dịch này?">
                                                <input type="hidden" name="paymentCode" value="${payment.paymentCode}">
                                                <button class="btn btn-sm btn-outline-success" type="submit">
                                                    Xác nhận hoàn
                                                </button>
                                            </form>
                                        </c:if>
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
