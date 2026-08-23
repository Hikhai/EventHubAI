package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO cho lịch sử trò chuyện trong bảng chat_logs.
 *
 * sessionId là mã cuộc trò chuyện do trình duyệt lưu trong localStorage.
 * userId luôn được lấy từ session đăng nhập ở server, không lấy từ request,
 * để lịch sử của các tài khoản không thể bị đọc lẫn nhau.
 */
public class ChatLogDAO {

    /**
     * Lấy các tin nhắn mới nhất theo đúng thứ tự thời gian tăng dần để dùng
     * làm context cho Gemini hoặc hiển thị lại trên giao diện.
     */
    public List<ChatMessage> findRecentByUserAndSession(int userId,
                                                         String sessionId,
                                                         int limit) throws SQLException {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT log_id, user_id, session_id, role, message, created_at " +
                "FROM chat_logs " +
                "WHERE user_id = ? AND session_id = ? " +
                "ORDER BY created_at DESC, log_id DESC LIMIT ?";

        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, sessionId);
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSet(rs));
                }
            }
        }

        // Query lấy DESC để giới hạn đúng N dòng mới nhất, sau đó trả về
        // ASC vì Gemini và UI đều cần đọc hội thoại từ cũ đến mới.
        Collections.reverse(messages);
        return messages;
    }

    /** Lưu một tin nhắn user hoặc assistant. */
    public void insert(int userId, String sessionId,
                       String role, String message) throws SQLException {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException("Role chat không hợp lệ: " + role);
        }

        String sql = "INSERT INTO chat_logs (user_id, session_id, role, message) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, sessionId);
            stmt.setString(3, role);
            stmt.setString(4, message);
            stmt.executeUpdate();
        }
    }

    /** Xóa toàn bộ lịch sử của một cuộc trò chuyện của đúng user đó. */
    public void deleteByUserAndSession(int userId, String sessionId) throws SQLException {
        String sql = "DELETE FROM chat_logs WHERE user_id = ? AND session_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        }
    }

    private ChatMessage mapResultSet(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ChatMessage(
                rs.getLong("log_id"),
                rs.getInt("user_id"),
                rs.getString("session_id"),
                rs.getString("role"),
                rs.getString("message"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
