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

    /**
     * Lấy tất cả danh mục đang active (is_active = 1).
     * Dùng trong dropdown chọn danh mục khi tạo sự kiện.
     */
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT * FROM categories WHERE is_active = 1 ORDER BY category_name";
        List<Category> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /**
     * Lấy tất cả danh mục kể cả inactive (dùng cho Admin quản lý).
     */
    public List<Category> findAllForAdmin() throws SQLException {
        String sql = "SELECT * FROM categories ORDER BY is_active DESC, category_name";
        List<Category> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    /**
     * Tìm danh mục theo ID.
     */
    public Category findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE category_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapResultSet(rs);
            return null;
        }
    }

    /**
     * Thêm danh mục mới.
     */
    public void insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.getCategoryName().trim());
            stmt.setString(2, category.getDescription());
            stmt.executeUpdate();
        }
    }

    /**
     * Cập nhật thông tin danh mục.
     */
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

    /**
     * Vô hiệu hóa danh mục (soft delete: set is_active = 0).
     * Không xóa thật vì có thể có event đang dùng DM này.
     */
    public void deactivate(int id) throws SQLException {
        String sql = "UPDATE categories SET is_active = 0 WHERE category_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Kích hoạt lại danh mục.
     */
    public void activate(int id) throws SQLException {
        String sql = "UPDATE categories SET is_active = 1 WHERE category_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại chưa (để tránh trùng tên).
     */
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(category_name) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    /**
     * Kiểm tra tên trùng nhưng loại trừ chính nó (dùng khi sửa tên).
     */
    public boolean existsByNameExcluding(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories " +
                "WHERE LOWER(category_name) = LOWER(?) AND category_id != ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            stmt.setInt(2, excludeId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    /**
     * Đếm số sự kiện PUBLISHED đang dùng danh mục này.
     * Dùng để kiểm tra trước khi vô hiệu hóa danh mục.
     */
    public int countPublishedEvents(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM events " +
                "WHERE category_id = ? AND status = 'PUBLISHED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
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