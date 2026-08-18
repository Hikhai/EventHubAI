package com.eventhub.util;

/**
 * Utility class chứa các hàm validate input thường dùng.
 */
public class ValidationUtil {

    private ValidationUtil() {}

    /** Chuỗi null hoặc rỗng sau khi trim */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Kiểm tra format email hợp lệ.
     * Regex đơn giản nhưng đủ dùng cho project này.
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) return false;
        String regex = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
        return email.matches(regex);
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

        boolean hasUpper  = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower  = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit  = password.chars().anyMatch(Character::isDigit);

        return hasUpper && hasLower && hasDigit;
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