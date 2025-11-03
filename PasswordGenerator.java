import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * DeepReach项目密码生成工具
 * 用于生成BCrypt加密的密码值
 */
public class PasswordGenerator {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        System.out.println("=== DeepReach项目密码生成工具 ===\n");

        // 推荐的密码选项
        String[] passwords = {
            "DeepReach@2024",
            "Admin@2024",
            "Tech@2024",
            "Product@2024",
            "Market@2024",
            "Dev@2024",
            "Agent@2024",
            "Buyer@2024",
            "User@2024",
            "admin123",
            "password123",
            "Dragon123"
        };

        System.out.println("密码加密结果：");
        System.out.println("================");

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("明文密码: " + password);
            System.out.println("加密值: " + encoded);
            System.out.println("---");
        }

        // 验证密码
        System.out.println("\n=== 密码验证测试 ===");
        String testPassword = "DeepReach@2024";
        String testEncoded = encoder.encode(testPassword);
        System.out.println("原始密码: " + testPassword);
        System.out.println("加密后: " + testEncoded);
        System.out.println("验证结果: " + encoder.matches(testPassword, testEncoded));
    }
}