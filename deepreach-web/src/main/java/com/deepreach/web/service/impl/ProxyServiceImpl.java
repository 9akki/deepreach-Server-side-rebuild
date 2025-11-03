package com.deepreach.web.service.impl;

import com.deepreach.web.domain.dto.BatchDeleteResultDTO;
import com.deepreach.web.entity.Proxy;
import com.deepreach.web.mapper.ProxyMapper;
import com.deepreach.web.service.ProxyService;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 代理池Service实现类
 *
 * 负责代理配置相关的业务逻辑实现，包括：
 * 1. 代理配置管理的具体实现
 * 2. 代理连接测试和验证
 * 3. 数据权限控制实现
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    @Autowired
    private ProxyMapper proxyMapper;

    private static final int CONNECTION_TIMEOUT = 5000; // 连接超时时间（毫秒）

    // ==================== 基础查询方法 ====================

    @Override
    public Proxy selectProxyById(Long proxyId) throws Exception {
        if (proxyId == null || proxyId <= 0) {
            log.warn("查询代理配置失败：代理ID无效 - {}", proxyId);
            return null;
        }

        try {
            Proxy proxy = proxyMapper.selectProxyById(proxyId);
            if (proxy != null) {
                // 检查权限：用户只能查看自己的代理配置
                if (!hasProxyPermission(proxyId)) {
                    log.warn("查询代理配置失败：无权限访问 - proxyId={}, userId={}", proxyId, getCurrentUserId());
                    return null;
                }
                log.debug("查询代理配置成功：proxyId={}, host={}", proxyId, proxy.getProxyHost());
            } else {
                log.debug("查询代理配置失败：代理不存在 - {}", proxyId);
            }
            return proxy;
        } catch (Exception e) {
            log.error("查询代理配置异常：proxyId={}", proxyId, e);
            throw new ServiceException("查询代理配置失败", e);
        }
    }

    @Override
    public List<Proxy> selectProxiesByUserId(Long userId) throws Exception {
        if (userId == null || userId <= 0) {
            log.warn("查询用户代理配置失败：用户ID无效 - {}", userId);
            return new ArrayList<>();
        }

        try {
            // 检查权限：用户只能查看自己的代理配置
            if (!Objects.equals(userId, getCurrentUserId()) && !SecurityUtils.isAdmin(getCurrentUserId())) {
                log.warn("查询用户代理配置失败：无权限访问 - targetUserId={}, currentUserId={}",
                        userId, getCurrentUserId());
                return new ArrayList<>();
            }

            List<Proxy> proxies = proxyMapper.selectProxiesByUserId(userId);
            log.debug("查询用户代理配置成功：userId={}, count={}", userId, proxies.size());
            return proxies;
        } catch (Exception e) {
            log.error("查询用户代理配置异常：userId={}", userId, e);
            throw new ServiceException("查询用户代理配置失败", e);
        }
    }

    @Override
    public List<Proxy> selectProxyList(Proxy proxy) throws Exception {
        try {
            // 设置当前用户ID，确保只能查询自己的代理配置
            if (proxy != null && proxy.getUserId() == null) {
                proxy.setUserId(getCurrentUserId());
            }

            List<Proxy> proxies = proxyMapper.selectProxyList(proxy);
            log.debug("查询代理配置列表成功：conditions={}, count={}", proxy, proxies.size());
            return proxies;
        } catch (Exception e) {
            log.error("查询代理配置列表异常：conditions={}", proxy, e);
            throw new ServiceException("查询代理配置列表失败", e);
        }
    }

    // ==================== 代理配置管理方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Proxy insertProxy(Proxy proxy) throws Exception {
        // 参数验证
        if (proxy == null) {
            throw new ServiceException("代理配置不能为空");
        }

        try {
            // 设置必要字段
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new ServiceException("用户未登录");
            }

            proxy.setUserId(currentUserId);
            proxy.setCreateTime(LocalDateTime.now());
            proxy.setCreateBy(String.valueOf(currentUserId));

            // 设置默认状态
            if (proxy.getStatus() == null || proxy.getStatus().trim().isEmpty()) {
                proxy.setStatus("0"); // 默认为正常状态
            }

            // 验证代理配置
            if (!proxy.isValid()) {
                throw new ServiceException("代理配置信息不完整");
            }

            if (!proxy.isValidHost()) {
                throw new ServiceException("代理主机地址格式无效");
            }

            if (!proxy.isValidPort()) {
                throw new ServiceException("代理端口号无效");
            }

            // 检查唯一性
            if (!checkProxyAddressUnique(proxy.getProxyHost(), proxy.getProxyPort(),
                    proxy.getUserId(), null)) {
                throw new ServiceException("代理地址和端口已存在");
            }

            // 测试代理连接（暂时跳过连接测试）
            // Map<String, Object> testResult = testProxyConnection(proxy);
            // if (!(Boolean) testResult.get("success")) {
            //     throw new ServiceException("代理连接测试失败：" + testResult.get("message"));
            // }

            // 插入记录（密码使用明文存储）
            int result = proxyMapper.insertProxy(proxy);
            if (result <= 0) {
                throw new ServiceException("创建代理配置失败");
            }

            log.info("创建代理配置成功：proxyId={}, host={}, port={}",
                    proxy.getProxyId(), proxy.getProxyHost(), proxy.getProxyPort());
            return proxy;
        } catch (Exception e) {
            log.error("创建代理配置异常：host={}, port={}", proxy.getProxyHost(), proxy.getProxyPort(), e);
            throw new ServiceException("创建代理配置失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProxy(Proxy proxy) throws Exception {
        // 参数验证
        if (proxy == null || proxy.getProxyId() == null) {
            throw new ServiceException("代理配置ID不能为空");
        }

        try {
            // 检查代理是否存在和权限
            Proxy existingProxy = selectProxyById(proxy.getProxyId());
            if (existingProxy == null) {
                throw new ServiceException("代理配置不存在");
            }

            // 检查唯一性（如果修改了地址或端口）
            String newHost = proxy.getProxyHost();
            String newPort = proxy.getProxyPort();
            if ((newHost != null && !newHost.equals(existingProxy.getProxyHost())) ||
                (newPort != null && !newPort.equals(existingProxy.getProxyPort()))) {

                if (!checkProxyAddressUnique(newHost, newPort, getCurrentUserId(), proxy.getProxyId())) {
                    throw new ServiceException("代理地址和端口已存在");
                }
            }

            // 设置更新信息
            proxy.setUpdateTime(LocalDateTime.now());
            proxy.setUpdateBy(String.valueOf(getCurrentUserId()));

            // 如果地址或端口有更新，测试连接（暂时跳过）
            if ((newHost != null && !newHost.equals(existingProxy.getProxyHost())) ||
                (newPort != null && !newPort.equals(existingProxy.getProxyPort()))) {
                // Map<String, Object> testResult = testProxyConnection(proxy);
                // if (!(Boolean) testResult.get("success")) {
                //     throw new ServiceException("代理连接测试失败：" + testResult.get("message"));
                // }
            }

            // 更新记录（密码使用明文存储）
            int result = proxyMapper.updateProxy(proxy);
            if (result <= 0) {
                throw new ServiceException("更新代理配置失败");
            }

            log.info("更新代理配置成功：proxyId={}, host={}, port={}",
                    proxy.getProxyId(), proxy.getProxyHost(), proxy.getProxyPort());
            return true;
        } catch (Exception e) {
            log.error("更新代理配置异常：proxyId={}", proxy.getProxyId(), e);
            throw new ServiceException("更新代理配置失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProxyById(Long proxyId) throws Exception {
        if (proxyId == null || proxyId <= 0) {
            throw new ServiceException("代理配置ID不能为空");
        }

        try {
            // 检查代理是否存在和权限
            Proxy proxy = selectProxyById(proxyId);
            if (proxy == null) {
                // 代理不存在，但返回成功（幂等操作）
                log.info("删除代理配置：代理不存在，幂等操作成功 - proxyId={}", proxyId);
                return true;
            }

            // 删除记录
            int result = proxyMapper.deleteProxyById(proxyId);
            if (result <= 0) {
                // 数据库删除失败，但代理确实存在过，记录警告
                log.warn("删除代理配置：数据库删除失败 - proxyId={}, host={}", proxyId, proxy.getProxyHost());
                return false;
            }

            log.info("删除代理配置成功：proxyId={}, host={}", proxyId, proxy.getProxyHost());
            return true;
        } catch (Exception e) {
            log.error("删除代理配置异常：proxyId={}", proxyId, e);
            throw new ServiceException("删除代理配置失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProxyByIds(List<Long> proxyIds) throws Exception {
        if (proxyIds == null || proxyIds.isEmpty()) {
            throw new ServiceException("代理配置ID列表不能为空");
        }

        try {
            // 检查权限和存在性
            for (Long proxyId : proxyIds) {
                Proxy proxy = selectProxyById(proxyId);
                if (proxy == null) {
                    throw new ServiceException("代理配置不存在：" + proxyId);
                }
            }

            // 批量删除
            int result = proxyMapper.deleteProxyByIds(proxyIds);
            if (result <= 0) {
                throw new ServiceException("批量删除代理配置失败");
            }

            log.info("批量删除代理配置成功：count={}, proxyIds={}", result, proxyIds);
            return true;
        } catch (Exception e) {
            log.error("批量删除代理配置异常：proxyIds={}", proxyIds, e);
            throw new ServiceException("批量删除代理配置失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchDeleteResultDTO deleteProxyByIdsWithResult(List<Long> proxyIds) throws Exception {
        if (proxyIds == null || proxyIds.isEmpty()) {
            throw new ServiceException("代理配置ID列表不能为空");
        }

        List<Long> successIds = new ArrayList<>();
        List<BatchDeleteResultDTO.DeleteFailureInfo> failures = new ArrayList<>();

        for (Long proxyId : proxyIds) {
            try {
                // 检查代理是否存在和权限
                Proxy proxy = selectProxyById(proxyId);
                if (proxy == null) {
                    // 代理不存在，记录为成功（幂等操作）
                    successIds.add(proxyId);
                    log.info("批量删除代理：代理不存在，幂等操作成功 - proxyId={}", proxyId);
                    continue;
                }

                // 删除记录
                int result = proxyMapper.deleteProxyById(proxyId);
                if (result > 0) {
                    successIds.add(proxyId);
                    log.info("批量删除代理：删除成功 - proxyId={}, host={}", proxyId, proxy.getProxyHost());
                } else {
                    // 数据库删除失败
                    failures.add(new BatchDeleteResultDTO.DeleteFailureInfo(proxyId, "数据库删除失败"));
                    log.warn("批量删除代理：数据库删除失败 - proxyId={}, host={}", proxyId, proxy.getProxyHost());
                }
            } catch (Exception e) {
                // 单个代理删除失败
                failures.add(new BatchDeleteResultDTO.DeleteFailureInfo(proxyId, "删除失败：" + e.getMessage()));
                log.error("批量删除代理：单个删除失败 - proxyId={}", proxyId, e);
            }
        }

        // 创建结果对象
        BatchDeleteResultDTO result = BatchDeleteResultDTO.partial(
                proxyIds.size(),
                successIds,
                failures
        );

        log.info("批量删除代理配置完成：totalCount={}, successCount={}, failureCount={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProxyStatus(Long proxyId, String status) throws Exception {
        if (proxyId == null || proxyId <= 0) {
            throw new ServiceException("代理配置ID不能为空");
        }

        if (status == null || (!"0".equals(status) && !"1".equals(status))) {
            throw new ServiceException("状态值无效");
        }

        try {
            // 检查代理是否存在和权限
            Proxy proxy = selectProxyById(proxyId);
            if (proxy == null) {
                throw new ServiceException("代理配置不存在");
            }

            // 更新状态
            int result = proxyMapper.updateProxyStatus(proxyId, status);
            if (result <= 0) {
                throw new ServiceException("更新代理状态失败");
            }

            String statusText = "0".equals(status) ? "启用" : "弃用";
            log.info("更新代理状态成功：proxyId={}, status={}", proxyId, statusText);
            return true;
        } catch (Exception e) {
            log.error("更新代理状态异常：proxyId={}, status={}", proxyId, status, e);
            throw new ServiceException("更新代理状态失败：" + e.getMessage(), e);
        }
    }

    // ==================== 代理连接测试方法 ====================

    @Override
    public Map<String, Object> testProxyConnection(Long proxyId) throws Exception {
        Proxy proxy = selectProxyById(proxyId);
        if (proxy == null) {
            throw new ServiceException("代理配置不存在");
        }
        return testProxyConnection(proxy);
    }

    @Override
    public Map<String, Object> testProxyConnection(Proxy proxy) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            // 参数验证
            if (proxy == null || proxy.getProxyHost() == null || proxy.getProxyPort() == null) {
                result.put("success", false);
                result.put("message", "代理配置信息不完整");
                return result;
            }

            String host = proxy.getProxyHost().trim();
            String portStr = proxy.getProxyPort().trim();
            int port;

            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("message", "端口号格式无效");
                return result;
            }

            // 测试连接
            long startTime = System.currentTimeMillis();
            boolean isConnected = testSocketConnection(host, port);
            long responseTime = System.currentTimeMillis() - startTime;

            if (isConnected) {
                result.put("success", true);
                result.put("message", "代理连接成功");
                result.put("responseTime", responseTime);
                result.put("host", host);
                result.put("port", port);
            } else {
                result.put("success", false);
                result.put("message", "代理连接失败，请检查代理配置");
                result.put("responseTime", responseTime);
                result.put("host", host);
                result.put("port", port);
            }

            log.debug("代理连接测试完成：host={}, port={}, success={}, responseTime={}ms",
                    host, port, result.get("success"), responseTime);

        } catch (Exception e) {
            log.error("代理连接测试异常：host={}, port={}", proxy.getProxyHost(), proxy.getProxyPort(), e);
            result.put("success", false);
            result.put("message", "连接测试异常：" + e.getMessage());
        }

        return result;
    }

    /**
     * 测试Socket连接
     */
    private boolean testSocketConnection(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            return true;
        } catch (Exception e) {
            log.debug("Socket连接失败：host={}, port={}, error={}", host, port, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    log.warn("关闭Socket连接异常", e);
                }
            }
        }
    }

    // ==================== 验证和权限方法 ====================

    @Override
    public boolean checkProxyAddressUnique(String host, String port, Long userId, Long proxyId) throws Exception {
        try {
            int count = proxyMapper.checkProxyAddressUnique(host, port, userId, proxyId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查代理地址唯一性异常：host={}, port={}, userId={}", host, port, userId, e);
            throw new ServiceException("检查代理地址唯一性失败", e);
        }
    }

    @Override
    public boolean hasProxyPermission(Long proxyId) throws Exception {
        try {
            // 管理员可以访问所有代理
            if (SecurityUtils.isAdmin(getCurrentUserId())) {
                return true;
            }

            // 检查代理所有权
            return proxyMapper.checkProxyOwnership(proxyId, getCurrentUserId());
        } catch (Exception e) {
            log.error("检查代理权限异常：proxyId={}", proxyId, e);
            return false;
        }
    }

    // ==================== 可用代理查询方法 ====================

    @Override
    public List<Proxy> getAvailableProxies(Integer proxyType) throws Exception {
        try {
            Long userId = getCurrentUserId();
            List<Proxy> proxies = proxyMapper.selectAvailableProxies(userId, proxyType);
            log.debug("查询可用代理成功：userId={}, proxyType={}, count={}", userId, proxyType, proxies.size());
            return proxies;
        } catch (Exception e) {
            log.error("查询可用代理异常：proxyType={}", proxyType, e);
            throw new ServiceException("查询可用代理失败", e);
        }
    }

    @Override
    public Proxy getRandomAvailableProxy(Integer proxyType) throws Exception {
        try {
            Long userId = getCurrentUserId();
            Proxy proxy = proxyMapper.selectRandomAvailableProxy(userId, proxyType);
            if (proxy != null) {
                log.debug("随机获取可用代理成功：proxyId={}, type={}", proxy.getProxyId(), proxy.getProxyType());
            } else {
                log.debug("随机获取可用代理失败：没有可用的代理");
            }
            return proxy;
        } catch (Exception e) {
            log.error("随机获取可用代理异常：proxyType={}", proxyType, e);
            throw new ServiceException("随机获取可用代理失败", e);
        }
    }

    // ==================== 批量操作方法 ====================

    @Override
    public List<Map<String, Object>> batchTestProxyConnections(List<Long> proxyIds) throws Exception {
        if (proxyIds == null || proxyIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Long proxyId : proxyIds) {
            try {
                Map<String, Object> result = testProxyConnection(proxyId);
                result.put("proxyId", proxyId);
                results.add(result);
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("proxyId", proxyId);
                errorResult.put("success", false);
                errorResult.put("message", "测试异常：" + e.getMessage());
                results.add(errorResult);
            }
        }

        log.info("批量测试代理连接完成：count={}, successCount={}",
                proxyIds.size(), results.stream().mapToInt(r -> (Boolean) r.get("success") ? 1 : 0).sum());
        return results;
    }

    // ==================== 统计方法 ====================

    @Override
    public Map<String, Object> getProxyStatistics() throws Exception {
        try {
            Long userId = getCurrentUserId();
            Map<String, Object> statistics = new HashMap<>();

            // 总数统计
            int totalCount = proxyMapper.countProxiesByUserId(userId);
            statistics.put("totalCount", totalCount);

            // 按类型统计
            int httpCount = proxyMapper.countProxiesByType(0, userId);
            int socks5Count = proxyMapper.countProxiesByType(1, userId);
            statistics.put("httpCount", httpCount);
            statistics.put("socks5Count", socks5Count);

            // 按状态统计
            List<Proxy> normalProxies = proxyMapper.selectProxiesByStatus("0", userId);
            List<Proxy> disabledProxies = proxyMapper.selectProxiesByStatus("1", userId);
            statistics.put("normalCount", normalProxies.size());
            statistics.put("disabledCount", disabledProxies.size());

            // 认证统计
            List<Proxy> authProxies = proxyMapper.selectProxiesWithAuthentication(userId);
            List<Proxy> noAuthProxies = proxyMapper.selectProxiesWithoutAuthentication(userId);
            statistics.put("authCount", authProxies.size());
            statistics.put("noAuthCount", noAuthProxies.size());

            log.debug("获取代理统计信息成功：userId={}", userId);
            return statistics;
        } catch (Exception e) {
            log.error("获取代理统计信息异常", e);
            throw new ServiceException("获取代理统计信息失败", e);
        }
    }

    @Override
    public Map<String, Object> getUserProxyStatistics(Long userId) throws Exception {
        // 检查权限
        if (!Objects.equals(userId, getCurrentUserId()) && !SecurityUtils.isAdmin(getCurrentUserId())) {
            throw new ServiceException("无权限查看指定用户的统计信息");
        }
        return getProxyStatistics();
    }

    // ==================== 密码加密解密方法（明文存储） ====================

    @Override
    public String encryptProxyPassword(String password) throws Exception {
        // 目前使用明文存储，直接返回原密码
        return password;
    }

    @Override
    public String decryptProxyPassword(String encryptedPassword) throws Exception {
        // 目前使用明文存储，直接返回原密码
        return encryptedPassword;
    }

    // ==================== 其他方法的简化实现 ====================

    @Override
    public Map<String, Object> importProxies(List<Proxy> proxies, boolean updateSupport) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", 0);
        result.put("failureCount", 0);
        result.put("message", "功能待实现");
        return result;
    }

    @Override
    public byte[] exportProxies(List<Proxy> proxies) throws Exception {
        throw new ServiceException("功能待实现");
    }

    @Override
    public Map<String, Object> checkProxyPoolHealth() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "功能待实现");
        return result;
    }

    @Override
    public Map<String, Object> cleanupInvalidProxies(Integer days) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "功能待实现");
        return result;
    }

    @Override
    public Proxy copyProxyConfig(Long proxyId, String newHost, String newPort) throws Exception {
        throw new ServiceException("功能待实现");
    }

    @Override
    public List<String> getProxyUsageRecommendations() throws Exception {
        return Arrays.asList("功能待实现");
    }

    @Override
    public Map<String, Object> validateProxyConfig(Proxy proxy) throws Exception {
        Map<String, Object> result = new HashMap<>();

        if (proxy == null) {
            result.put("valid", false);
            result.put("errors", Arrays.asList("代理配置不能为空"));
            return result;
        }

        List<String> errors = new ArrayList<>();

        if (!proxy.isValidHost()) {
            errors.add("代理主机地址格式无效");
        }

        if (!proxy.isValidPort()) {
            errors.add("代理端口号无效");
        }

        if (proxy.getProxyType() == null || (proxy.getProxyType() != 0 && proxy.getProxyType() != 1)) {
            errors.add("代理类型无效");
        }

        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
            return null;
        }
    }
}