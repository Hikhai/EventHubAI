package com.eventhub.exception;

/** Lỗi liên quan đến đánh giá sự kiện */
public class ReviewException extends BusinessException {
    public ReviewException(String message) {
        super(message);
    }
}