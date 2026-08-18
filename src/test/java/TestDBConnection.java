import com.eventhub.config.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * File test tạm thời — chạy xong có thể xóa.
 * Kiểm tra kết nối database có hoạt động không.
 */
public class TestDBConnection {

    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU TEST DATABASE CONNECTION ===\n");

        // TEST 1: Kết nối cơ bản
        testBasicConnection();

        // TEST 2: Đọc dữ liệu từ bảng
        testReadData();

        System.out.println("\n=== KẾT THÚC TEST ===");
    }

    static void testBasicConnection() {
        System.out.println("--- Test 1: Kết nối cơ bản ---");
        try {
            // Đặt tạm env variable trực tiếp để test
            // (trong thực tế đọc từ System.getenv)
            System.setProperty("DB_URL",
                    "jdbc:mysql://localhost:3306/eventhub_db" +
                            "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh" +
                            "&characterEncoding=UTF-8&allowPublicKeyRetrieval=true");
            System.setProperty("DB_USERNAME", "root");
            System.setProperty("DB_PASSWORD", "123456"); // ← Sửa password

            Connection conn = DBConnection.getConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Kết nối database THÀNH CÔNG!");
                System.out.println("   Database: " + conn.getCatalog());
                conn.close();
            }

        } catch (Exception e) {
            System.out.println("❌ Kết nối THẤT BẠI: " + e.getMessage());
            System.out.println("   → Kiểm tra: MySQL đang chạy chưa?");
            System.out.println("   → Kiểm tra: Username/Password đúng chưa?");
            System.out.println("   → Kiểm tra: Database eventhub_db đã tạo chưa?");
        }
    }

    static void testReadData() {
        System.out.println("\n--- Test 2: Đọc dữ liệu ---");
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Đọc categories
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM categories");
            rs.next();
            System.out.println("✅ Bảng categories: " + rs.getInt("cnt") + " danh mục");

            // Đọc users
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users");
            rs.next();
            System.out.println("✅ Bảng users: " + rs.getInt("cnt") + " tài khoản");

            // Đọc events
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM events");
            rs.next();
            System.out.println("✅ Bảng events: " + rs.getInt("cnt") + " sự kiện");

        } catch (Exception e) {
            System.out.println("❌ Đọc dữ liệu THẤT BẠI: " + e.getMessage());
        }
    }
}