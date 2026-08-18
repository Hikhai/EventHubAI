package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.User;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * DAO xử lý tất cả thao tác database liên quan đến bảng users.
 * Chỉ có SQL ở đây, không có business logic.
 */
public class UserDAO {

    /**
     * Tìm user theo email (dùng cho đăng nhập).
     * @return User nếu tìm thấy, null nếu không có
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.toLowerCase().trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);  // Chuyển ResultSet → User object
            }
            return null;  // Không tìm thấy
        }
    }

    /**
     * Tìm user theo ID.
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSet(rs);
            }
            return null;
        }
    }

    /**
     * Kiểm tra email đã tồn tại chưa (dùng khi đăng ký).
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.toLowerCase().trim());
            ResultSet rs = stmt.executeQuery();

            rs.next();
            return rs.getInt(1) > 0;  // COUNT > 0 → đã tồn tại
        }
    }

    /**
     * Thêm user mới vào database (dùng khi đăng ký tài khoản).
     * @return userId vừa được tạo
     */
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password, role) VALUES (?, ?, ?, ?)";

        // RETURN_GENERATED_KEYS để lấy ID vừa insert
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getFullName().trim());
            stmt.setString(2, user.getEmail().toLowerCase().trim());
            stmt.setString(3, user.getPassword());  // Đã hash BCrypt
            stmt.setString(4, "USER");              // Luôn là USER khi đăng ký

            stmt.executeUpdate();

            // Lấy ID vừa tạo
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("Không thể lấy ID sau khi insert user");
        }
    }

    /**
     * Đếm tổng số user (dùng cho Dashboard).
     */
    public int countActiveUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'USER' AND is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Chuyển 1 hàng ResultSet thành User object.
     * Method private, chỉ dùng trong class này.
     */
    private User mapResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));

        // Chuyển java.sql.Timestamp sang LocalDateTime
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        return user;
    }
}