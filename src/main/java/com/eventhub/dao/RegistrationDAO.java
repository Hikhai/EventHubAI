package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.Registration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng registrations.
 */
public class RegistrationDAO {

    /**
     * Tìm bản ghi đăng ký của một user với một event.
     * Dùng để kiểm tra user đã đăng ký chưa và trạng thái là gì.
     */
    public Registration findByUserAndEvent(int userId, int eventId) throws SQLException {
        String sql = "SELECT * FROM registrations WHERE user_id = ? AND event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapResultSet(rs);
            return null;
        }
    }

    /**
     * Lấy tất cả đăng ký của một user (JOIN với events để lấy thông tin sự kiện).
     * Dùng cho trang "Sự kiện của tôi".
     */
    public List<Registration> findAllByUser(int userId) throws SQLException {
        String sql = "SELECT r.*, " +
                "e.title AS event_title, e.start_time AS event_start_time, " +
                "e.end_time AS event_end_time, e.location AS event_location, " +
                "e.status AS event_status, e.image_path AS event_image_path, " +
                "e.avg_rating AS event_avg_rating " +
                "FROM registrations r " +
                "JOIN events e ON r.event_id = e.event_id " +
                "WHERE r.user_id = ? " +
                "ORDER BY r.registered_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            List<Registration> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSetWithEvent(rs));
            return list;
        }
    }

    /**
     * Lấy tất cả đăng ký của một sự kiện (JOIN với users để lấy thông tin user).
     * Dùng cho Admin xem danh sách người đăng ký.
     */
    public List<Registration> findAllByEvent(int eventId) throws SQLException {
        String sql = "SELECT r.*, u.full_name AS user_full_name, u.email AS user_email " +
                "FROM registrations r " +
                "JOIN users u ON r.user_id = u.user_id " +
                "WHERE r.event_id = ? " +
                "ORDER BY r.registered_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            List<Registration> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSetWithUser(rs));
            return list;
        }
    }

    /**
     * Thêm mới bản ghi đăng ký (INSERT).
     * Dùng Connection từ ngoài để cùng transaction với increment.
     */
    public void insert(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "INSERT INTO registrations (user_id, event_id, status) VALUES (?, ?, 'REGISTERED')";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        stmt.setInt(2, eventId);
        stmt.executeUpdate();
    }

    /**
     * Kích hoạt lại đăng ký đã hủy (UPDATE từ CANCELLED → REGISTERED).
     * Dùng khi user "đăng ký lại" sau khi đã hủy.
     */
    public void reactivate(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE registrations SET status='REGISTERED', " +
                "registered_at=NOW(), cancelled_at=NULL " +
                "WHERE user_id=? AND event_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        stmt.setInt(2, eventId);
        stmt.executeUpdate();
    }

    /**
     * Hủy đăng ký (UPDATE status → CANCELLED, ghi thời gian hủy).
     */
    public void cancel(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE registrations SET status='CANCELLED', cancelled_at=NOW() " +
                "WHERE user_id=? AND event_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        stmt.setInt(2, eventId);
        stmt.executeUpdate();
    }

    /**
     * Lấy N đăng ký gần đây nhất (dùng cho Dashboard).
     */
    public List<Registration> findRecent(int limit) throws SQLException {
        String sql = "SELECT r.*, u.full_name AS user_full_name, u.email AS user_email, " +
                "e.title AS event_title, e.start_time AS event_start_time, " +
                "e.end_time AS event_end_time, e.location AS event_location, " +
                "e.status AS event_status, e.image_path AS event_image_path, " +
                "e.avg_rating AS event_avg_rating " +
                "FROM registrations r " +
                "JOIN users u ON r.user_id = u.user_id " +
                "JOIN events e ON r.event_id = e.event_id " +
                "WHERE r.status = 'REGISTERED' " +
                "ORDER BY r.registered_at DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            List<Registration> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSetFull(rs));
            return list;
        }
    }

    /**
     * Đếm tổng số đăng ký ACTIVE.
     */
    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE status = 'REGISTERED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // Map chỉ các trường của bảng registrations
    private Registration mapResultSet(ResultSet rs) throws SQLException {
        Registration reg = new Registration();
        reg.setRegistrationId(rs.getInt("registration_id"));
        reg.setUserId(rs.getInt("user_id"));
        reg.setEventId(rs.getInt("event_id"));
        reg.setStatus(rs.getString("status"));

        Timestamp regAt = rs.getTimestamp("registered_at");
        if (regAt != null) reg.setRegisteredAt(regAt.toLocalDateTime());

        Timestamp cancelAt = rs.getTimestamp("cancelled_at");
        if (cancelAt != null) reg.setCancelledAt(cancelAt.toLocalDateTime());

        return reg;
    }

    // Map kèm thông tin user (JOIN với users)
    private Registration mapResultSetWithUser(ResultSet rs) throws SQLException {
        Registration reg = mapResultSet(rs);
        reg.setUserFullName(rs.getString("user_full_name"));
        reg.setUserEmail(rs.getString("user_email"));
        return reg;
    }

    // Map kèm thông tin event (JOIN với events)
    private Registration mapResultSetWithEvent(ResultSet rs) throws SQLException {
        Registration reg = mapResultSet(rs);
        reg.setEventTitle(rs.getString("event_title"));
        reg.setEventLocation(rs.getString("event_location"));
        reg.setEventStatus(rs.getString("event_status"));
        reg.setEventImagePath(rs.getString("event_image_path"));
        reg.setEventAvgRating(rs.getDouble("event_avg_rating"));

        Timestamp start = rs.getTimestamp("event_start_time");
        if (start != null) reg.setEventStartTime(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("event_end_time");
        if (end != null) reg.setEventEndTime(end.toLocalDateTime());

        return reg;
    }

    // Map đầy đủ cả user lẫn event
    private Registration mapResultSetFull(ResultSet rs) throws SQLException {
        Registration reg = mapResultSetWithUser(rs);
        reg.setEventTitle(rs.getString("event_title"));
        reg.setEventLocation(rs.getString("event_location"));
        reg.setEventStatus(rs.getString("event_status"));
        reg.setEventImagePath(rs.getString("event_image_path"));
        reg.setEventAvgRating(rs.getDouble("event_avg_rating"));

        Timestamp start = rs.getTimestamp("event_start_time");
        if (start != null) reg.setEventStartTime(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("event_end_time");
        if (end != null) reg.setEventEndTime(end.toLocalDateTime());

        return reg;
    }
}