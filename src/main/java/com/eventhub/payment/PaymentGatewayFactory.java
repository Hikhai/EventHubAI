package com.eventhub.payment;

import com.eventhub.config.PaymentConfig;
import com.eventhub.exception.PaymentException;

/** Chọn cổng thanh toán từ PAYMENT_PROVIDER. */
public final class PaymentGatewayFactory {
    private PaymentGatewayFactory() {
    }

    public static PaymentGateway createConfiguredGateway() throws PaymentException {
        if ("VNPAY".equals(PaymentConfig.getProvider())) {
            if (!PaymentConfig.isVnPayConfigured()) {
                throw new PaymentException(
                        "VNPAY chưa được cấu hình. Hãy đặt VNPAY_TMN_CODE và VNPAY_HASH_SECRET."
                );
            }
            return new VnPayPaymentGateway();
        }
        return new MockPaymentGateway();
    }
}
