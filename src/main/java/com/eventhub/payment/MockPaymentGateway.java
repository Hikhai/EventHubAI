package com.eventhub.payment;

import com.eventhub.exception.PaymentException;
import com.eventhub.model.Payment;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Cổng mô phỏng để demo ổn định mà không dùng tiền thật. */
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public String getProviderCode() {
        return "MOCK";
    }

    @Override
    public String createCheckoutUrl(Payment payment, HttpServletRequest request)
            throws PaymentException {
        if (payment == null || payment.getPaymentCode() == null) {
            throw new PaymentException("Không thể tạo trang thanh toán mô phỏng.");
        }
        return request.getContextPath() + "/user/payments/mock?code=" +
                URLEncoder.encode(payment.getPaymentCode(), StandardCharsets.UTF_8);
    }
}
