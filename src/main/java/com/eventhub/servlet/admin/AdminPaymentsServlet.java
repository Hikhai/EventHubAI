package com.eventhub.servlet.admin;

import com.eventhub.model.Payment;
import com.eventhub.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Danh sách và thống kê giao dịch dành cho Admin. */
@WebServlet("/admin/payments")
public class AdminPaymentsServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String status = req.getParameter("status");
            List<Payment> payments = paymentService.getAdminPayments(status);
            List<Payment> allPayments = paymentService.getAdminPayments(null);
            long paidCount = allPayments.stream().filter(Payment::isPaid).count();
            long pendingCount = allPayments.stream().filter(Payment::isPending).count();
            long refundCount = allPayments.stream()
                    .filter(p -> "REFUND_PENDING".equals(p.getStatus())).count();
            NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
            format.setMaximumFractionDigits(0);

            req.setAttribute("payments", payments);
            req.setAttribute("selectedStatus", status);
            req.setAttribute("totalPayments", allPayments.size());
            req.setAttribute("paidCount", paidCount);
            req.setAttribute("pendingCount", pendingCount);
            req.setAttribute("refundCount", refundCount);
            req.setAttribute("paidRevenue",
                    format.format(paymentService.calculatePaidRevenue(allPayments)) + " ₫");
            req.getRequestDispatcher("/WEB-INF/views/admin/payment-list.jsp")
                    .forward(req, resp);
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Không thể tải danh sách thanh toán.");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            paymentService.confirmManualRefund(req.getParameter("paymentCode"));
            req.getSession().setAttribute("successMsg",
                    "Đã xác nhận hoàn tiền cho giao dịch.");
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/admin/payments?status=REFUND_PENDING");
    }
}
