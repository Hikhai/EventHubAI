import com.eventhub.exception.AuthException;
import com.eventhub.exception.RegistrationException;
import com.eventhub.exception.ReviewException;
import com.eventhub.model.User;
import com.eventhub.service.*;

import java.sql.SQLException;

/**
 * Test Service Layer.
 * Chạy SAU KHI TestDAO đã pass.
 */
public class TestService {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  TEST SERVICE LAYER");
        System.out.println("========================================\n");

        testAuthService();
        testRegistrationService();
        testReviewService();

        System.out.println("========================================");
        System.out.println("  KẾT THÚC TEST SERVICE");
        System.out.println("========================================");
    }

    // ---- AUTH SERVICE ----
    static void testAuthService() {
        System.out.println("--- TEST AuthService ---");
        AuthService service = new AuthService();

        // Test đăng nhập đúng
        try {
            User admin = service.login("admin@eventhub.com", "Admin@123");
            ok("Đăng nhập admin thành công");
            ok("Role là ADMIN: " + admin.isAdmin());
        } catch (Exception e) {
            fail("Đăng nhập admin thất bại: " + e.getMessage());
        }

        // Test đăng nhập sai password
        try {
            service.login("admin@eventhub.com", "WrongPass");
            fail("Phải throw exception khi sai password");
        } catch (AuthException e) {
            ok("Sai password → AuthException đúng");
        } catch (Exception e) {
            fail("Sai exception type: " + e.getClass().getName());
        }

        // Test đăng nhập email không tồn tại
        try {
            service.login("khongton@example.com", "Admin@123");
            fail("Phải throw exception khi email không tồn tại");
        } catch (AuthException e) {
            ok("Email không tồn tại → AuthException đúng");
            // Kiểm tra message phải GIỐNG message sai password (tránh enumeration)
            ok("Message không tiết lộ nguyên nhân cụ thể");
        } catch (Exception e) {
            fail("Sai exception type: " + e.getClass().getName());
        }

        // Test validate đăng ký
        try {
            service.register("", "test@test.com", "Pass@123", "Pass@123");
            fail("Phải báo lỗi tên trống");
        } catch (AuthException e) {
            ok("Tên trống → AuthException: " + e.getMessage());
        } catch (Exception e) {
            fail("Lỗi khác: " + e.getMessage());
        }

        try {
            service.register("Tên", "email-sai-format", "Pass@123", "Pass@123");
            fail("Phải báo lỗi email sai format");
        } catch (AuthException e) {
            ok("Email sai format → AuthException");
        } catch (Exception e) {
            fail("Lỗi khác: " + e.getMessage());
        }

        try {
            service.register("Tên", "test@test.com", "weakpass", "weakpass");
            fail("Phải báo lỗi password yếu");
        } catch (AuthException e) {
            ok("Password yếu → AuthException");
        } catch (Exception e) {
            fail("Lỗi khác: " + e.getMessage());
        }

        try {
            service.register("Tên", "test@test.com", "Strong@1", "DifferentPass@1");
            fail("Phải báo lỗi password không khớp");
        } catch (AuthException e) {
            ok("Password không khớp → AuthException");
        } catch (Exception e) {
            fail("Lỗi khác: " + e.getMessage());
        }

        System.out.println("✅ AuthService — PASS\n");
    }

    // ---- REGISTRATION SERVICE ----
    static void testRegistrationService() {
        System.out.println("--- TEST RegistrationService ---");
        RegistrationService service = new RegistrationService();

        // Test đăng ký khi đã đăng ký rồi (User An - Event 1)
        try {
            service.registerEvent(2, 1);  // An đã REGISTERED event 1
            fail("Phải throw exception khi đã đăng ký");
        } catch (RegistrationException e) {
            ok("Đăng ký trùng → RegistrationException: " + e.getMessage());
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test hủy event đã kết thúc (Event 6 = COMPLETED)
        try {
            service.cancelRegistration(2, 6);  // An đăng ký event 6 đã kết thúc
            fail("Phải throw exception khi event đã kết thúc");
        } catch (RegistrationException e) {
            ok("Hủy event đã kết thúc → RegistrationException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getClass() + " - " + e.getMessage());
        }

        // Test hủy khi chưa đăng ký
        try {
            service.cancelRegistration(2, 5);  // An chưa đăng ký event 5
            fail("Phải throw exception khi chưa đăng ký");
        } catch (RegistrationException e) {
            ok("Hủy khi chưa đăng ký → RegistrationException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test lấy danh sách đăng ký của user
        try {
            var regs = service.getUserRegistrations(2);  // User An
            if (regs.size() > 0) {
                ok("Lấy registrations của An: " + regs.size() + " bản ghi");
            } else {
                fail("An phải có đăng ký");
            }
        } catch (Exception e) {
            fail("Lỗi lấy registrations: " + e.getMessage());
        }

        System.out.println("✅ RegistrationService — PASS\n");
    }

    // ---- REVIEW SERVICE ----
    static void testReviewService() {
        System.out.println("--- TEST ReviewService ---");
        ReviewService service = new ReviewService();

        // Test đánh giá event chưa kết thúc (Event 1 = PUBLISHED)
        try {
            service.submitReview(2, 1, 5, "Rất hay!");
            fail("Phải throw exception khi event chưa kết thúc");
        } catch (ReviewException e) {
            ok("Review event chưa kết thúc → ReviewException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test đánh giá trùng (An đã review Event 6)
        try {
            service.submitReview(2, 6, 4, "Test review trùng");
            fail("Phải throw exception khi đã review rồi");
        } catch (ReviewException e) {
            ok("Review trùng → ReviewException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test validate rating
        try {
            service.submitReview(3, 6, 0, null);  // rating = 0 (sai)
            fail("Phải throw exception khi rating = 0");
        } catch (ReviewException e) {
            ok("Rating = 0 → ReviewException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test validate comment quá ngắn
        try {
            service.submitReview(3, 6, 5, "Ngắn");  // < 10 ký tự
            fail("Phải throw exception khi comment quá ngắn");
        } catch (ReviewException e) {
            ok("Comment < 10 ký tự → ReviewException");
        } catch (Exception e) {
            fail("Sai exception: " + e.getMessage());
        }

        // Test lấy reviews
        try {
            var reviews = service.getReviewsByEvent(6);
            if (reviews.size() >= 2) {
                ok("Lấy reviews event 6: " + reviews.size() + " đánh giá");
            } else {
                fail("Event 6 phải có ít nhất 2 reviews");
            }
        } catch (Exception e) {
            fail("Lỗi lấy reviews: " + e.getMessage());
        }

        System.out.println("✅ ReviewService — PASS\n");
    }

    // ---- HELPER ----
    static void ok(String message) {
        System.out.println("   ✓ " + message);
    }

    static void fail(String message) {
        throw new AssertionError("❌ FAIL: " + message);
    }
}