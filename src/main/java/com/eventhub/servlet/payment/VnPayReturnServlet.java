package com.eventhub.servlet.payment;

import com.eventhub.model.Payment;
import com.eventhub.payment.VnPayPaymentGateway;
import com.eventhub.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Trang VNPAY chuyển trình duyệt về sau thanh toán. */
@WebServlet("/payment/vnpay/return")
public class VnPayReturnServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();
    private final VnPayPaymentGateway gateway = new VnPayPaymentGateway();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String paymentCode = req.getParameter("vnp_TxnRef");
        boolean signatureValid = gateway.verifySignature(req.getParameterMap());
        String resultMessage;
        Payment payment = null;

        try {
            payment = paymentService.getPayment(paymentCode);
            if (!signatureValid) {
                resultMessage = "Chữ ký phản hồi không hợp lệ. Giao dịch không được cập nhật.";
            } else if (payment == null || !amountMatches(payment, req.getParameter("vnp_Amount"))) {
                resultMessage = "Không tìm thấy giao dịch hoặc số tiền không khớp.";
            } else if ("00".equals(req.getParameter("vnp_ResponseCode"))
                    && "00".equals(req.getParameter("vnp_TransactionStatus"))) {
                payment = paymentService.completeVnPayPayment(paymentCode,
                        req.getParameter("vnp_TransactionNo"),
                        req.getParameter("vnp_ResponseCode"),
                        req.getParameter("vnp_BankCode"));
                resultMessage = "Thanh toán thành công. Vé của bạn đã được xác nhận.";
            } else {
                payment = paymentService.recordVnPayFailure(paymentCode,
                        req.getParameter("vnp_ResponseCode"));
                resultMessage = "Thanh toán không thành công hoặc đã bị hủy.";
            }
        } catch (Exception e) {
            resultMessage = e.getMessage() == null
                    ? "Không thể xác nhận kết quả thanh toán." : e.getMessage();
        }

        req.setAttribute("payment", payment);
        req.setAttribute("signatureValid", signatureValid);
        req.setAttribute("resultMessage", resultMessage);
        req.getRequestDispatcher("/WEB-INF/views/user/payment-result.jsp")
                .forward(req, resp);
    }

    private boolean amountMatches(Payment payment, String vnpAmount) {
        try {
            BigInteger expected = payment.getAmount().multiply(BigDecimal.valueOf(100))
                    .toBigIntegerExact();
            return expected.equals(new BigInteger(vnpAmount));
        } catch (Exception e) {
            return false;
        }
    }
}
