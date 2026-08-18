package com.eventhub.exception;

/** Lỗi liên quan đến upload/xử lý file ảnh */
public class FileException extends BusinessException {
    public FileException(String message) {
        super(message);
    }
}