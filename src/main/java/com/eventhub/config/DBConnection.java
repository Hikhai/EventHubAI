package com.eventhub.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cung cấp Connection từ HikariCP pool.
 *
 * Cách dùng trong DAO:
 *   try (Connection conn = DBConnection.getConnection()) {
 *       // thực hiện query
 *   }
 */
public class DBConnection {

    private static final String URL = System.getenv("DB_URL");
    private static final String USERNAME = System.getenv("DB_USERNAME");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private static volatile HikariDataSource dataSource;

    private DBConnection() {
    }

    /**
     * Khởi tạo pool (gọi khi app start). An toàn nếu gọi nhiều lần.
     */
    public static void init() {
        getDataSource();
    }

    /**
     * Đóng pool khi app shutdown để giải phóng kết nối MySQL.
     */
    public static void shutdown() {
        HikariDataSource ds = dataSource;
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
        dataSource = null;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Chạy logic trong 1 transaction, tự commit/rollback và trả connection về pool.
     */
    public static <T> T inTransaction(SqlWork<T> work) throws Exception {
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            T result = work.execute(conn);
            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Hikari sẽ reset autocommit khi trả về pool
            }
            conn.close();
        }
    }

    public static void inTransaction(SqlAction action) throws Exception {
        inTransaction(conn -> {
            action.execute(conn);
            return null;
        });
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface SqlAction {
        void execute(Connection conn) throws Exception;
    }

    private static HikariDataSource getDataSource() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (DBConnection.class) {
                local = dataSource;
                if (local == null) {
                    local = createPool();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    private static HikariDataSource createPool() {
        if (URL == null || URL.isBlank()
                || USERNAME == null || USERNAME.isBlank()
                || PASSWORD == null) {
            throw new IllegalStateException(
                    "Database config chưa được cấu hình! " +
                            "Kiểm tra environment variables: " +
                            "DB_URL, DB_USERNAME, DB_PASSWORD"
            );
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Không tìm thấy MySQL driver (com.mysql.cj.jdbc.Driver). " +
                            "Reload Maven rồi rebuild artifact EventHubAI:war exploded.",
                    e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("EventHubPool");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);
        config.setInitializationFailTimeout(10_000);
        config.setLeakDetectionThreshold(30_000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        return new HikariDataSource(config);
    }
}
