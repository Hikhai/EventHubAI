package com.eventhub.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class xử lý mật khẩu với BCrypt.
 * BCrypt tự động tạo salt ngẫu nhiên → 2 lần hash cùng password cho kết quả khác nhau.
 */
public class PasswordUtil {

    // Workload = 10: cân bằng giữa bảo mật và tốc độ
    private static final int BCRYPT_ROUNDS = 10;

    private PasswordUtil() {}

    /**
     * Hash mật khẩu plain text thành BCrypt hash để lưu vào DB.
     * @param plainPassword mật khẩu người dùng nhập
     * @return chuỗi hash (bắt đầu bằng $2a$10$...)
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Kiểm tra mật khẩu nhập vào có khớp với hash trong DB không.
     * @param plainPassword mật khẩu người dùng nhập
     * @param hashedPassword hash lấy từ DB
     * @return true nếu khớp
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // BCrypt throw exception nếu hash format sai
            return false;
        }
    }
}