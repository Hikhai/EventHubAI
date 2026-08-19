// src/test/java/DebugLogin.java
import com.eventhub.dao.UserDAO;
import com.eventhub.model.User;
import com.eventhub.util.PasswordUtil;

public class DebugLogin {
    public static void main(String[] args) throws Exception {
        UserDAO dao = new UserDAO();

        // Lấy user từ DB
        User admin = dao.findByEmail("admin@eventhub.com");

        if (admin == null) {
            System.out.println("❌ Không tìm thấy admin trong DB!");
            return;
        }

        System.out.println("Tìm thấy user: " + admin.getFullName());
        System.out.println("Email: " + admin.getEmail());
        System.out.println("Hash trong DB: " + admin.getPassword());
        System.out.println("Hash bắt đầu bằng $2a$: "
                + admin.getPassword().startsWith("$2a$"));

        // Test verify
        boolean ok = PasswordUtil.verify("Admin@123", admin.getPassword());
        System.out.println("Verify Admin@123: " + ok);
    }
}