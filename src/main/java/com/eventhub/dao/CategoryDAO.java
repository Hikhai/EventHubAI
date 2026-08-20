package com.eventhub.dao;

import com.eventhub.config.DBConnection;
import com.eventhub.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng categories.
 */
public class CategoryDAO {

    public List<Category> findAll() throws SQLException {
        return queryList("SELECT * FROM categories WHERE is_active = 1 ORDER BY category_name");
    }

    public List<Category> findAllForAdmin() throws SQLException {
        return queryList("SELECT * FROM categories ORDER BY is_active DESC, category_name");
    }

    public Category findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE category_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
                return null;
            }
        }
    }

    public void insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getCategoryName().trim());
            stmt.setString(2, category.getDescription());
            stmt.executeUpdate();
        }
    }

    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET category_name = ?, description = ? WHERE category_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getCategoryName().trim());
            stmt.setString(2, category.getDescription());
            stmt.setInt(3, category.getCategoryId());
            stmt.executeUpdate();
        }
    }

    public void deactivate(int id) throws SQLException {
        updateActiveFlag(id, false);
    }

    public void activate(int id) throws SQLException {
        updateActiveFlag(id, true);
    }

    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM categories WHERE LOWER(category_name) = LOWER(?) LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByNameExcluding(String name, int excludeId) throws SQLException {
        String sql = "SELECT 1 FROM categories " +
                "WHERE LOWER(category_name) = LOWER(?) AND category_id != ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countPublishedEvents(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM events WHERE category_id = ? AND status = 'PUBLISHED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateActiveFlag(int id, boolean active) throws SQLException {
        String sql = "UPDATE categories SET is_active = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    private List<Category> queryList(String sql) throws SQLException {
        List<Category> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Category mapResultSet(ResultSet rs) throws SQLException {
        Category cat = new Category();
        cat.setCategoryId(rs.getInt("category_id"));
        cat.setCategoryName(rs.getString("category_name"));
        cat.setDescription(rs.getString("description"));
        cat.setActive(rs.getBoolean("is_active"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) cat.setCreatedAt(ts.toLocalDateTime());

        return cat;
    }
}
