package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng reviews.
 */
public class ReviewDAO {

    public Review findByUserAndEvent(int userId, int eventId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserAndEvent(userId, eventId, conn);
        }
    }

    public Review findByUserAndEvent(int userId, int eventId, Connection conn) throws SQLException {
        String sql = "SELECT r.*, u.full_name AS user_full_name " +
                "FROM reviews r JOIN users u ON r.user_id = u.user_id " +
                "WHERE r.user_id = ? AND r.event_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
                return null;
            }
        }
    }

    public List<Review> findAllByEvent(int eventId) throws SQLException {
        String sql = "SELECT r.*, u.full_name AS user_full_name " +
                "FROM reviews r JOIN users u ON r.user_id = u.user_id " +
                "WHERE r.event_id = ? ORDER BY r.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            List<Review> list = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
            return list;
        }
    }

    public void insert(int userId, int eventId, int rating,
                       String comment, Connection conn) throws SQLException {
        String sql = "INSERT INTO reviews (user_id, event_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, eventId);
            stmt.setInt(3, rating);

            if (comment != null && !comment.trim().isEmpty()) {
                stmt.setString(4, comment.trim());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.executeUpdate();
        }
    }

    public double getOverallAvgRating() throws SQLException {
        String sql = "SELECT COALESCE(AVG(rating), 0) FROM reviews";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getDouble(1);
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Review mapResultSet(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setEventId(rs.getInt("event_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setUserFullName(rs.getString("user_full_name"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) review.setCreatedAt(ts.toLocalDateTime());

        return review;
    }
}
