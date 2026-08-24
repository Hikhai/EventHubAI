package com.eventhub.payment;

import com.eventhub.exception.PaymentException;
import com.eventhub.model.Payment;
import jakarta.servlet.http.HttpServletRequest;

/** Strategy tạo trang checkout cho từng cổng thanh toán. */
public interface PaymentGateway {
    String getProviderCode();

    String createCheckoutUrl(Payment payment, HttpServletRequest request)
            throws PaymentException;
}
