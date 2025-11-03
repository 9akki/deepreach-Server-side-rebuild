import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * DeepReach项目密码验证工具
 * 用于测试和修复密码验证问题
 */
public class PasswordValidator {

    // 您数据库中现有的加密值
    private static final String EXISTING_HASH = "$2a$10$7JB720yubVSOfvVWbfXCOOHrXvmdxVMV8NVRhGGhvAkykflVGO5aO";

    // 测试明文密码
    private static final String PLAIN_PASSWORD = "123456";

    public static void main(String[] args) {
        System.out.println("=== DeepReach密码验证工具 ===\n");

        // 测试不同强度的BCrypt编码器
        testPasswordEncoderStrength();

        // 测试现有密码哈希
        testExistingPassword();

        // 生成新的加密值
        generateNewEncryptions();

        // 提供修复建议
        provideFixSuggestions();
    }

    /**
     * 测试不同强度的BCrypt编码器
     */
    private static void testPasswordEncoderStrength() {
        System.out.println("🔍 测试不同强度的BCrypt编码器：");
        System.out.println("=====================================");

        int[] strengths = {8, 10, 12};

        for (int strength : strengths) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
            String encoded = encoder.encode(PLAIN_PASSWORD);
            boolean matches = encoder.matches(PLAIN_PASSWORD, encoded);

            System.out.println("强度 " + strength + ":");
            System.out.println("  加密值: " + encoded);
            System.out.println("  验证结果: " + (matches ? "✅ 通过" : "❌ 失败"));
            System.out.println();
        }
    }

    /**
     * 测试现有密码哈希
     */
    private static void testExistingPassword() {
        System.out.println("🔍 测试现有密码哈希：");
        System.out.println("=====================================");

        System.out.println("现有哈希: " + EXISTING_HASH);
        System.out.println("测试密码: " + PLAIN_PASSWORD);

        // 使用默认强度(10)测试
        BCryptPasswordEncoder defaultEncoder = new BCryptPasswordEncoder();
        boolean defaultMatches = defaultEncoder.matches(PLAIN_PASSWORD, EXISTING_HASH);
        System.out.println("默认强度(10)验证: " + (defaultMatches ? "✅ 通过" : "❌ 失败"));

        // 使用不同强度测试
        for (int strength : new int[]{8, 10, 12}) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(strength);
            boolean matches = encoder.matches(PLAIN_PASSWORD, EXISTING_HASH);
            System.out.println("强度" + strength + "验证: " + (matches ? "✅ 通过" : "❌ 失败"));
        }

        System.out.println();
    }

    /**
     * 生成新的加密值
     */
    private static void generateNewEncryptions() {
        System.out.println("🔑 生成新的加密值：");
        System.out.println("=====================================");

        String[] passwords = {"123456", "admin", "password", "123123"};

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            boolean matches = encoder.matches(password, encoded);
            System.out.println("密码: " + password);
            System.out.println("加密: " + encoded);
            System.out.println("验证: " + (matches ? "✅ 通过" : "❌ 失败"));
            System.out.println();
        }
    }

    /**
     * 提供修复建议
     */
    private static void provideFixSuggestions() {
        System.out.println("🛠️ 修复建议：");
        System.out.println("=====================================");

        System.out.println("1. 检查BCrypt编码器配置：");
        System.out.println("   - 确保SecurityConfig中使用的是BCryptPasswordEncoder(10)");
        System.out.println("   - 确保所有地方使用相同的编码器实例");

        System.out.println("\n2. 可能的解决方案：");
        System.out.println("   方案A: 重新生成密码哈希（推荐）");
        System.out.println("   方案B: 修改BCrypt编码器配置");
        System.out.println("   方案C: 添加调试日志确认问题");

        System.out.println("\n3. 推荐的新密码哈希（123456）：");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String newHash = encoder.encode("123456");
        System.out.println("   新哈希值: " + newHash);

        System.out.println("\n4. SQL更新语句：");
        System.out.println("   UPDATE sys_user SET password = '" + newHash + "' WHERE 1=1;");

        System.out.println("\n5. 验证方法：");
        System.out.println("   - 先用admin/123456测试");
        System.out.println("   - 如果失败，查看应用日志");
        System.out.println("   - 检查SecurityConfig配置");
    }
}