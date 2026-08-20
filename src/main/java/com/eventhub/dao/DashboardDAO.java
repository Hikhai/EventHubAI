package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.dto.DashboardDTO;
import com.eventhub.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO chuyên biệt cho các query phức tạp của Dashboard Admin.
 */
public class DashboardDAO {

    private static final String EVENT_SELECT =
            "SELECT e.*, c.category_name, u.full_name AS created_by_name " +
                    "FROM events e " +
                    "JOIN categories c ON e.category_id = c.category_id " +
                    "JOIN users u ON e.created_by = u.user_id ";

    public int[] getOverviewStats() throws SQLException {
        String sql = "SELECT " +
                "COUNT(*) AS total, " +
                "SUM(CASE WHEN status='PUBLISHED' AND start_time > NOW() THEN 1 ELSE 0 END) AS active, " +
                "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled " +
                "FROM events";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return new int[]{
                    rs.getInt("total"),
                    rs.getInt("active"),
                    rs.getInt("completed"),
                    rs.getInt("cancelled")
            };
        }
    }

    public List<DashboardDTO.MonthStat> getRegistrationsByMonth() throws SQLException {
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
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<DashboardDTO.MonthStat> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new DashboardDTO.MonthStat(
                        rs.getString("month_label"),
                        rs.getInt("count")
                ));
            }
            return list;
        }
    }

    public List<DashboardDTO.CategoryStat> getEventsByCategory() throws SQLException {
        String sql = "SELECT c.category_name, COUNT(e.event_id) AS total_events, " +
                "COALESCE(SUM(e.current_registered), 0) AS total_registered " +
                "FROM categories c " +
                "LEFT JOIN events e ON c.category_id = e.category_id " +
                "WHERE c.is_active = 1 " +
                "GROUP BY c.category_id, c.category_name " +
                "ORDER BY total_events DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<DashboardDTO.CategoryStat> list = new ArrayList<>();
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

    public List<Event> getTopEvents(int limit) throws SQLException {
        String sql = EVENT_SELECT +
                "WHERE e.status != 'CANCELLED' " +
                "ORDER BY e.current_registered DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return mapEvents(stmt);
        }
    }

    public List<Event> getUpcomingEvents(int days) throws SQLException {
        String sql = EVENT_SELECT +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL ? DAY) " +
                "ORDER BY e.start_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, days);
            return mapEvents(stmt);
        }
    }

    public List<Event> getAlmostFullEvents(double threshold) throws SQLException {
        String sql = EVENT_SELECT +
                "WHERE e.status = 'PUBLISHED' " +
                "AND e.start_time > NOW() " +
                "AND e.max_participants > 0 " +
                "AND (e.current_registered / e.max_participants) >= ? " +
                "ORDER BY (e.current_registered / e.max_participants) DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, threshold);
            return mapEvents(stmt);
        }
    }

    public List<Event> getTopRatedEvents(int minReviews, int limit) throws SQLException {
        String sql = EVENT_SELECT +
                "WHERE e.total_reviews >= ? " +
                "ORDER BY e.avg_rating DESC, e.total_reviews DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, minReviews);
            stmt.setInt(2, limit);
            return mapEvents(stmt);
        }
    }

    private List<Event> mapEvents(PreparedStatement stmt) throws SQLException {
        List<Event> list = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(EventDAO.mapResultSet(rs));
            }
        }
        return list;
    }
}
