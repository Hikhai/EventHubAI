package com.eventhub.util;

import java.util.regex.Pattern;

/**
 * Utility class chứa các hàm validate input thường dùng.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    private ValidationUtil() {}

    /** Chuỗi null hoặc rỗng sau khi trim */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Kiểm tra format email hợp lệ.
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Kiểm tra mật khẩu đủ mạnh:
     * - 8-50 ký tự
     * - Có ít nhất 1 chữ hoa
     * - Có ít nhất 1 chữ thường
     * - Có ít nhất 1 chữ số
     */
    public static boolean isValidPassword(String password) {
        if (isBlank(password)) return false;
        int len = password.length();
        if (len < 8 || len > 50) return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (int i = 0; i < len; i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            if (hasUpper && hasLower && hasDigit) return true;
        }
        return false;
    }

    /**
     * Parse String thành Integer an toàn.
     * @return null nếu không parse được
     */
    public static Integer parseIntOrNull(String str) {
        if (isBlank(str)) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse String thành int với giá trị mặc định.
     * @return defaultValue nếu không parse được
     */
    public static int parseIntOrDefault(String str, int defaultValue) {
        Integer result = parseIntOrNull(str);
        return result != null ? result : defaultValue;
    }
}
