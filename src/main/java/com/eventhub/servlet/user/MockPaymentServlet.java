package com.eventhub.servlet.user;

import com.eventhub.exception.PaymentException;
import com.eventhub.model.Payment;
import com.eventhub.model.User;
import com.eventhub.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/** Trang checkout mô phỏng: thành công, thất bại hoặc hủy. */
@WebServlet("/user/payments/mock")
public class MockPaymentServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("loggedInUser");
        try {
            Payment payment = paymentService.getPaymentForUser(
                    req.getParameter("code"), user.getUserId());
            req.setAttribute("payment", payment);
            req.getRequestDispatcher("/WEB-INF/views/user/mock-payment.jsp")
                    .forward(req, resp);
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/user/payments");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute("loggedInUser");
        String code = req.getParameter("code");
        String action = req.getParameter("action");
        try {
            if ("success".equals(action)) {
                paymentService.completeMockPayment(code, user.getUserId());
                req.getSession().setAttribute("successMsg",
                        "Thanh toán thành công! Vé đã được xác nhận.");
            } else if ("failed".equals(action)) {
                paymentService.cancelMockPayment(code, user.getUserId(), true);
                req.getSession().setAttribute("errorMsg",
                        "Giao dịch mô phỏng thất bại. Chỗ giữ đã được trả lại.");
            } else {
                paymentService.cancelMockPayment(code, user.getUserId(), false);
                req.getSession().setAttribute("successMsg",
                        "Đã hủy thanh toán và trả lại chỗ giữ.");
            }
        } catch (PaymentException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Không thể xử lý giao dịch.");
        }
        resp.sendRedirect(req.getContextPath() + "/user/payments");
    }
}
