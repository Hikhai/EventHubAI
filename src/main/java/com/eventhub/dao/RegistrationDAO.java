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

    public Registration findByUserAndEvent(int userId, int eventId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserAndEvent(userId, eventId, conn);
        }
    }

    public Registration findByUserAndEvent(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "SELECT registration_id, user_id, event_id, status, registered_at, cancelled_at " +
                "FROM registrations WHERE user_id = ? AND event_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
                return null;
            }
        }
    }

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
            return mapList(stmt, this::mapResultSetWithEvent);
        }
    }

    public List<Registration> findAllByEvent(int eventId) throws SQLException {
        String sql = "SELECT r.*, u.full_name AS user_full_name, u.email AS user_email " +
                "FROM registrations r " +
                "JOIN users u ON r.user_id = u.user_id " +
                "WHERE r.event_id = ? " +
                "ORDER BY r.registered_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            return mapList(stmt, this::mapResultSetWithUser);
        }
    }

    public void insert(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "INSERT INTO registrations (user_id, event_id, status) VALUES (?, ?, 'REGISTERED')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
        }
    }

    public void reactivate(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE registrations SET status='REGISTERED', " +
                "registered_at=NOW(), cancelled_at=NULL " +
                "WHERE user_id=? AND event_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
        }
    }

    public void cancel(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE registrations SET status='CANCELLED', cancelled_at=NOW() " +
                "WHERE user_id=? AND event_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
        }
    }

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
            return mapList(stmt, this::mapResultSetFull);
        }
    }

    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE status = 'REGISTERED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

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

    private Registration mapResultSetWithUser(ResultSet rs) throws SQLException {
        Registration reg = mapResultSet(rs);
        reg.setUserFullName(rs.getString("user_full_name"));
        reg.setUserEmail(rs.getString("user_email"));
        return reg;
    }

    private Registration mapResultSetWithEvent(ResultSet rs) throws SQLException {
        Registration reg = mapResultSet(rs);
        mapEventFields(reg, rs);
        return reg;
    }

    private Registration mapResultSetFull(ResultSet rs) throws SQLException {
        Registration reg = mapResultSetWithUser(rs);
        mapEventFields(reg, rs);
        return reg;
    }

    private void mapEventFields(Registration reg, ResultSet rs) throws SQLException {
        reg.setEventTitle(rs.getString("event_title"));
        reg.setEventLocation(rs.getString("event_location"));
        reg.setEventStatus(rs.getString("event_status"));
        reg.setEventImagePath(rs.getString("event_image_path"));
        reg.setEventAvgRating(rs.getDouble("event_avg_rating"));

        Timestamp start = rs.getTimestamp("event_start_time");
        if (start != null) reg.setEventStartTime(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("event_end_time");
        if (end != null) reg.setEventEndTime(end.toLocalDateTime());
    }

    @FunctionalInterface
    private interface RowMapper {
        Registration map(ResultSet rs) throws SQLException;
    }

    private List<Registration> mapList(PreparedStatement stmt, RowMapper mapper) throws SQLException {
        List<Registration> list = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapper.map(rs));
            }
        }
        return list;
    }
}
