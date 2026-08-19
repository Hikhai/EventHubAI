import com.eventhub.util.PasswordUtil;

/**
 * File tiện ích: Tạo BCrypt hash cho các password mẫu.
 * Chạy 1 lần để lấy hash, sau đó update vào DB.
 */
public class GenerateHash {

    public static void main(String[] args) {
        System.out.println("=== TẠO BCRYPT HASH ===\n");

        // Danh sách password cần hash
        String[] passwords = {
                "Admin@123",   // Cho admin
                "User@123"     // Cho các user
        };

        for (String password : passwords) {
            String hash = PasswordUtil.hash(password);
            System.out.println("Password : " + password);
            System.out.println("Hash     : " + hash);
            System.out.println("Verify   : " + PasswordUtil.verify(password, hash));
            System.out.println();
        }
    }
}