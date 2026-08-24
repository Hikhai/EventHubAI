package com.eventhub.servlet.user;

import com.eventhub.model.Payment;
import com.eventhub.model.User;
import com.eventhub.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/** Lịch sử thanh toán của người dùng hiện tại. */
@WebServlet("/user/payments")
public class UserPaymentsServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("loggedInUser");
        try {
            List<Payment> payments = paymentService.getUserPayments(user.getUserId());
            for (Payment payment : payments) {
                if ("MOCK".equals(payment.getProvider())
                        && (payment.getCheckoutUrl() == null || payment.getCheckoutUrl().isBlank())) {
                    payment.setCheckoutUrl(req.getContextPath()
                            + "/user/payments/mock?code=" + payment.getPaymentCode());
                }
            }
            req.setAttribute("payments", payments);
            req.getRequestDispatcher("/WEB-INF/views/user/payment-history.jsp")
                    .forward(req, resp);
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Không thể tải lịch sử thanh toán.");
            resp.sendRedirect(req.getContextPath() + "/events");
        }
    }
}
