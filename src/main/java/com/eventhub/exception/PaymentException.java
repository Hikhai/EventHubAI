package com.eventhub.exception;

/** Lỗi nghiệp vụ liên quan đến giữ chỗ và thanh toán vé. */
public class PaymentException extends BusinessException {
    public PaymentException(String message) {
        super(message);
    }
}
