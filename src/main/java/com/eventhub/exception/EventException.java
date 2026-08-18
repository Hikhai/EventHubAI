package com.eventhub.exception;

/** Lỗi liên quan đến sự kiện */
public class EventException extends BusinessException {
    public EventException(String message) {
        super(message);
    }
}