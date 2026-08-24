package com.eventhub.servlet.payment;

import com.eventhub.model.Payment;
import com.eventhub.payment.VnPayPaymentGateway;
import com.eventhub.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/** IPN công khai để VNPAY xác nhận giao dịch phía server. */
@WebServlet("/payment/vnpay/ipn")
public class VnPayIpnServlet extends HttpServlet {
    private final PaymentService paymentService = new PaymentService();
    private final VnPayPaymentGateway gateway = new VnPayPaymentGateway();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            if (!gateway.verifySignature(req.getParameterMap())) {
                write(resp, "97", "Invalid Checksum");
                return;
            }

            String code = req.getParameter("vnp_TxnRef");
            Payment payment = paymentService.getPayment(code);
            if (payment == null || !"VNPAY".equals(payment.getProvider())) {
                write(resp, "01", "Order not Found");
                return;
            }
            if (!amountMatches(payment, req.getParameter("vnp_Amount"))) {
                write(resp, "04", "Invalid Amount");
                return;
            }
            if (!payment.isPending()) {
                write(resp, "02", "Order already confirmed");
                return;
            }

            String responseCode = req.getParameter("vnp_ResponseCode");
            String transactionStatus = req.getParameter("vnp_TransactionStatus");
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                paymentService.completeVnPayPayment(code,
                        req.getParameter("vnp_TransactionNo"), responseCode,
                        req.getParameter("vnp_BankCode"));
            } else {
                paymentService.recordVnPayFailure(code, responseCode);
            }
            write(resp, "00", "Confirm Success");
        } catch (Exception e) {
            write(resp, "99", "Unknown error");
        }
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

    private void write(HttpServletResponse resp, String code, String message) throws IOException {
        resp.getWriter().write("{\"RspCode\":\"" + code +
                "\",\"Message\":\"" + message + "\"}");
    }
}
