package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng events.
 */
public class EventDAO {

    private static final String BASE_SELECT =
            "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                    "FROM events e " +
                    "JOIN categories c ON e.category_id = c.category_id " +
                    "JOIN users u ON e.created_by = u.user_id ";

    public Event findById(int eventId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(eventId, conn);
        }
    }

    public Event findById(int eventId, Connection conn) throws SQLException {
        return findOne(conn, BASE_SELECT + "WHERE e.event_id = ?", eventId);
    }

    /**
     * Tìm event và LOCK ROW để tránh race condition.
     * PHẢI truyền Connection từ ngoài vào (để cùng transaction).
     */
    public Event findByIdForUpdate(int eventId, Connection conn) throws SQLException {
        return findOne(conn, BASE_SELECT + "WHERE e.event_id = ? FOR UPDATE", eventId);
    }

    public List<Event> findAllForUser(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() ");
        appendSearchFilters(sql, filter, false);
        sql.append("ORDER BY e.start_time ASC LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = bindSearchFilters(stmt, filter, 1, false);
            stmt.setInt(idx++, filter.getPageSize());
            stmt.setInt(idx, filter.getOffset());
            return mapList(stmt);
        }
    }

    public int countForUser(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM events e WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() "
        );
        appendSearchFilters(sql, filter, false);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bindSearchFilters(stmt, filter, 1, false);
            return queryCount(stmt);
        }
    }

    public List<Event> findAllForAdmin(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE 1=1 ");
        appendSearchFilters(sql, filter, true);
        sql.append("ORDER BY e.created_at DESC LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = bindSearchFilters(stmt, filter, 1, true);
            stmt.setInt(idx++, filter.getPageSize());
            stmt.setInt(idx, filter.getOffset());
            return mapList(stmt);
        }
    }

    public int countForAdmin(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM events e WHERE 1=1 ");
        appendSearchFilters(sql, filter, true);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            bindSearchFilters(stmt, filter, 1, true);
            return queryCount(stmt);
        }
    }

    public int insert(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, summary_ai, location, " +
                "start_time, end_time, registration_deadline, max_participants, " +
                "status, category_id, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindEventFields(stmt, event);
            stmt.setInt(11, event.getCreatedBy());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new SQLException("Không thể lấy ID sự kiện sau khi insert");
        }
    }

    public void update(Event event) throws SQLException {
        String sql = "UPDATE events SET title=?, description=?, summary_ai=?, location=?, " +
                "start_time=?, end_time=?, registration_deadline=?, max_participants=?, " +
                "status=?, category_id=? WHERE event_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindEventFields(stmt, event);
            stmt.setInt(11, event.getEventId());
            stmt.executeUpdate();
        }
    }

    public void updateImage(int eventId, String imagePath, String imageSource) throws SQLException {
        String sql = "UPDATE events SET image_path=?, image_source=? WHERE event_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, imagePath);
            stmt.setString(2, imageSource);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        }
    }

    public void incrementRegistered(int eventId, Connection conn) throws SQLException {
        executeUpdate(conn,
                "UPDATE events SET current_registered = current_registered + 1 WHERE event_id=?",
                eventId);
    }

    public void decrementRegistered(int eventId, Connection conn) throws SQLException {
        executeUpdate(conn,
                "UPDATE events SET current_registered = GREATEST(current_registered - 1, 0) WHERE event_id=?",
                eventId);
    }

    public void updateRating(int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE events SET " +
                "avg_rating = (SELECT ROUND(AVG(rating), 1) FROM reviews WHERE event_id = ?), " +
                "total_reviews = (SELECT COUNT(*) FROM reviews WHERE event_id = ?) " +
                "WHERE event_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, eventId);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        }
    }

    public void delete(int eventId) throws SQLException {
        try {
            DBConnection.inTransaction(conn -> {
                executeUpdate(conn, "DELETE FROM reviews WHERE event_id = ?", eventId);
                executeUpdate(conn, "DELETE FROM registrations WHERE event_id = ?", eventId);
                executeUpdate(conn, "DELETE FROM events WHERE event_id = ?", eventId);
            });
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    /**
     * Hủy sự kiện và toàn bộ đăng ký ACTIVE trong cùng transaction.
     */
    public void cancelEventAndRegistrations(int eventId) throws SQLException {
        try {
            DBConnection.inTransaction(conn -> {
                executeUpdate(conn,
                        "UPDATE registrations SET status='CANCELLED', cancelled_at=NOW() " +
                                "WHERE event_id=? AND status='REGISTERED'",
                        eventId);
                executeUpdate(conn,
                        "UPDATE events SET status='CANCELLED' WHERE event_id=?",
                        eventId);
            });
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public int countRegistered(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE event_id=? AND status='REGISTERED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            return queryCount(stmt);
        }
    }

    public List<Event> findRecommendedForUser(int userId, int limit) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.end_time > NOW() " +
                "AND e.current_registered < e.max_participants " +
                "AND e.category_id IN (" +
                "  SELECT DISTINCT ev.category_id FROM registrations r " +
                "  JOIN events ev ON r.event_id = ev.event_id " +
                "  WHERE r.user_id = ? AND r.status = 'REGISTERED'" +
                ") " +
                "AND e.event_id NOT IN (" +
                "  SELECT event_id FROM registrations WHERE user_id = ? AND status = 'REGISTERED'" +
                ") " +
                "ORDER BY e.start_time ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, limit);
            return mapList(stmt);
        }
    }

    public List<Event> findRecommendedFallback(int limit) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() " +
                "AND e.current_registered < e.max_participants " +
                "ORDER BY e.start_time ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return mapList(stmt);
        }
    }

    public List<Event> findSimilar(int categoryId, int excludeEventId) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.category_id = ? AND e.event_id != ? " +
                "AND e.status = 'PUBLISHED' AND e.end_time > NOW() " +
                "ORDER BY e.start_time ASC LIMIT 3";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            stmt.setInt(2, excludeEventId);
            return mapList(stmt);
        }
    }

    static Event mapResultSet(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getInt("event_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setSummaryAi(rs.getString("summary_ai"));
        event.setImagePath(rs.getString("image_path"));
        event.setImageSource(rs.getString("image_source"));
        event.setLocation(rs.getString("location"));
        event.setMaxParticipants(rs.getInt("max_participants"));
        event.setCurrentRegistered(rs.getInt("current_registered"));
        event.setAvgRating(rs.getDouble("avg_rating"));
        event.setTotalReviews(rs.getInt("total_reviews"));
        event.setStatus(rs.getString("status"));
        event.setCategoryId(rs.getInt("category_id"));
        event.setCreatedBy(rs.getInt("created_by"));
        event.setCategoryName(rs.getString("category_name"));
        event.setCreatedByName(rs.getString("created_by_name"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) event.setStartTime(startTime.toLocalDateTime());

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) event.setEndTime(endTime.toLocalDateTime());

        Timestamp deadline = rs.getTimestamp("registration_deadline");
        if (deadline != null) event.setRegistrationDeadline(deadline.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) event.setCreatedAt(createdAt.toLocalDateTime());

        return event;
    }

    private Event findOne(Connection conn, String sql, int eventId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
                return null;
            }
        }
    }

    private void appendSearchFilters(StringBuilder sql, EventFilterDTO filter, boolean includeStatus) {
        if (filter.hasKeyword()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ?) ");
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND e.category_id = ? ");
        }
        if (includeStatus && filter.hasStatus()) {
            sql.append("AND e.status = ? ");
        }
    }

    private int bindSearchFilters(PreparedStatement stmt, EventFilterDTO filter,
                                  int paramIndex, boolean includeStatus) throws SQLException {
        if (filter.hasKeyword()) {
            String keyword = "%" + filter.getKeyword().trim() + "%";
            stmt.setString(paramIndex++, keyword);
            stmt.setString(paramIndex++, keyword);
        }
        if (filter.getCategoryId() != null) {
            stmt.setInt(paramIndex++, filter.getCategoryId());
        }
        if (includeStatus && filter.hasStatus()) {
            stmt.setString(paramIndex++, filter.getStatus());
        }
        return paramIndex;
    }

    private void bindEventFields(PreparedStatement stmt, Event event) throws SQLException {
        stmt.setString(1, event.getTitle().trim());
        stmt.setString(2, event.getDescription().trim());
        stmt.setString(3, event.getSummaryAi());
        stmt.setString(4, event.getLocation().trim());
        stmt.setTimestamp(5, Timestamp.valueOf(event.getStartTime()));
        stmt.setTimestamp(6, Timestamp.valueOf(event.getEndTime()));
        stmt.setTimestamp(7, Timestamp.valueOf(event.getRegistrationDeadline()));
        stmt.setInt(8, event.getMaxParticipants());
        stmt.setString(9, event.getStatus());
        stmt.setInt(10, event.getCategoryId());
    }

    private static void executeUpdate(Connection conn, String sql, int eventId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.executeUpdate();
        }
    }

    private static int queryCount(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static List<Event> mapList(PreparedStatement stmt) throws SQLException {
        List<Event> list = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }
}
