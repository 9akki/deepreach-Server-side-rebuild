package com.deepreach.web.controller;

import com.deepreach.web.entity.Proxy;
import com.deepreach.web.service.ProxyService;
import com.deepreach.web.domain.dto.BatchDeleteResultDTO;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代理池管理Controller
 *
 * 负责代理配置管理的RESTful API控制器，包括：
 * 1. 代理配置的增删改查API
 * 2. 代理连接测试API
 * 3. 代理状态管理API
 * 4. 代理统计和查询API
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/proxy")
public class ProxyController extends BaseController {

    @Autowired
    private ProxyService proxyService;

    // ==================== 查询接口 ====================

    /**
     * 获取代理配置列表
     *
     * 支持多条件查询和分页
     * 自动应用用户权限过滤
     *
     * @param proxy 查询条件对象
     * @return 分页代理配置列表
     */
    @GetMapping("/list")
    public TableDataInfo list(Proxy proxy) {
        try {
            startPage(); // 启动分页
            List<Proxy> list = proxyService.selectProxyList(proxy);
            return getDataTable(list);
        } catch (Exception e) {
            log.error("查询代理配置列表失败", e);
            return TableDataInfo.error("查询代理配置列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据代理ID获取详细信息
     *
     * @param proxyId 代理ID
     * @return 代理配置详细信息
     */
    @GetMapping("/{proxyId}")
    public Result getInfo(@PathVariable Long proxyId) {
        try {
            Proxy proxy = proxyService.selectProxyById(proxyId);
            if (proxy == null) {
                return Result.error("代理配置不存在");
            }
            return Result.success(proxy);
        } catch (Exception e) {
            log.error("获取代理配置详情失败：proxyId={}", proxyId, e);
            return Result.error("获取代理配置详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户的代理配置列表
     *
     * @return 代理配置列表
     */
    @GetMapping("/user")
    public Result getUserProxies() {
        try {
            Long userId = getCurrentUserId();
            List<Proxy> proxies = proxyService.selectProxiesByUserId(userId);
            return Result.success(proxies);
        } catch (Exception e) {
            log.error("获取用户代理配置失败", e);
            return Result.error("获取用户代理配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取可用的代理配置列表
     *
     * @param proxyType 代理类型（可选）
     * @return 可用的代理配置列表
     */
    @GetMapping("/available")
    public Result getAvailableProxies(@RequestParam(required = false) Integer proxyType) {
        try {
            List<Proxy> proxies = proxyService.getAvailableProxies(proxyType);
            return Result.success(proxies);
        } catch (Exception e) {
            log.error("获取可用代理配置失败：proxyType={}", proxyType, e);
            return Result.error("获取可用代理配置失败：" + e.getMessage());
        }
    }

    /**
     * 随机获取一个可用的代理配置
     *
     * @param proxyType 代理类型（可选）
     * @return 可用的代理配置
     */
    @GetMapping("/random")
    public Result getRandomAvailableProxy(@RequestParam(required = false) Integer proxyType) {
        try {
            Proxy proxy = proxyService.getRandomAvailableProxy(proxyType);
            if (proxy == null) {
                return Result.error("没有可用的代理配置");
            }
            return Result.success(proxy);
        } catch (Exception e) {
            log.error("随机获取可用代理失败：proxyType={}", proxyType, e);
            return Result.error("随机获取可用代理失败：" + e.getMessage());
        }
    }

    // ==================== 新增接口 ====================

    /**
     * 新增代理配置
     *
     * @param proxy 代理配置对象
     * @return 操作结果
     */
    @PostMapping
    @Log(title = "代理配置", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody Proxy proxy) {
        try {
            Proxy createdProxy = proxyService.insertProxy(proxy);
            return Result.success("新增代理配置成功", createdProxy);
        } catch (Exception e) {
            log.error("新增代理配置失败：host={}, port={}", proxy.getProxyHost(), proxy.getProxyPort(), e);
            return Result.error("新增代理配置失败：" + e.getMessage());
        }
    }

    // ==================== 修改接口 ====================

    /**
     * 修改代理配置
     *
     * @param proxy 代理配置对象
     * @return 操作结果
     */
    @PutMapping
    @Log(title = "代理配置", businessType = BusinessType.UPDATE)
    public Result edit(@Validated @RequestBody Proxy proxy) {
        try {
            boolean success = proxyService.updateProxy(proxy);
            if (success) {
                return Result.success("修改代理配置成功");
            } else {
                return Result.error("修改代理配置失败");
            }
        } catch (Exception e) {
            log.error("修改代理配置失败：proxyId={}", proxy.getProxyId(), e);
            return Result.error("修改代理配置失败：" + e.getMessage());
        }
    }

    /**
     * 更新代理状态
     *
     * @param proxyId 代理ID
     * @param status 状态（0正常 1弃用）
     * @return 操作结果
     */
    @PutMapping("/{proxyId}/status/{status}")
    @Log(title = "代理配置", businessType = BusinessType.UPDATE)
    public Result updateStatus(@PathVariable Long proxyId, @PathVariable String status) {
        try {
            boolean success = proxyService.updateProxyStatus(proxyId, status);
            if (success) {
                String statusText = "0".equals(status) ? "启用" : "弃用";
                return Result.success(statusText + "代理配置成功");
            } else {
                return Result.error("更新代理状态失败");
            }
        } catch (Exception e) {
            log.error("更新代理状态失败：proxyId={}, status={}", proxyId, status, e);
            return Result.error("更新代理状态失败：" + e.getMessage());
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除代理配置
     *
     * @param proxyId 代理ID
     * @return 操作结果
     */
    @DeleteMapping("/{proxyId}")
    @Log(title = "代理配置", businessType = BusinessType.DELETE)
    public Result remove(@PathVariable Long proxyId) {
        try {
            boolean success = proxyService.deleteProxyById(proxyId);
            if (success) {
                return Result.success("删除代理配置成功");
            } else {
                return Result.error("删除代理配置失败");
            }
        } catch (Exception e) {
            log.error("删除代理配置失败：proxyId={}", proxyId, e);
            return Result.error("删除代理配置失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除代理配置
     *
     * @param proxyIds 代理ID数组
     * @return 操作结果
     */
    @DeleteMapping
    @Log(title = "代理配置", businessType = BusinessType.DELETE)
    public Result removes(@RequestBody Long[] proxyIds) {
        try {
            List<Long> proxyIdList = java.util.Arrays.asList(proxyIds);
            BatchDeleteResultDTO result = proxyService.deleteProxyByIdsWithResult(proxyIdList);

            if (result.isAllSuccess()) {
                return Result.success("批量删除代理配置成功", result);
            } else if (result.isPartialSuccess()) {
                return Result.success("批量删除代理配置部分成功", result);
            } else {
                return Result.error(500, "批量删除代理配置失败", result);
            }
        } catch (Exception e) {
            log.error("批量删除代理配置失败：proxyIds={}", java.util.Arrays.toString(proxyIds), e);
            return Result.error("批量删除代理配置失败：" + e.getMessage());
        }
    }

    // ==================== 测试接口 ====================

    /**
     * 测试代理连接
     *
     * @param proxyId 代理ID
     * @return 测试结果
     */
    @PostMapping("/{proxyId}/test")
    public Result testConnection(@PathVariable Long proxyId) {
        try {
            Map<String, Object> result = proxyService.testProxyConnection(proxyId);
            return Result.success("测试完成", result);
        } catch (Exception e) {
            log.error("测试代理连接失败：proxyId={}", proxyId, e);
            return Result.error("测试代理连接失败：" + e.getMessage());
        }
    }

    /**
     * 测试临时代理连接
     *
     * @param proxy 代理配置对象
     * @return 测试结果
     */
    @PostMapping("/test")
    public Result testTempConnection(@RequestBody Proxy proxy) {
        try {
            Map<String, Object> result = proxyService.testProxyConnection(proxy);
            return Result.success("测试完成", result);
        } catch (Exception e) {
            log.error("测试临时代理连接失败：host={}, port={}", proxy.getProxyHost(), proxy.getProxyPort(), e);
            return Result.error("测试代理连接失败：" + e.getMessage());
        }
    }

    /**
     * 批量测试代理连接
     *
     * @param proxyIds 代理ID列表
     * @return 批量测试结果
     */
    @PostMapping("/batch-test")
    public Result batchTestConnections(@RequestBody List<Long> proxyIds) {
        try {
            List<Map<String, Object>> results = proxyService.batchTestProxyConnections(proxyIds);
            return Result.success("批量测试完成", results);
        } catch (Exception e) {
            log.error("批量测试代理连接失败：proxyIds={}", proxyIds, e);
            return Result.error("批量测试代理连接失败：" + e.getMessage());
        }
    }

    // ==================== 统计接口 ====================

    /**
     * 获取代理统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result getStatistics() {
        try {
            Map<String, Object> statistics = proxyService.getProxyStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取代理统计信息失败", e);
            return Result.error("获取代理统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定用户的代理统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    @GetMapping("/statistics/user/{userId}")
    public Result getUserStatistics(@PathVariable Long userId) {
        try {
            Map<String, Object> statistics = proxyService.getUserProxyStatistics(userId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户代理统计信息失败：userId={}", userId, e);
            return Result.error("获取用户代理统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 验证接口 ====================

    /**
     * 检查代理地址是否唯一
     *
     * @param host 代理主机地址
     * @param port 代理端口
     * @param proxyId 排除的代理ID（用于更新验证）
     * @return 检查结果
     */
    @GetMapping("/check-unique")
    public Result checkUnique(@RequestParam String host,
                             @RequestParam String port,
                             @RequestParam(required = false) Long proxyId) {
        try {
            Long userId = getCurrentUserId();
            boolean isUnique = proxyService.checkProxyAddressUnique(host, port, userId, proxyId);
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查代理地址唯一性失败：host={}, port={}", host, port, e);
            return Result.error("检查代理地址唯一性失败：" + e.getMessage());
        }
    }

    /**
     * 验证代理配置
     *
     * @param proxy 代理配置对象
     * @return 验证结果
     */
    @PostMapping("/validate")
    public Result validateConfig(@RequestBody Proxy proxy) {
        try {
            Map<String, Object> result = proxyService.validateProxyConfig(proxy);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证代理配置失败", e);
            return Result.error("验证代理配置失败：" + e.getMessage());
        }
    }

    // ==================== 高级功能接口 ====================

    /**
     * 检查代理池健康状态
     *
     * @return 健康检查结果
     */
    @GetMapping("/health")
    public Result checkHealth() {
        try {
            Map<String, Object> result = proxyService.checkProxyPoolHealth();
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查代理池健康状态失败", e);
            return Result.error("检查代理池健康状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取代理使用建议
     *
     * @return 使用建议列表
     */
    @GetMapping("/recommendations")
    public Result getRecommendations() {
        try {
            List<String> recommendations = proxyService.getProxyUsageRecommendations();
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取代理使用建议失败", e);
            return Result.error("获取代理使用建议失败：" + e.getMessage());
        }
    }
}