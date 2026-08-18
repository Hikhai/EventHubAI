package com.eventhub.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class để lấy kết nối database.
 *
 * Cách dùng trong DAO:
 *   try (Connection conn = DBConnection.getConnection()) {
 *       // thực hiện query
 *   }
 * Connection tự đóng khi ra khỏi try-with-resources.
 */
public class DBConnection {

    // Đọc config từ biến môi trường (không hardcode vào code)
    private static final String URL =
            System.getenv("DB_URL");

    private static final String USERNAME =
            System.getenv("DB_USERNAME");

    private static final String PASSWORD =
            System.getenv("DB_PASSWORD");

    // Ngăn tạo instance (class chỉ có method static)
    private DBConnection() {
    }

    /**
     * Tạo và trả về một Connection mới tới database.
     * Caller có trách nhiệm đóng Connection này!
     */
    public static Connection getConnection() throws SQLException {

        // =====================================================
        // 1. Kiểm tra database configuration
        // =====================================================
        if (URL == null || URL.isBlank()
                || USERNAME == null || USERNAME.isBlank()
                || PASSWORD == null) {

            throw new SQLException(
                    "Database config chưa được cấu hình! " +
                            "Kiểm tra environment variables: " +
                            "DB_URL, DB_USERNAME, DB_PASSWORD"
            );
        }

        // =====================================================
        // 2. Load MySQL JDBC Driver
        // =====================================================
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Không tìm thấy MySQL JDBC Driver! " +
                            "Kiểm tra mysql-connector-j trong WEB-INF/lib.",
                    e
            );
        }

        // =====================================================
        // 3. Tạo database connection
        // =====================================================
        try {
            return DriverManager.getConnection(
                    URL,
                    USERNAME,
                    PASSWORD
            );
        } catch (SQLException e) {
            throw new SQLException(
                    "Không thể kết nối tới database: " + e.getMessage(),
                    e
            );
        }
    }
}