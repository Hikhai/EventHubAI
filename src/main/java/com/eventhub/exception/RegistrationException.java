package com.eventhub.exception;

/** Lỗi liên quan đến đăng ký sự kiện */
public class RegistrationException extends BusinessException {
    public RegistrationException(String message) {
        super(message);
    }
}