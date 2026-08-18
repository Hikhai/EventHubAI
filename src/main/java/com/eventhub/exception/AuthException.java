package com.eventhub.exception;

/** Lỗi liên quan đến xác thực: đăng nhập, đăng ký */
public class AuthException extends BusinessException {
    public AuthException(String message) {
        super(message);
    }
}