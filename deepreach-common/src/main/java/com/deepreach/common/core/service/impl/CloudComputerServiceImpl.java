package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.CloudUser;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.dto.CloudComputerParameterDTO;
import com.deepreach.common.core.service.CloudComputerService;
import com.deepreach.common.core.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 云电脑服务实现类
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Slf4j
@Service
public class CloudComputerServiceImpl implements CloudComputerService {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    @Qualifier("cloudComputerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    private static final String DEFAULT_ACCESS_KEY_ID = "LTAI5tKDpAnhZr52ttZfyTp7";
    private static final String DEFAULT_ACCESS_KEY_SECRET = "yPLYk8hkTCfCW0mZdbqyQ1SzAMEmAr";
    private static final String DEFAULT_ENDPOINT = "https://ecd.us-west-1.aliyuncs.com";
    private static final String DEFAULT_API_VERSION = "2020-10-02";
    private static final String DEFAULT_ACTION = "GetLoginToken";
    private static final String DEFAULT_REGION_ID = "us-west-1";
    private static final String DEFAULT_OFFICE_SITE_ID = "us-west-1+dir-5588339126";

    @Override
    public CloudComputerParameterDTO getCloudComputerParameter(Long userId) {
        try {
            log.info("开始获取用户{}的云电脑参数", userId);

            // 1. 获取用户信息
            SysUser user = sysUserService.selectUserById(userId);
            if (user == null) {
                log.warn("用户不存在：{}", userId);
                return CloudComputerParameterDTO.fail("用户不存在");
            }

            String username = user.getUsername();
            log.debug("用户名：{}", username);

            // 2. 检查用户是否已分配云电脑
            String checkCloudUserSql = "SELECT COUNT(*) FROM t_cloud_user WHERE client_username = ?";
            Integer cloudUserCount = jdbcTemplate.queryForObject(checkCloudUserSql, Integer.class, username);

            if (cloudUserCount == null || cloudUserCount == 0) {
                log.info("用户{}未分配云电脑", username);
                return CloudComputerParameterDTO.fail("未分配云电脑");
            }

            log.info("用户{}已分配云电脑，开始获取参数", username);

            // 3. 获取云电脑参数
            return getCloudComputerParameters(username);

        } catch (Exception e) {
            log.error("获取云电脑参数失败：用户ID={}", userId, e);
            return CloudComputerParameterDTO.fail("获取云电脑参数失败：" + e.getMessage());
        }
    }

    /**
     * 获取云电脑详细参数
     */
    private CloudComputerParameterDTO getCloudComputerParameters(String username) {
        try {
            // 4. 获取云电脑用户信息
            CloudUser cloudUser = fetchCloudUser(username);
            if (cloudUser == null) {
                log.error("未找到云电脑用户信息：{}", username);
                return CloudComputerParameterDTO.fail("未找到用户云电脑配置");
            }

            String endUserId = cloudUser.getEndUserId();
            if (isBlank(endUserId)) {
                log.error("云电脑用户缺少end_user_id：{}", username);
                return CloudComputerParameterDTO.fail("未找到用户云电脑配置");
            }
            log.debug("end_user_id：{}", endUserId);

            // 5. 获取computer_id
            String computerId = fetchComputerId(endUserId);
            if (isBlank(computerId)) {
                log.error("未找到用户的computer_id：end_user_id={}", endUserId);
                return CloudComputerParameterDTO.fail("未找到用户云电脑信息");
            }
            log.debug("computer_id：{}", computerId);

            // 6. 获取office_site_id
            String officeSiteId = fetchOfficeSiteId(computerId);
            if (isBlank(officeSiteId)) {
                log.error("未找到云电脑的office_site_id：computer_id={}", computerId);
                return CloudComputerParameterDTO.fail("未找到云电脑配置信息");
            }
            log.debug("office_site_id：{}", officeSiteId);

            // 7. 解析登录区域
            String loginRegionId = extractRegionFromOfficeSiteId(officeSiteId);
            log.debug("loginRegionId：{}", loginRegionId);

            // 8. 加载阿里云配置
            Map<String, String> configMap = loadAliyunConfig();

            // 根据endUserId优先加载用户级配置，缺失时回退到全局配置
            String accessKeyId = resolveConfig(configMap, endUserId, "access.key.id");
            String accessKeySecret = resolveConfig(configMap, endUserId, "access.key.secret");
            String endpoint = resolveConfig(configMap, endUserId, "endpoint");
            String apiVersion = resolveConfig(configMap, endUserId, "api.version");
            String action = resolveConfig(configMap, endUserId, "action");
            String regionId = resolveConfig(configMap, endUserId, "region.id");
            String configuredOfficeSiteId = resolveConfig(configMap, endUserId, "office.site.id");
            String signatureMethod = resolveConfig(configMap, endUserId, "signature.method");
            String signatureVersion = resolveConfig(configMap, endUserId, "signature.version");

            // 如果数据库中没有配置办公站点则使用云电脑表中的数据
            String officeSiteIdForRequest = hasText(configuredOfficeSiteId) ?
                configuredOfficeSiteId : officeSiteId;

            if (isBlank(officeSiteIdForRequest)) {
                log.error("无法确定办公站点ID，用户: {}", endUserId);
                return CloudComputerParameterDTO.fail("云电脑办公站点未配置");
            }

            if (isBlank(accessKeyId) || isBlank(accessKeySecret) ||
                isBlank(endpoint) || isBlank(apiVersion) ||
                isBlank(action)) {
                log.error("阿里云ECD基础配置不完整，用户: {}", endUserId);
                return CloudComputerParameterDTO.fail("阿里云配置不完整");
            }

            if (isBlank(regionId)) {
                regionId = loginRegionId;
            }

            if (isBlank(regionId)) {
                log.error("无法确定区域ID，用户: {}", endUserId);
                return CloudComputerParameterDTO.fail("云电脑区域未配置");
            }

            if (isBlank(signatureMethod)) {
                signatureMethod = "HMAC-SHA1";
            }

            if (isBlank(signatureVersion)) {
                signatureVersion = "1.0";
            }

            String clientId = UUID.randomUUID().toString();

            String userPassword = cloudUser.getPassword();
            if (isBlank(userPassword)) {
                log.error("云电脑用户缺少密码信息，用户: {}", endUserId);
                return CloudComputerParameterDTO.fail("云电脑密码未配置");
            }

            // 9. 获取loginToken
            String loginToken = generateLoginToken(
                accessKeyId,
                accessKeySecret,
                endpoint,
                apiVersion,
                action,
                regionId,
                clientId,
                officeSiteIdForRequest,
                endUserId,
                userPassword,
                signatureMethod,
                signatureVersion
            );

            if (isBlank(loginToken)) {
                log.error("阿里云API未返回loginToken，用户: {}", endUserId);
                return CloudComputerParameterDTO.fail("未获取到登录令牌");
            }

            log.info("成功获取云电脑参数：clientId={}, computerId={}, loginRegionId={}",
                    clientId, computerId, loginRegionId);

            return CloudComputerParameterDTO.success(loginToken, clientId, computerId, loginRegionId);

        } catch (Exception e) {
            log.error("获取云电脑详细参数失败：username={}", username, e);
            return CloudComputerParameterDTO.fail("获取云电脑参数失败：" + e.getMessage());
        }
    }

    /**
     * 生成登录令牌
     *
     * 调用阿里云ECD API获取真实的loginToken
     */
    private String generateLoginToken(String accessKeyId,
                                      String accessKeySecret,
                                      String endpoint,
                                      String apiVersion,
                                      String action,
                                      String regionId,
                                      String clientId,
                                      String officeSiteId,
                                      String endUserId,
                                      String password,
                                      String signatureMethod,
                                      String signatureVersion) throws Exception {
        log.info("调用阿里云ECD API获取loginToken，用户: {}, 办公站点: {}", endUserId, officeSiteId);

        // 调用阿里云ECD GetLoginToken API
        String loginToken = callAliyunEcdGetLoginToken(
            accessKeyId,
            accessKeySecret,
            endpoint,
            apiVersion,
            action,
            regionId,
            clientId,
            officeSiteId,
            endUserId,
            password,
            signatureMethod,
            signatureVersion
        );

        log.info("成功获取真实loginToken: {}", loginToken);
        return loginToken;
    }

    /**
     * 调用阿里云ECD GetLoginToken API
     */
    private String callAliyunEcdGetLoginToken(String accessKeyId,
                                              String accessKeySecret,
                                              String endpoint,
                                              String apiVersion,
                                              String action,
                                              String regionId,
                                              String clientId,
                                              String officeSiteId,
                                              String endUserId,
                                              String password,
                                              String signatureMethod,
                                              String signatureVersion) throws Exception {
        // 构建公共参数
        String timestamp = formatTimestamp(System.currentTimeMillis());
        String nonce = java.util.UUID.randomUUID().toString();

        String normalizedEndpoint = normalizeEndpoint(endpoint);

        // 构建请求参数
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("Action", action);
        params.put("Version", apiVersion);
        params.put("RegionId", regionId);
        params.put("ClientId", clientId);
        params.put("OfficeSiteId", officeSiteId);
        params.put("EndUserId", endUserId);
        params.put("Password", password);
        params.put("Format", "JSON");
        params.put("AccessKeyId", accessKeyId);
        params.put("SignatureMethod", signatureMethod);
        params.put("Timestamp", timestamp);
        params.put("SignatureVersion", signatureVersion);
        params.put("SignatureNonce", nonce);

        // 按字母顺序排序参数用于签名
        java.util.List<String> sortedKeys = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(sortedKeys);

        // 构建查询字符串
        StringBuilder queryString = new StringBuilder();
        for (String key : sortedKeys) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(key).append("=").append(java.net.URLEncoder.encode(params.get(key), "UTF-8"));
        }

        // 构建待签名字符串
        String stringToSign = "POST&%2F&" + java.net.URLEncoder.encode(queryString.toString(), "UTF-8");

        // 计算签名
        String signature = calculateSignature(stringToSign, accessKeySecret);

        // 构建最终URL
        String url = normalizedEndpoint + "/?" + queryString.toString() + "&Signature=" + java.net.URLEncoder.encode(signature, "UTF-8");

        log.info("阿里云API请求URL: {}", url);

        // 发送HTTP请求
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();
        log.info("阿里云API响应码: {}", responseCode);

        if (responseCode == 200) {
            // 读取响应
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            String responseBody = response.toString();
            log.info("阿里云API响应内容: {}", responseBody);

            // 解析响应获取loginToken
            String loginToken = extractLoginTokenFromResponse(responseBody);
            if (isBlank(loginToken)) {
                throw new Exception("阿里云API未返回登录令牌");
            }
            return loginToken;
        } else {
            // 读取错误响应
            java.io.InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                java.io.BufferedReader errorReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(errorStream)
                );
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                log.error("阿里云API错误响应: {}", errorResponse.toString());
            }
            connection.disconnect();
            throw new Exception("阿里云API调用失败，响应码: " + responseCode);
        }
    }

    /**
     * 格式化时间戳为阿里云要求的格式
     */
    private String formatTimestamp(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }

    /**
     * 计算HMAC-SHA1签名
     */
    private String calculateSignature(String stringToSign, String accessKeySecret) throws Exception {
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
            (accessKeySecret + "&").getBytes("UTF-8"), "HmacSHA1"
        );
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] result = mac.doFinal(stringToSign.getBytes("UTF-8"));
        return javax.xml.bind.DatatypeConverter.printBase64Binary(result);
    }

    /**
     * 从阿里云API响应中提取loginToken
     */
    private String extractLoginTokenFromResponse(String response) {
        // 根据阿里云API响应格式解析loginToken
        // 假设响应格式: {"LoginToken":"token_xxx","ClientId":"xxx",...}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"LoginToken\":\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private CloudUser fetchCloudUser(String username) {
        try {
            String sql = "SELECT id, client_username, end_user_id, password FROM t_cloud_user WHERE client_username = ?";
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(CloudUser.class), username);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String fetchComputerId(String endUserId) {
        try {
            String sql = "SELECT computer_id FROM t_user_computer WHERE end_user_id = ? ORDER BY id LIMIT 1";
            return jdbcTemplate.queryForObject(sql, String.class, endUserId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String fetchOfficeSiteId(String computerId) {
        try {
            String sql = "SELECT office_siteId FROM t_computer WHERE computer_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, computerId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, String> loadAliyunConfig() {
        Map<String, String> configMap = new java.util.LinkedHashMap<>();

        // 先加载应用配置中的默认值
        loadDefaultsFromProperties(configMap);

        // 再尝试加载数据库配置，数据库配置优先级更高
        try {
            String sql = "SELECT config_key, config_value FROM t_aliyun_ecd_config WHERE status = 1";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                Object key = row.get("config_key");
                Object value = row.get("config_value");
                if (key instanceof String && value instanceof String) {
                    configMap.put((String) key, (String) value);
                }
            }
        } catch (DataAccessException e) {
            log.warn("读取数据库阿里云配置失败，将使用默认配置。错误: {}", e.getMessage());
        }

        return configMap;
    }

    private void loadDefaultsFromProperties(Map<String, String> target) {
        if (environment == null) {
            applyDefaultConstants(target);
            return;
        }
        addIfPresent(target, "access.key.id", environment.getProperty("aliyun.ecd.access-key-id"));
        addIfPresent(target, "access.key.secret", environment.getProperty("aliyun.ecd.access-key-secret"));
        addIfPresent(target, "endpoint", environment.getProperty("aliyun.ecd.endpoint"));
        addIfPresent(target, "api.version", environment.getProperty("aliyun.ecd.api-version"));
        addIfPresent(target, "action", environment.getProperty("aliyun.ecd.action"));
        addIfPresent(target, "region.id", environment.getProperty("aliyun.ecd.region-id"));
        addIfPresent(target, "office.site.id", environment.getProperty("aliyun.ecd.office-site-id"));

        applyDefaultConstants(target);
    }

    private void addIfPresent(Map<String, String> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value);
        }
    }

    private void applyDefaultConstants(Map<String, String> target) {
        target.putIfAbsent("access.key.id", DEFAULT_ACCESS_KEY_ID);
        target.putIfAbsent("access.key.secret", DEFAULT_ACCESS_KEY_SECRET);
        target.putIfAbsent("endpoint", DEFAULT_ENDPOINT);
        target.putIfAbsent("api.version", DEFAULT_API_VERSION);
        target.putIfAbsent("action", DEFAULT_ACTION);
        target.putIfAbsent("region.id", DEFAULT_REGION_ID);
        target.putIfAbsent("office.site.id", DEFAULT_OFFICE_SITE_ID);
    }

    private String resolveConfig(Map<String, String> configMap, String endUserId, String key) {
        if (configMap == null || configMap.isEmpty()) {
            return null;
        }
        if (hasText(endUserId)) {
            String userSpecificKey = endUserId + "." + key;
            String userValue = configMap.get(userSpecificKey);
            if (hasText(userValue)) {
                return userValue;
            }
        }
        return configMap.get(key);
    }

    private String extractRegionFromOfficeSiteId(String officeSiteId) {
        if (isBlank(officeSiteId)) {
            return null;
        }
        if (officeSiteId.contains("+")) {
            return officeSiteId.substring(0, officeSiteId.indexOf('+'));
        }
        return officeSiteId;
    }

    private String normalizeEndpoint(String endpoint) {
        if (isBlank(endpoint)) {
            return endpoint;
        }
        String trimmed = endpoint.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (trimmed.endsWith("/")) {
                return trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasText(String value) {
        return !isBlank(value);
    }
}
