package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng events.
 * Đây là DAO phức tạp nhất vì events có nhiều trường và JOIN.
 */
public class EventDAO {

    // SQL base để SELECT event (JOIN category + user để lấy tên)
    private static final String BASE_SELECT =
            "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                    "FROM events e " +
                    "JOIN categories c ON e.category_id = c.category_id " +
                    "JOIN users u ON e.created_by = u.user_id ";

    /**
     * Tìm sự kiện theo ID (dùng cho mọi role).
     */
    public Event findById(int eventId) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapResultSet(rs);
            return null;
        }
    }

    /**
     * Tìm event và LOCK ROW để tránh race condition.
     * Dùng trong transaction khi đăng ký sự kiện.
     * PHẢI truyền Connection từ ngoài vào (để cùng transaction).
     */
    public Event findByIdForUpdate(int eventId, Connection conn) throws SQLException {
        String sql = BASE_SELECT + "WHERE e.event_id = ? FOR UPDATE";

        // KHÔNG dùng try-with-resources cho conn vì conn do bên ngoài quản lý
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, eventId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) return mapResultSet(rs);
        return null;
    }

    /**
     * Lấy danh sách sự kiện cho User (chỉ PUBLISHED, chưa hết hạn).
     * Hỗ trợ tìm kiếm và lọc theo danh mục.
     */
    public List<Event> findAllForUser(EventFilterDTO filter) throws SQLException {
        // Xây dựng SQL động dựa trên filter
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() ");

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ?) ");
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND e.category_id = ? ");
        }

        sql.append("ORDER BY e.start_time ASC ");
        sql.append("LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().trim() + "%";
                stmt.setString(paramIndex++, keyword);
                stmt.setString(paramIndex++, keyword);
            }
            if (filter.getCategoryId() != null) {
                stmt.setInt(paramIndex++, filter.getCategoryId());
            }

            stmt.setInt(paramIndex++, filter.getPageSize());
            stmt.setInt(paramIndex, filter.getOffset());

            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        }
    }

    /**
     * Đếm tổng số sự kiện cho User (để tính phân trang).
     */
    public int countForUser(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM events e WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() "
        );

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ?) ");
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND e.category_id = ? ");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().trim() + "%";
                stmt.setString(paramIndex++, keyword);
                stmt.setString(paramIndex++, keyword);
            }
            if (filter.getCategoryId() != null) {
                stmt.setInt(paramIndex, filter.getCategoryId());
            }

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Lấy danh sách sự kiện cho Admin (tất cả trạng thái).
     */
    public List<Event> findAllForAdmin(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE 1=1 ");

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ?) ");
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND e.category_id = ? ");
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND e.status = ? ");
        }

        sql.append("ORDER BY e.created_at DESC ");
        sql.append("LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().trim() + "%";
                stmt.setString(paramIndex++, keyword);
                stmt.setString(paramIndex++, keyword);
            }
            if (filter.getCategoryId() != null) {
                stmt.setInt(paramIndex++, filter.getCategoryId());
            }
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                stmt.setString(paramIndex++, filter.getStatus());
            }

            stmt.setInt(paramIndex++, filter.getPageSize());
            stmt.setInt(paramIndex, filter.getOffset());

            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        }
    }

    /**
     * Đếm tổng sự kiện cho Admin.
     */
    public int countForAdmin(EventFilterDTO filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM events e WHERE 1=1 ");

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ?) ");
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND e.category_id = ? ");
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND e.status = ? ");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().trim() + "%";
                stmt.setString(paramIndex++, keyword);
                stmt.setString(paramIndex++, keyword);
            }
            if (filter.getCategoryId() != null) {
                stmt.setInt(paramIndex++, filter.getCategoryId());
            }
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                stmt.setString(paramIndex, filter.getStatus());
            }

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Thêm sự kiện mới vào DB.
     * @return eventId vừa tạo
     */
    public int insert(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, summary_ai, location, " +
                "start_time, end_time, registration_deadline, max_participants, " +
                "status, category_id, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, event.getTitle().trim());
            stmt.setString(2, event.getDescription().trim());
            stmt.setString(3, event.getSummaryAi());  // có thể null
            stmt.setString(4, event.getLocation().trim());
            stmt.setTimestamp(5, Timestamp.valueOf(event.getStartTime()));
            stmt.setTimestamp(6, Timestamp.valueOf(event.getEndTime()));
            stmt.setTimestamp(7, Timestamp.valueOf(event.getRegistrationDeadline()));
            stmt.setInt(8, event.getMaxParticipants());
            stmt.setString(9, event.getStatus());
            stmt.setInt(10, event.getCategoryId());
            stmt.setInt(11, event.getCreatedBy());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
            throw new SQLException("Không thể lấy ID sự kiện sau khi insert");
        }
    }

    /**
     * Cập nhật thông tin sự kiện.
     */
    public void update(Event event) throws SQLException {
        String sql = "UPDATE events SET title=?, description=?, summary_ai=?, location=?, " +
                "start_time=?, end_time=?, registration_deadline=?, max_participants=?, " +
                "status=?, category_id=? WHERE event_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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
            stmt.setInt(11, event.getEventId());

            stmt.executeUpdate();
        }
    }

    /**
     * Cập nhật đường dẫn ảnh và nguồn ảnh cho sự kiện.
     * Gọi sau khi upload hoặc AI gen ảnh xong.
     */
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

    /**
     * Tăng số người đăng ký lên 1 (dùng trong transaction đăng ký sự kiện).
     * Nhận Connection từ ngoài để cùng transaction với việc insert registration.
     */
    public void incrementRegistered(int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE events SET current_registered = current_registered + 1 WHERE event_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, eventId);
        stmt.executeUpdate();
    }

    /**
     * Giảm số người đăng ký xuống 1 (dùng trong transaction hủy đăng ký).
     */
    public void decrementRegistered(int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE events SET current_registered = current_registered - 1 WHERE event_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, eventId);
        stmt.executeUpdate();
    }

    /**
     * Cập nhật avg_rating sau khi có review mới.
     * Dùng SELECT AVG() trực tiếp từ bảng reviews để đảm bảo chính xác.
     */
    public void updateRating(int eventId, Connection conn) throws SQLException {
        String sql = "UPDATE events SET " +
                "avg_rating = (SELECT ROUND(AVG(rating), 1) FROM reviews WHERE event_id = ?), " +
                "total_reviews = (SELECT COUNT(*) FROM reviews WHERE event_id = ?) " +
                "WHERE event_id = ?";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, eventId);
        stmt.setInt(2, eventId);
        stmt.setInt(3, eventId);
        stmt.executeUpdate();
    }

    /**
     * Xóa sự kiện khỏi DB (hard delete, chỉ dùng khi không có đăng ký).
     */
    public void delete(int eventId) throws SQLException {
        // Xóa reviews trước (FK constraint)
        String deleteReviews = "DELETE FROM reviews WHERE event_id = ?";
        // Xóa registrations (FK constraint)
        String deleteRegs = "DELETE FROM registrations WHERE event_id = ?";
        // Xóa event
        String deleteEvent = "DELETE FROM events WHERE event_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);  // Bắt đầu transaction
            try {
                PreparedStatement s1 = conn.prepareStatement(deleteReviews);
                s1.setInt(1, eventId);
                s1.executeUpdate();

                PreparedStatement s2 = conn.prepareStatement(deleteRegs);
                s2.setInt(1, eventId);
                s2.executeUpdate();

                PreparedStatement s3 = conn.prepareStatement(deleteEvent);
                s3.setInt(1, eventId);
                s3.executeUpdate();

                conn.commit();  // Thành công → commit
            } catch (SQLException e) {
                conn.rollback();  // Lỗi → rollback
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Đếm số đăng ký ACTIVE (REGISTERED) của một sự kiện.
     * Dùng để quyết định xóa cứng hay xóa mềm.
     */
    public int countRegistered(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE event_id=? AND status='REGISTERED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Lấy sự kiện gợi ý cho User dựa trên lịch sử đăng ký.
     * Tìm events cùng danh mục với events User đã đăng ký.
     */
    public List<Event> findRecommendedForUser(int userId, int limit) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.end_time > NOW() " +
                "AND e.current_registered < e.max_participants " +
                // Cùng danh mục với events user đã đăng ký
                "AND e.category_id IN (" +
                "  SELECT DISTINCT ev.category_id FROM registrations r " +
                "  JOIN events ev ON r.event_id = ev.event_id " +
                "  WHERE r.user_id = ? AND r.status = 'REGISTERED'" +
                ") " +
                // Loại trừ events user đã đăng ký rồi
                "AND e.event_id NOT IN (" +
                "  SELECT event_id FROM registrations WHERE user_id = ? AND status = 'REGISTERED'" +
                ") " +
                "ORDER BY e.start_time ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, limit);

            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
            return list;
        }
    }

    /**
     * Fallback: Lấy sự kiện mới nhất (dùng khi guest hoặc user chưa có lịch sử).
     */
    public List<Event> findRecommendedFallback(int limit) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.status = 'PUBLISHED' AND e.end_time > NOW() " +
                "AND e.current_registered < e.max_participants " +
                "ORDER BY e.start_time ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
            return list;
        }
    }

    /**
     * Lấy 3 sự kiện tương tự (cùng danh mục, loại trừ event hiện tại).
     * Dùng trong trang chi tiết sự kiện.
     */
    public List<Event> findSimilar(int categoryId, int excludeEventId) throws SQLException {
        String sql = BASE_SELECT +
                "WHERE e.category_id = ? AND e.event_id != ? " +
                "AND e.status = 'PUBLISHED' AND e.end_time > NOW() " +
                "ORDER BY e.start_time ASC LIMIT 3";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            stmt.setInt(2, excludeEventId);

            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
            return list;
        }
    }

    /**
     * Chuyển ResultSet thành Event object.
     * JOIN nên có thêm category_name và created_by_name.
     */
    private Event mapResultSet(ResultSet rs) throws SQLException {
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

        // JOIN fields
        event.setCategoryName(rs.getString("category_name"));
        event.setCreatedByName(rs.getString("created_by_name"));

        // Chuyển Timestamp → LocalDateTime
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
}