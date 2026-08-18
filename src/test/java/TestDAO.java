import com.eventhub.dao.*;
import com.eventhub.dto.EventFilterDTO;
import com.eventhub.model.*;
import com.eventhub.util.PasswordUtil;
import com.eventhub.util.ValidationUtil;

import java.util.List;

/**
 * Test toàn bộ DAO layer.
 * Chạy sau khi đã test DBConnection thành công.
 */
public class TestDAO {

    public static void main(String[] args) {

        // =========================================================
        // DATABASE CONFIG — dùng cho test DAO
        // =========================================================
        System.setProperty(
                "DB_URL",
                "jdbc:mysql://localhost:3306/eventhub_db" +
                        "?useSSL=false" +
                        "&serverTimezone=Asia/Ho_Chi_Minh" +
                        "&characterEncoding=UTF-8" +
                        "&allowPublicKeyRetrieval=true"
        );

        System.setProperty("DB_USERNAME", "root");
        System.setProperty("DB_PASSWORD", "123456");

        System.out.println("========================================");
        System.out.println("  TEST DAO LAYER — EVENTHUB AI");
        System.out.println("========================================\n");

        testUserDAO();
        testCategoryDAO();
        testEventDAO();
        testRegistrationDAO();
        testReviewDAO();
        testUtils();

        System.out.println("\n========================================");
        System.out.println("  KẾT THÚC TEST");
        System.out.println("========================================");
    }

    // ========================================
    // TEST USER DAO
    // ========================================
    static void testUserDAO() {
        System.out.println("--- TEST UserDAO ---");
        UserDAO dao = new UserDAO();

        try {
            // Test 1: Tìm admin theo email
            User admin = dao.findByEmail("admin@eventhub.com");
            assertNotNull(admin, "Tìm admin theo email");
            assertEquals("ADMIN", admin.getRole(), "Role phải là ADMIN");
            assertEquals("Quản trị viên", admin.getFullName(), "Tên admin");

            // Test 2: Tìm user thường
            User user = dao.findByEmail("an@example.com");
            assertNotNull(user, "Tìm user An");
            assertEquals("USER", user.getRole(), "Role phải là USER");

            // Test 3: Email không tồn tại
            User notFound = dao.findByEmail("khongton@tai.com");
            assertNull(notFound, "Email không tồn tại phải trả null");

            // Test 4: Tìm theo ID
            User byId = dao.findById(1);
            assertNotNull(byId, "Tìm user theo ID=1");

            // Test 5: Kiểm tra email tồn tại
            boolean exists = dao.existsByEmail("admin@eventhub.com");
            assertTrue(exists, "Email admin phải tồn tại");

            boolean notExists = dao.existsByEmail("khongton@example.com");
            assertFalse(notExists, "Email giả không tồn tại");

            // Test 6: Email case-insensitive
            User upperCase = dao.findByEmail("ADMIN@EVENTHUB.COM");
            assertNotNull(upperCase, "Tìm email không phân biệt hoa/thường");

            // Test 7: Đếm user
            int count = dao.countActiveUsers();
            assertTrue(count >= 3, "Phải có ít nhất 3 user: " + count);

            System.out.println("✅ UserDAO — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ UserDAO FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ UserDAO LỖI NGOÀI Ý MUỐN: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // TEST CATEGORY DAO
    // ========================================
    static void testCategoryDAO() {
        System.out.println("--- TEST CategoryDAO ---");
        CategoryDAO dao = new CategoryDAO();

        try {
            // Test 1: Lấy tất cả danh mục active
            List<Category> categories = dao.findAll();
            assertEquals(6, categories.size(),
                    "Phải có 6 danh mục, thực tế: " + categories.size());

            // Test 2: In ra tên các danh mục để kiểm tra
            System.out.println("   Danh sách danh mục:");
            for (Category c : categories) {
                System.out.println("   - [" + c.getCategoryId() + "] "
                        + c.getCategoryName()
                        + " (active=" + c.isActive() + ")");
            }

            // Test 3: Tìm theo ID
            Category cat = dao.findById(1);
            assertNotNull(cat, "Tìm danh mục ID=1");
            System.out.println("   Danh mục ID=1: " + cat.getCategoryName());

            // Test 4: Kiểm tra tên trùng
            boolean exists = dao.existsByName("Hội thảo");
            assertTrue(exists, "Hội thảo phải tồn tại");

            boolean notExists = dao.existsByName("Danh mục không tồn tại");
            assertFalse(notExists, "Danh mục giả không tồn tại");

            System.out.println("✅ CategoryDAO — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ CategoryDAO FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ CategoryDAO LỖI NGOÀI Ý MUỐN: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // TEST EVENT DAO
    // ========================================
    static void testEventDAO() {
        System.out.println("--- TEST EventDAO ---");
        EventDAO dao = new EventDAO();

        try {
            // Test 1: Tìm event theo ID
            Event event = dao.findById(1);
            assertNotNull(event, "Tìm event ID=1");
            assertNotNull(event.getTitle(), "Event phải có title");
            assertNotNull(event.getCategoryName(),
                    "Event phải có categoryName (từ JOIN)");
            System.out.println("   Event ID=1: " + event.getTitle());
            System.out.println("   Category: " + event.getCategoryName());
            System.out.println("   Status: " + event.getStatus());

            // Test 2: Event không tồn tại
            Event notFound = dao.findById(9999);
            assertNull(notFound, "Event ID=9999 phải trả null");

            // Test 3: Lấy danh sách cho User (chỉ PUBLISHED)
            EventFilterDTO filter = new EventFilterDTO();
            List<Event> userEvents = dao.findAllForUser(filter);
            assertTrue(userEvents.size() > 0,
                    "Phải có ít nhất 1 event PUBLISHED: " + userEvents.size());
            System.out.println("   Events cho User (PUBLISHED): "
                    + userEvents.size() + " sự kiện");

            // Kiểm tra tất cả đều là PUBLISHED
            for (Event e : userEvents) {
                assertEquals("PUBLISHED", e.getStatus(),
                        "User chỉ thấy PUBLISHED, nhưng thấy: " + e.getStatus());
            }

            // Test 4: Đếm events cho User
            int count = dao.countForUser(filter);
            assertEquals(userEvents.size(), count,
                    "Count phải bằng size của list: count=" + count
                            + ", list=" + userEvents.size());

            // Test 5: Tìm kiếm theo keyword
            EventFilterDTO searchFilter = new EventFilterDTO();
            searchFilter.setKeyword("AI");
            List<Event> searchResult = dao.findAllForUser(searchFilter);
            System.out.println("   Tìm kiếm 'AI': " + searchResult.size() + " kết quả");

            // Test 6: Lọc theo danh mục
            EventFilterDTO catFilter = new EventFilterDTO();
            catFilter.setCategoryId(1);  // Danh mục "Hội thảo"
            List<Event> catResult = dao.findAllForUser(catFilter);
            System.out.println("   Lọc danh mục ID=1 (Hội thảo): "
                    + catResult.size() + " sự kiện");

            // Test 7: Lấy danh sách cho Admin (tất cả status)
            EventFilterDTO adminFilter = new EventFilterDTO();
            adminFilter.setPageSize(10);
            List<Event> adminEvents = dao.findAllForAdmin(adminFilter);
            assertTrue(adminEvents.size() >= userEvents.size(),
                    "Admin thấy nhiều hơn hoặc bằng User: admin="
                            + adminEvents.size() + ", user=" + userEvents.size());
            System.out.println("   Events cho Admin (tất cả): "
                    + adminEvents.size() + " sự kiện");

            // Test 8: Kiểm tra computed methods của Event
            Event completedEvent = dao.findById(6); // Workshop Python (COMPLETED)
            assertNotNull(completedEvent, "Event COMPLETED phải tồn tại");
            System.out.println("   Event COMPLETED (ID=6):");
            System.out.println("   - isEnded(): " + completedEvent.isEnded());
            System.out.println("   - isFull(): " + completedEvent.isFull());
            System.out.println("   - fillRate: "
                    + String.format("%.1f", completedEvent.getFillRatePercent()) + "%");
            System.out.println("   - avgRating: " + completedEvent.getAvgRating());
            System.out.println("   - totalReviews: " + completedEvent.getTotalReviews());

            assertTrue(completedEvent.isEnded(),
                    "Event COMPLETED phải isEnded() = true");

            // Test 9: Gợi ý sự kiện
            List<Event> recommended = dao.findRecommendedForUser(2, 5); // User An
            System.out.println("   Gợi ý cho User An: "
                    + recommended.size() + " sự kiện");

            List<Event> fallback = dao.findRecommendedFallback(5);
            System.out.println("   Fallback recommendations: "
                    + fallback.size() + " sự kiện");

            System.out.println("✅ EventDAO — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ EventDAO FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ EventDAO LỖI NGOÀI Ý MUỐN: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // TEST REGISTRATION DAO
    // ========================================
    static void testRegistrationDAO() {
        System.out.println("--- TEST RegistrationDAO ---");
        RegistrationDAO dao = new RegistrationDAO();

        try {
            // Test 1: Tìm đăng ký tồn tại
            // User An (ID=2) đã đăng ký Event 1
            Registration reg = dao.findByUserAndEvent(2, 1);
            assertNotNull(reg, "User An đã đăng ký Event 1");
            assertEquals("REGISTERED", reg.getStatus(),
                    "Trạng thái phải là REGISTERED");
            System.out.println("   Registration An-Event1: "
                    + reg.getStatus());

            // Test 2: Tìm đăng ký đã hủy
            // User Cường (ID=4) đã hủy Event 1
            Registration cancelled = dao.findByUserAndEvent(4, 1);
            assertNotNull(cancelled, "User Cường có bản ghi với Event 1");
            assertEquals("CANCELLED", cancelled.getStatus(),
                    "Cường đã hủy, phải là CANCELLED");
            System.out.println("   Registration Cường-Event1: "
                    + cancelled.getStatus());

            // Test 3: Không có bản ghi
            Registration notFound = dao.findByUserAndEvent(2, 5); // An chưa đăng ký Event 5
            assertNull(notFound,
                    "User An chưa đăng ký Event 5, phải trả null");

            // Test 4: Lấy tất cả đăng ký của User An
            List<Registration> userRegs = dao.findAllByUser(2);
            assertTrue(userRegs.size() > 0,
                    "User An phải có ít nhất 1 đăng ký");
            System.out.println("   Đăng ký của User An: "
                    + userRegs.size() + " bản ghi");

            // Kiểm tra JOIN fields có dữ liệu không
            for (Registration r : userRegs) {
                assertNotNull(r.getEventTitle(),
                        "Đăng ký phải có eventTitle (từ JOIN)");
                System.out.println("   - " + r.getEventTitle()
                        + " | " + r.getStatus()
                        + " | isEnded=" + r.isEventEnded());
            }

            // Test 5: Lấy đăng ký theo Event
            List<Registration> eventRegs = dao.findAllByEvent(1);
            assertTrue(eventRegs.size() >= 2,
                    "Event 1 phải có ít nhất 2 đăng ký");
            System.out.println("   Đăng ký Event 1: "
                    + eventRegs.size() + " người");

            // Test 6: Tổng đếm
            int total = dao.countTotal();
            assertTrue(total > 0, "Phải có đăng ký: " + total);
            System.out.println("   Tổng đăng ký ACTIVE: " + total);

            // Test 7: Recent registrations
            List<Registration> recent = dao.findRecent(5);
            assertTrue(recent.size() > 0, "Phải có recent registrations");
            System.out.println("   Recent registrations: " + recent.size());

            System.out.println("✅ RegistrationDAO — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ RegistrationDAO FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ RegistrationDAO LỖI NGOÀI Ý MUỐN: "
                    + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // TEST REVIEW DAO
    // ========================================
    static void testReviewDAO() {
        System.out.println("--- TEST ReviewDAO ---");
        ReviewDAO dao = new ReviewDAO();

        try {
            // Test 1: Tìm review tồn tại
            // User An (ID=2) đã review Event 6
            Review review = dao.findByUserAndEvent(2, 6);
            assertNotNull(review, "User An đã review Event 6");
            assertTrue(review.getRating() >= 1 && review.getRating() <= 5,
                    "Rating phải từ 1-5: " + review.getRating());
            assertNotNull(review.getUserFullName(),
                    "Review phải có tên user (từ JOIN)");
            System.out.println("   Review An-Event6: "
                    + review.getRating() + " sao - " + review.getUserFullName());
            System.out.println("   Stars: " + review.getStarsDisplay());

            // Test 2: Review không tồn tại
            Review notFound = dao.findByUserAndEvent(2, 1);
            assertNull(notFound,
                    "An chưa review Event 1 (chưa kết thúc), phải null");

            // Test 3: Lấy tất cả reviews của Event 6
            List<Review> reviews = dao.findAllByEvent(6);
            assertEquals(2, reviews.size(),
                    "Event 6 phải có 2 reviews: " + reviews.size());
            System.out.println("   Reviews Event 6:");
            for (Review r : reviews) {
                System.out.println("   - " + r.getUserFullName()
                        + ": " + r.getRating() + "⭐ - " + r.getComment());
            }

            // Test 4: Tính rating tổng
            double avgRating = dao.getOverallAvgRating();
            System.out.println("   Rating trung bình hệ thống: "
                    + String.format("%.2f", avgRating));

            // Test 5: Đếm tổng reviews
            int count = dao.countAll();
            assertEquals(2, count,
                    "Phải có 2 reviews: " + count);

            System.out.println("✅ ReviewDAO — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ ReviewDAO FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ ReviewDAO LỖI NGOÀI Ý MUỐN: "
                    + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // TEST UTILITY CLASSES
    // ========================================
    static void testUtils() {
        System.out.println("--- TEST Utility Classes ---");

        try {
            // ---- PasswordUtil ----
            System.out.println("   [PasswordUtil]");

            // Hash và verify
            String password = "Admin@123";
            String hash = PasswordUtil.hash(password);
            assertNotNull(hash, "Hash không được null");
            assertTrue(hash.startsWith("$2a$"),
                    "BCrypt hash phải bắt đầu bằng $2a$");
            System.out.println("   Hash: " + hash.substring(0, 20) + "...");

            boolean correct = PasswordUtil.verify(password, hash);
            assertTrue(correct, "Mật khẩu đúng phải verify = true");

            boolean wrong = PasswordUtil.verify("WrongPass123", hash);
            assertFalse(wrong, "Mật khẩu sai phải verify = false");

            // Hash 2 lần → 2 kết quả khác nhau (do salt ngẫu nhiên)
            String hash2 = PasswordUtil.hash(password);
            assertFalse(hash.equals(hash2),
                    "2 lần hash phải cho kết quả khác nhau (do salt)");
            System.out.println("   ✅ BCrypt hash/verify đúng");

            // ---- ValidationUtil ----
            System.out.println("   [ValidationUtil]");

            // isBlank
            assertTrue(ValidationUtil.isBlank(null), "null phải blank");
            assertTrue(ValidationUtil.isBlank(""), "empty phải blank");
            assertTrue(ValidationUtil.isBlank("   "), "spaces phải blank");
            assertFalse(ValidationUtil.isBlank("hello"), "hello không blank");

            // Email validation
            assertTrue(ValidationUtil.isValidEmail("test@example.com"),
                    "Email hợp lệ");
            assertTrue(ValidationUtil.isValidEmail("user.name+tag@domain.co"),
                    "Email phức tạp hợp lệ");
            assertFalse(ValidationUtil.isValidEmail("notanemail"),
                    "Không có @ phải invalid");
            assertFalse(ValidationUtil.isValidEmail("@nodomain"),
                    "Không có tên phải invalid");
            assertFalse(ValidationUtil.isValidEmail(""),
                    "Email rỗng phải invalid");

            // Password validation
            assertTrue(ValidationUtil.isValidPassword("Admin@123"),
                    "Admin@123 phải valid");
            assertFalse(ValidationUtil.isValidPassword("short1A"),
                    "< 8 ký tự phải invalid");
            assertFalse(ValidationUtil.isValidPassword("nouppercase1"),
                    "Không có chữ hoa phải invalid");
            assertFalse(ValidationUtil.isValidPassword("NOLOWERCASE1"),
                    "Không có chữ thường phải invalid");
            assertFalse(ValidationUtil.isValidPassword("NoNumbers!"),
                    "Không có số phải invalid");

            // parseInt
            assertEquals(42, (int) ValidationUtil.parseIntOrNull("42"),
                    "Parse 42 thành công");
            assertNull(ValidationUtil.parseIntOrNull("abc"),
                    "Parse 'abc' phải null");
            assertNull(ValidationUtil.parseIntOrNull(null),
                    "Parse null phải null");
            assertEquals(10,
                    ValidationUtil.parseIntOrDefault("invalid", 10),
                    "Default value phải là 10");

            System.out.println("   ✅ ValidationUtil tất cả đúng");

            // ---- DateUtil ----
            System.out.println("   [DateUtil]");

            // Parse HTML datetime
            java.time.LocalDateTime dt =
                    com.eventhub.util.DateUtil.parseHtmlDateTime("2025-07-20T09:30");
            assertNotNull(dt, "Parse HTML datetime hợp lệ");
            assertEquals(2025, dt.getYear(), "Năm phải là 2025");
            assertEquals(7, dt.getMonthValue(), "Tháng phải là 7");

            java.time.LocalDateTime invalid =
                    com.eventhub.util.DateUtil.parseHtmlDateTime("invalid");
            assertNull(invalid, "Parse datetime không hợp lệ phải null");

            System.out.println("   ✅ DateUtil tất cả đúng");

            System.out.println("✅ Utility Classes — TẤT CẢ TEST PASS\n");

        } catch (AssertionError e) {
            System.out.println("❌ Utility FAIL: " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("❌ Utility LỖI NGOÀI Ý MUỐN: "
                    + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // ========================================
    // HELPER METHODS (thay thế JUnit)
    // ========================================
    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("FAIL — " + message);
        System.out.println("   ✓ " + message);
    }

    static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError("FAIL — " + message);
        System.out.println("   ✓ " + message);
    }

    static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("FAIL — " + message + " (got null)");
        System.out.println("   ✓ " + message);
    }

    static void assertNull(Object obj, String message) {
        if (obj != null) throw new AssertionError("FAIL — " + message + " (not null)");
        System.out.println("   ✓ " + message);
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError("FAIL — " + message
                    + " | expected=" + expected + ", actual=" + actual);
        }
        System.out.println("   ✓ " + message);
    }
}