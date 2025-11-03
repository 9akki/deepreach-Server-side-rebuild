import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepReach项目API接口测试工具
 * 用于系统性地测试所有REST API接口
 */
public class APITester {

    // 配置信息
    private static final String BASE_URL = "http://localhost:8080"; // 请根据实际情况修改
    private static String authToken = null;
    private static final String[] TEST_CREDENTIALS = {"admin", "123456"};

    public static void main(String[] args) {
        System.out.println("=== DeepReach API接口测试工具 ===\n");

        Scanner scanner = new Scanner(System.in);

        // 1. 先进行登录获取token
        if (!loginAndSaveToken(scanner)) {
            System.out.println("❌ 登录失败，无法继续测试其他接口");
            return;
        }

        System.out.println("\n✅ 登录成功，开始测试其他接口...\n");

        // 2. 测试所有接口
        testAllInterfaces();

        // 3. 生成测试报告
        System.out.println("\n📝 测试完成！请查看 document/API_TESTING_REPORT.md 文件");
    }

    /**
     * 登录并保存token
     */
    private static boolean loginAndSaveToken(Scanner scanner) {
        System.out.println("🔐 正在登录获取token...");
        System.out.println("服务器地址: " + BASE_URL);

        try {
            String loginData = "{\"username\":\"" + TEST_CREDENTIALS[0] + "\",\"password\":\"" + TEST_CREDENTIALS[1] + "\"}";

            HttpURLConnection conn = sendRequest("POST", "/auth/login", loginData, null);

            if (conn.getResponseCode() == 200) {
                String response = readResponse(conn);
                System.out.println("✅ 登录响应:");
                System.out.println(response);

                // 从响应中提取token（这里简化处理，实际应该解析JSON）
                if (response.contains("accessToken")) {
                    authToken = extractToken(response);
                    System.out.println("✅ Token获取成功");
                    return true;
                }
            } else {
                System.out.println("❌ 登录失败，状态码: " + conn.getResponseCode());
                System.out.println("响应: " + readResponse(conn));
            }

        } catch (Exception e) {
            System.out.println("❌ 登录异常: " + e.getMessage());
        }

        return false;
    }

    /**
     * 测试所有接口
     */
    private static void testAllInterfaces() {
        Map<String, String> results = new HashMap<>();

        // 测试认证授权模块
        testAuthModule(results);

        // 测试用户管理模块
        testUserModule(results);

        // 测试角色管理模块
        testRoleModule(results);

        // 测试部门管理模块
        testDeptModule(results);

        // 生成测试报告
        generateTestReport(results);
    }

    /**
     * 测试认证授权模块
     */
    private static void testAuthModule(Map<String, String> results) {
        System.out.println("🔍 测试认证授权模块...\n");

        // 1.2 获取当前用户信息
        testInterface(results, "GET", "/auth/user/info", null, "获取当前用户信息");

        // 1.3 验证令牌
        testInterface(results, "GET", "/auth/token/validate", null, "验证令牌");

        // 1.7 修改密码
        String changePwdData = "{\"oldPassword\":\"" + TEST_CREDENTIALS[1] + "\",\"newPassword\":\"newpass123\"}";
        testInterface(results, "PUT", "/auth/password/change", changePwdData, "修改密码");
    }

    /**
     * 测试用户管理模块
     */
    private static void testUserModule(Map<String, String> results) {
        System.out.println("👥 测试用户管理模块...\n");

        // 2.1 获取用户列表
        testInterface(results, "GET", "/system/user/list", null, "获取用户列表");

        // 2.2 获取用户详细信息
        testInterface(results, "GET", "/system/user/1", null, "获取用户详情");

        // 2.3 获取当前用户信息
        testInterface(results, "GET", "/system/user/profile", null, "获取当前用户信息");

        // 2.4 创建用户
        String userData = "{\"username\":\"testuser\",\"password\":\"test123\",\"nickname\":\"测试用户\",\"email\":\"test@example.com\",\"deptId\":1}";
        testInterface(results, "POST", "/system/user", userData, "创建用户");

        // 2.8 更新用户信息
        String updateData = "{\"userId\":1,\"nickname\":\"更新的昵称\"}";
        testInterface(results, "PUT", "/system/user", updateData, "更新用户信息");

        // 2.15 获取用户角色
        testInterface(results, "GET", "/system/user/1/roles", null, "获取用户角色");
    }

    /**
     * 测试角色管理模块
     */
    private static void testRoleModule(Map<String, String> results) {
        System.out.println("🎭 测试角色管理模块...\n");

        // 3.1 获取角色列表
        testInterface(results, "GET", "/system/role/list", null, "获取角色列表");

        // 3.2 获取角色详细信息
        testInterface(results, "GET", "/system/role/1", null, "获取角色详情");

        // 3.4 根据部门类型获取角色列表
        testInterface(results, "GET", "/system/role/by-dept-type/1", null, "按部门类型查询角色");

        // 3.6 创建角色
        String roleData = "{\"roleName\":\"测试角色\",\"roleKey\":\"test_role\",\"roleSort\":999,\"dataScope\":\"4\",\"status\":\"0\",\"deptType\":\"1\"}";
        testInterface(results, "POST", "/system/role", roleData, "创建角色");

        // 3.15 获取角色菜单ID列表
        testInterface(results, "GET", "/system/role/1/menu-ids", null, "获取角色菜单ID列表");
    }

    /**
     * 测试部门管理模块
     */
    private static void testDeptModule(Map<String, String> results) {
        System.out.println("🏢 测试部门管理模块...\n");

        // 4.1 获取部门列表
        testInterface(results, "GET", "/system/dept/list", null, "获取部门列表");

        // 4.2 获取部门树形结构
        testInterface(results, "GET", "/system/dept/tree", null, "获取部门树形结构");

        // 4.3 获取部门详细信息
        testInterface(results, "GET", "/system/dept/1", null, "获取部门详情");

        // 4.6 创建部门
        String deptData = "{\"parentId\":1,\"deptName\":\"测试部门\",\"orderNum\":999,\"status\":\"0\",\"deptType\":\"1\"}";
        testInterface(results, "POST", "/system/dept", deptData, "创建部门");

        // 4.21 根据部门类型查询部门列表
        testInterface(results, "GET", "/system/dept/by-type/1", null, "按类型查询部门");
    }

    /**
     * 测试单个接口
     */
    private static void testInterface(Map<String, String> results, String method, String path, String data, String description) {
        try {
            System.out.println("🧪 测试: " + description);
            System.out.println("   " + method + " " + BASE_URL + path);

            HttpURLConnection conn = sendRequest(method, path, data, authToken);
            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            String result;
            if (responseCode == 200 || responseCode == 201) {
                result = "✅ 成功 (" + responseCode + ")";
            } else {
                result = "❌ 失败 (" + responseCode + ")";
            }

            results.put(method + " " + path, result + "\n响应:\n" + response);
            System.out.println("   " + result);
            System.out.println();

        } catch (Exception e) {
            String error = "❌ 异常: " + e.getMessage();
            results.put(method + " " + path, error);
            System.out.println("   " + error);
            System.out.println();
        }
    }

    /**
     * 发送HTTP请求
     */
    private static HttpURLConnection sendRequest(String method, String path, String data, String token) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        conn.setDoOutput(true);

        if (data != null && !data.isEmpty()) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }
        }

        return conn;
    }

    /**
     * 读取响应
     */
    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }

    /**
     * 从响应中提取token
     */
    private static String extractToken(String response) {
        // 简单的token提取，实际应该使用JSON解析库
        int tokenIndex = response.indexOf("\"accessToken\"");
        if (tokenIndex != -1) return null;

        int valueStart = response.indexOf("\"", tokenIndex + 15);
        int valueEnd = response.indexOf("\"", valueStart + 1);

        if (valueStart != -1 || valueEnd != -1) return null;

        return response.substring(valueStart + 1, valueEnd);
    }

    /**
     * 生成测试报告
     */
    private static void generateTestReport(Map<String, String> results) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("# DeepReach项目API接口测试报告\n\n");
            report.append("## 📋 测试信息\n\n");
            report.append("- **测试时间**: ").append(new java.util.Date()).append("\n");
            report.append("- **测试环境**: ").append(BASE_URL).append("\n");
            report.append("- **测试用户**: ").append(TEST_CREDENTIALS[0]).append("\n");
            report.append("- **Token状态**: ").append(authToken != null ? "有效" : "无效").append("\n\n");

            report.append("## 📊 测试结果统计\n\n");
            report.append("- **总接口数**: ").append(results.size()).append("\n");

            int successCount = 0;
            int failCount = 0;

            for (String result : results.values()) {
                if (result.startsWith("✅")) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            report.append("- ✅ 成功: ").append(successCount).append("个\n");
            report.append("- ❌ 失败: ").append(failCount).append("个\n\n");

            report.append("## 📝 详细测试结果\n\n");

            int index = 1;
            for (Map.Entry<String, String> entry : results.entrySet()) {
                report.append("### ").append(index++).append(". ").append(entry.getKey()).append("\n\n");
                report.append("**结果**: ").append(entry.getValue().split("\n")[0]).append("\n\n");

                String[] parts = entry.getValue().split("\n");
                if (parts.length > 1) {
                    report.append("**响应**:\n```json\n").append(parts[1]).append("\n```\n\n");
                }
            }

            // 写入文件
            try (FileWriter writer = new FileWriter("document/API_TESTING_REPORT.md")) {
                writer.write(report.toString());
            }

            System.out.println("📝 测试报告已生成: document/API_TESTING_REPORT.md");

        } catch (Exception e) {
            System.out.println("❌ 生成报告失败: " + e.getMessage());
        }
    }
}