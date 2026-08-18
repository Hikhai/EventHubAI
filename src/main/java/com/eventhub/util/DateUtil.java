package com.eventhub.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class xử lý ngày giờ.
 * HTML input[type=datetime-local] gửi format "yyyy-MM-ddTHH:mm"
 * MySQL lưu format LocalDateTime
 */
public class DateUtil {

    // Format HTML datetime-local input
    private static final DateTimeFormatter HTML_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // Format hiển thị cho người dùng
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtil() {}

    /**
     * Parse chuỗi từ HTML datetime-local input thành LocalDateTime.
     * @return null nếu chuỗi null hoặc sai format
     */
    public static LocalDateTime parseHtmlDateTime(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(str.trim(), HTML_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Convert LocalDateTime thành chuỗi để điền vào HTML input.
     * Dùng khi sửa sự kiện (pre-fill form).
     */
    public static String toHtmlDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(HTML_FORMAT);
    }

    /**
     * Format đẹp để hiển thị cho người dùng.
     */
    public static String toDisplayString(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DISPLAY_FORMAT);
    }
}