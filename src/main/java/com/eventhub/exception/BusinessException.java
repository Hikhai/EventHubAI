package com.eventhub.exception;

/**
 * Base exception cho tất cả lỗi nghiệp vụ trong hệ thống.
 * Các exception cụ thể sẽ kế thừa class này.
 */
public class BusinessException extends Exception {

    public BusinessException(String message) {
        super(message);
    }
}