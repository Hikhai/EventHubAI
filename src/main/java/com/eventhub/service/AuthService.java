package com.eventhub.service;

import com.eventhub.dao.UserDAO;
import com.eventhub.exception.AuthException;
import com.eventhub.model.User;
import com.eventhub.util.PasswordUtil;
import com.eventhub.util.ValidationUtil;

import java.sql.SQLException;

/**
 * Service xử lý xác thực: đăng ký, đăng nhập.
 * Tất cả business logic liên quan đến auth nằm ở đây.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // =====================================================
    // ĐĂNG KÝ TÀI KHOẢN
    // =====================================================

    /**
     * Đăng ký tài khoản mới.
     * Validate → Check trùng email → Hash password → Lưu DB.
     *
     * @throws AuthException nếu vi phạm business rule
     * @throws SQLException  nếu lỗi database
     */
    public void register(String fullName, String email,
                         String password, String confirmPassword)
            throws AuthException, SQLException {

        // --- BƯỚC 1: Validate từng trường ---
        validateRegisterInput(fullName, email, password, confirmPassword);

        // --- BƯỚC 2: Chuẩn hóa email ---
        String normalizedEmail = email.trim().toLowerCase();

        // --- BƯỚC 3: Kiểm tra email chưa tồn tại ---
        if (userDAO.existsByEmail(normalizedEmail)) {
            throw new AuthException("Email này đã được sử dụng. Vui lòng dùng email khác.");
        }

        // --- BƯỚC 4: Hash mật khẩu (KHÔNG lưu plain text) ---
        String hashedPassword = PasswordUtil.hash(password);

        // --- BƯỚC 5: Tạo User object và lưu vào DB ---
        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(hashedPassword);
        newUser.setRole("USER");  // Luôn là USER khi đăng ký

        userDAO.insert(newUser);
        // Đăng ký xong KHÔNG tự đăng nhập (theo spec)
    }

    /**
     * Validate input đăng ký theo từng rule.
     * Throw exception ngay khi gặp lỗi đầu tiên.
     */
    private void validateRegisterInput(String fullName, String email,
                                       String password, String confirmPassword)
            throws AuthException {

        // Họ tên
        if (ValidationUtil.isBlank(fullName)) {
            throw new AuthException("Vui lòng nhập họ và tên.");
        }
        if (fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new AuthException("Họ và tên phải từ 2 đến 100 ký tự.");
        }

        // Email
        if (ValidationUtil.isBlank(email)) {
            throw new AuthException("Vui lòng nhập email.");
        }
        if (!ValidationUtil.isValidEmail(email.trim())) {
            throw new AuthException("Email không đúng định dạng.");
        }
        if (email.trim().length() > 150) {
            throw new AuthException("Email không được vượt quá 150 ký tự.");
        }

        // Mật khẩu
        if (ValidationUtil.isBlank(password)) {
            throw new AuthException("Vui lòng nhập mật khẩu.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new AuthException(
                    "Mật khẩu phải từ 8-50 ký tự, " +
                            "gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số."
            );
        }

        // Xác nhận mật khẩu
        if (ValidationUtil.isBlank(confirmPassword)) {
            throw new AuthException("Vui lòng xác nhận mật khẩu.");
        }
        if (!password.equals(confirmPassword)) {
            throw new AuthException("Mật khẩu xác nhận không khớp.");
        }
    }

    // =====================================================
    // ĐĂNG NHẬP
    // =====================================================

    /**
     * Đăng nhập và trả về User nếu thành công.
     *
     * BẢO MẬT: Luôn trả về cùng 1 message lỗi dù sai email hay sai password.
     * Tránh attacker dùng message khác nhau để đoán email tồn tại.
     *
     * @return User object nếu đăng nhập thành công
     * @throws AuthException nếu sai thông tin hoặc tài khoản bị khóa
     */
    public User login(String email, String password)
            throws AuthException, SQLException {

        // Thông báo lỗi chung (không tiết lộ sai email hay sai password)
        final String GENERIC_ERROR = "Email hoặc mật khẩu không chính xác.";

        // --- BƯỚC 1: Validate input cơ bản ---
        if (ValidationUtil.isBlank(email) || ValidationUtil.isBlank(password)) {
            throw new AuthException(GENERIC_ERROR);
        }

        // --- BƯỚC 2: Tìm user theo email ---
        User user = userDAO.findByEmail(email.trim().toLowerCase());

        // Email không tồn tại → trả cùng message (không nói email sai)
        if (user == null) {
            throw new AuthException(GENERIC_ERROR);
        }

        // --- BƯỚC 3: Kiểm tra mật khẩu ---
        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new AuthException(GENERIC_ERROR);
        }

        // --- BƯỚC 4: Kiểm tra tài khoản có bị khóa không ---
        // (Kiểm tra SAU verify password để tránh timing attack)
        if (!user.isActive()) {
            throw new AuthException(
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên."
            );
        }

        // Đăng nhập thành công → trả về User
        return user;
    }
}