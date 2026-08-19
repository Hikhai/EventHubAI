package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.dto.DashboardDTO;
import com.eventhub.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO chuyên biệt cho các query phức tạp của Dashboard Admin.
 * Dùng aggregate functions (COUNT, AVG, GROUP BY) thay vì N queries riêng lẻ.
 */
public class DashboardDAO {

    /**
     * Lấy tất cả số tổng quan trong 1 query.
     * CASE WHEN giúp đếm theo điều kiện mà không cần nhiều query.
     */
    public int[] getOverviewStats() throws SQLException {
        // [0]=total, [1]=active, [2]=completed, [3]=cancelled
        String sql = "SELECT " +
                "COUNT(*) AS total, " +
                "SUM(CASE WHEN status='PUBLISHED' AND start_time > NOW() THEN 1 ELSE 0 END) AS active, " +
                "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled " +
                "FROM events";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return new int[]{
                    rs.getInt("total"),
                    rs.getInt("active"),
                    rs.getInt("completed"),
                    rs.getInt("cancelled")
            };
        }
    }

    /**
     * Lấy số đăng ký theo từng tháng (12 tháng gần nhất) cho Line Chart.
     */
    /**
     * Lấy số đăng ký theo từng tháng (12 tháng gần nhất) cho Line Chart.
     */
    public List<DashboardDTO.MonthStat> getRegistrationsByMonth() throws SQLException {
        // Dùng subquery để tránh ONLY_FULL_GROUP_BY issue
        // Đưa DATE_FORMAT vào cùng một biểu thức, dùng alias
        String sql =
                "SELECT month_label, month_sort, count FROM ( " +
                        "  SELECT " +
                        "    DATE_FORMAT(registered_at, '%m/%Y') AS month_label, " +
                        "    DATE_FORMAT(registered_at, '%Y-%m') AS month_sort, " +
                        "    COUNT(*) AS count " +
                        "  FROM registrations " +
                        "  WHERE registered_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                        "  AND status = 'REGISTERED' " +
                        "  GROUP BY month_label, month_sort " +
                        ") AS t " +
                        "ORDER BY month_sort ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            List<DashboardDTO.MonthStat> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new DashboardDTO.MonthStat(
                        rs.getString("month_label"),
                        rs.getInt("count")
                ));
            }
            return list;
        }
    }

    /**
     * Lấy số sự kiện và đăng ký theo danh mục cho Doughnut Chart.
     */
    public List<DashboardDTO.CategoryStat> getEventsByCategory() throws SQLException {
        String sql = "SELECT c.category_name, COUNT(e.event_id) AS total_events, " +
                "COALESCE(SUM(e.current_registered), 0) AS total_registered " +
                "FROM categories c " +
                "LEFT JOIN events e ON c.category_id = e.category_id " +
                "WHERE c.is_active = 1 " +
                "GROUP BY c.category_id, c.category_name " +
                "ORDER BY total_events DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            List<DashboardDTO.CategoryStat> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new DashboardDTO.CategoryStat(
                        rs.getString("category_name"),
                        rs.getInt("total_events"),
                        rs.getInt("total_registered")
                ));
            }
            return list;
        }
    }

    /**
     * Top N sự kiện có nhiều người đăng ký nhất.
     */
    public List<Event> getTopEvents(int limit) throws SQLException {
        String sql = "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                "FROM events e " +
                "JOIN categories c ON e.category_id = c.category_id " +
                "JOIN users u ON e.created_by = u.user_id " +
                "WHERE e.status != 'CANCELLED' " +
                "ORDER BY e.current_registered DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapEvent(rs));
            return list;
        }
    }

    /**
     * Sự kiện sắp diễn ra trong N ngày tới.
     */
    public List<Event> getUpcomingEvents(int days) throws SQLException {
        String sql = "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                "FROM events e " +
                "JOIN categories c ON e.category_id = c.category_id " +
                "JOIN users u ON e.created_by = u.user_id " +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL ? DAY) " +
                "ORDER BY e.start_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, days);
            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapEvent(rs));
            return list;
        }
    }

    /**
     * Sự kiện sắp hết chỗ (tỷ lệ đăng ký >= threshold%).
     */
    public List<Event> getAlmostFullEvents(double threshold) throws SQLException {
        String sql = "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                "FROM events e " +
                "JOIN categories c ON e.category_id = c.category_id " +
                "JOIN users u ON e.created_by = u.user_id " +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.start_time > NOW() " +
                "AND (e.current_registered / e.max_participants) >= ? " +
                "ORDER BY (e.current_registered / e.max_participants) DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, threshold);
            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapEvent(rs));
            return list;
        }
    }

    /**
     * Top N sự kiện được đánh giá cao nhất.
     */
    public List<Event> getTopRatedEvents(int minReviews, int limit) throws SQLException {
        String sql = "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                "FROM events e " +
                "JOIN categories c ON e.category_id = c.category_id " +
                "JOIN users u ON e.created_by = u.user_id " +
                "WHERE e.total_reviews >= ? " +
                "ORDER BY e.avg_rating DESC, e.total_reviews DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, minReviews);
            stmt.setInt(2, limit);
            List<Event> list = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapEvent(rs));
            return list;
        }
    }

    // Map ResultSet thành Event (dùng chung cho Dashboard queries)
    private Event mapEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getInt("event_id"));
        event.setTitle(rs.getString("title"));
        event.setLocation(rs.getString("location"));
        event.setMaxParticipants(rs.getInt("max_participants"));
        event.setCurrentRegistered(rs.getInt("current_registered"));
        event.setAvgRating(rs.getDouble("avg_rating"));
        event.setTotalReviews(rs.getInt("total_reviews"));
        event.setStatus(rs.getString("status"));
        event.setImagePath(rs.getString("image_path"));
        event.setCategoryName(rs.getString("category_name"));
        event.setCreatedByName(rs.getString("created_by_name"));

        Timestamp start = rs.getTimestamp("start_time");
        if (start != null) event.setStartTime(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) event.setEndTime(end.toLocalDateTime());

        return event;
    }
}