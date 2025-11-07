package com.deepreach.web.controller;

import com.deepreach.web.entity.AiInstance;
import com.deepreach.web.entity.dto.InstanceTypeStatistics;
import com.deepreach.web.entity.dto.PlatformUsageStatistics;
import com.deepreach.web.entity.dto.CharacterUsageStatistics;
import com.deepreach.web.service.AiInstanceService;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.entity.DrPriceConfig;
import com.deepreach.web.service.UserDrBalanceService;
import com.deepreach.web.service.DrBillingRecordService;
import com.deepreach.web.service.DrPriceConfigService;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.exception.InsufficientMarketingInstanceException;
import com.deepreach.web.service.impl.UserDrBalanceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI实例Controller
 *
 * AI实例管理RESTful API控制器，负责：
 * 1. 实例基本信息管理API
 * 2. 实例类型和平台管理API
 * 3. 实例搜索和推荐API
 * 4. 实例统计和分析API
 * 5. 实例导入导出API
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/instance")
public class AiInstanceController {

    @Autowired
    private AiInstanceService instanceService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private UserDrBalanceService balanceService;

    @Autowired
    private DrBillingRecordService billingRecordService;

    @Autowired
    private DrPriceConfigService priceConfigService;

    @Autowired
    private UserDrBalanceService userDrBalanceService;

    // ==================== 查询接口 ====================

    /**
     * 获取实例列表
     *
     * 支持多条件查询和分页
     * 自动应用权限过滤
     *
     * @param instance 查询条件对象
     * @return 分页实例列表
     */
    @GetMapping("/list")
    public TableDataInfo list(AiInstance instance) {
        try {
            // 获取当前用户ID
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return TableDataInfo.error("用户未登录");
            }

            startPage(); // 启动分页
            List<com.deepreach.web.domain.vo.AiInstanceVO> list = instanceService.selectListVOWithUserPermission(instance, currentUserId);
            return getDataTable(list);
        } catch (Exception e) {
            log.error("查询实例列表失败", e);
            return TableDataInfo.error("查询实例列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据实例ID获取详细信息
     *
     * 获取实例的完整信息
     *
     * @param instanceId 实例ID
     * @return 实例详细信息
     */
    @GetMapping("/{instanceId}")
    public Result getInfo(@PathVariable Long instanceId) {
        try {
            // 检查访问权限
            Long currentUserId = getCurrentUserId();
            if (!instanceService.hasAccessPermission(instanceId, currentUserId)) {
                return Result.error("无权限访问该实例");
            }

            com.deepreach.web.domain.vo.AiInstanceVO instance = instanceService.selectCompleteInfoVO(instanceId, currentUserId);
            if (instance == null) {
                return Result.error("实例不存在");
            }

            return Result.success(instance);
        } catch (Exception e) {
            log.error("获取实例信息失败：实例ID={}", instanceId, e);
            return Result.error("获取实例信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的实例列表
     *
     * 查询当前用户创建的所有实例
     *
     * @return 我的实例列表
     */
    @GetMapping("/my")
    public Result getMyInstances() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            List<com.deepreach.web.domain.vo.AiInstanceVO> list = instanceService.selectByUserIdVO(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取我的实例列表失败", e);
            return Result.error("获取我的实例列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取可访问的实例列表
     *
     * 查询用户可以访问的所有实例
     *
     * @return 可访问的实例列表
     */
    @GetMapping("/accessible")
    public Result getAccessibleInstances() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            List<com.deepreach.web.domain.vo.AiInstanceVO> list = instanceService.selectAccessibleInstancesVO(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取可访问实例列表失败", e);
            return Result.error("获取可访问实例列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据类型查询实例
     *
     * @param instanceType 实例类型（0营销 1拓客）
     * @return 实例列表
     */
    @GetMapping("/type/{instanceType}")
    public Result getInstancesByType(@PathVariable String instanceType) {
        try {
            List<AiInstance> list = instanceService.selectByInstanceType(instanceType);
            return Result.success(list);
        } catch (Exception e) {
            log.error("按类型查询实例失败：类型={}", instanceType, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据平台查询实例
     *
     * @param platformId 平台ID
     * @return 实例列表
     */
    @GetMapping("/platform/{platformId}")
    public Result getInstancesByPlatform(@PathVariable Integer platformId) {
        try {
            List<AiInstance> list = instanceService.selectByPlatformId(platformId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("按平台查询实例失败：平台ID={}", platformId, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据人设查询实例
     *
     * @param characterId AI人设ID
     * @return 实例列表
     */
    @GetMapping("/character/{characterId}")
    public Result getInstancesByCharacter(@PathVariable Integer characterId) {
        try {
            List<AiInstance> list = instanceService.selectByCharacterId(characterId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("按人设查询实例失败：人设ID={}", characterId, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 搜索实例
     *
     * @param keyword 关键词
     * @param limit 限制数量（可选）
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result searchInstances(@RequestParam String keyword,
                                 @RequestParam(required = false) Integer limit) {
        try {
            Long currentUserId = getCurrentUserId();
            List<AiInstance> list = instanceService.searchInstances(keyword, currentUserId, limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("搜索实例失败：关键词={}", keyword, e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取最新实例
     *
     * @param limit 限制数量（可选）
     * @return 最新实例列表
     */
    @GetMapping("/latest")
    public Result getLatestInstances(@RequestParam(required = false) Integer limit) {
        try {
            List<AiInstance> list = instanceService.selectLatestInstances(limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取最新实例失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 查询未绑定人设的实例
     *
     * @return 未绑定人设的实例列表
     */
    @GetMapping("/unbound-character")
    public Result getUnboundCharacterInstances() {
        try {
            Long currentUserId = getCurrentUserId();
            List<AiInstance> list = instanceService.selectUnboundCharacterInstances(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询未绑定人设的实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 查询配置了代理的实例
     *
     * @return 配置了代理的实例列表
     */
    @GetMapping("/with-proxy")
    public Result getWithProxyInstances() {
        try {
            Long currentUserId = getCurrentUserId();
            List<AiInstance> list = instanceService.selectWithProxyInstances(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询配置了代理的实例失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 创建接口 ====================

    /**
     * 创建实例
     *
     * 用户创建实例接口 - 只有员工可以创建实例
     *
     * @param instance 实例对象
     * @return 创建结果
     */
    @PostMapping
    @Log(title = "实例管理", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody AiInstance instance) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 调用Service层处理创建逻辑
            AiInstance created = instanceService.createInstanceWithValidation(instance, currentUserId);
            return Result.success("创建实例成功", created);

        } catch (InsufficientMarketingInstanceException e) {
            // 让全局异常处理器处理，返回403状态码
            throw e;
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建实例失败：名称={}", instance.getInstanceName(), e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("创建实例失败：" + e.getMessage(), e);
        }
    }

    // 交有服务端完成
//    /**
//     * 处理营销实例创建
//     */
//    private Result handleMarketingInstanceCreation(AiInstance instance, Long currentUserId, Long parentUserId) throws Exception {
//        try {
//            // 1. 检查父账户余额是否大于100
//            UserDrBalance parentBalance = balanceService.getByUserId(parentUserId);
//            if (parentBalance == null) {
//                return Result.error("父账户余额信息不存在");
//            }
//
//            BigDecimal availableBalance = parentBalance.getDrBalance();
//            BigDecimal marketingInstancePrice = new BigDecimal("100.00"); // 营销实例预扣费
//
//            if (availableBalance.compareTo(marketingInstancePrice) < 0) {
//                return Result.error("余额不足，无法创建营销实例");
//            }
//
//            // 2. 将100元余额转为预扣费余额
//            if(userDrBalanceService.preDeductForInstance(parentUserId, marketingInstancePrice, currentUserId))
//                // 这里应该调用余额转移服务，暂时记录日志
//                log.info("为父账户 {} 转移 {} 元到预扣费余额", parentUserId, marketingInstancePrice);
//
//            // 3. 设置创建者信息并创建实例
//            instance.setCreateBy(SecurityUtils.getCurrentUsername());
//            instance.setUserId(currentUserId);
//
//            AiInstance created = instanceService.insert(instance);
//
//            // 4. 扣除当天费用
//            deductDailyFee(created, parentUserId, "0"); // "0"表示营销实例
//
//            return Result.success("创建营销实例成功", created);
//
//        } catch (Exception e) {
//            log.error("创建营销实例失败：名称={}", instance.getInstanceName(), e);
//            throw e;
//        }
//    }
//
//    /**
//     * 处理拓客实例创建
//     */
//    private Result handleProspectingInstanceCreation(AiInstance instance, Long currentUserId) throws Exception {
//        try {
//            // 1. 获取当前用户的实例统计
//            Integer platformId = instance.getPlatformId();
//            if (platformId == null) {
//                return Result.error("平台ID不能为空");
//            }
//
//            // 查询该用户名下该平台的营销实例数量
//            List<AiInstance> marketingInstances = instanceService.selectByUserIdAndTypeAndPlatform(
//                currentUserId, "0", null); // "0"表示营销实例
//            int marketingCount = marketingInstances.size();
//
//            // 查询该用户名下该平台的拓客实例数量
//            List<AiInstance> prospectingInstances = instanceService.selectByUserIdAndTypeAndPlatform(
//                currentUserId, "1", platformId); // "1"表示拓客实例
//            int prospectingCount = prospectingInstances.size();
//
//            // 2. 检查拓客实例数量是否超过限制（营销实例数量 * 10）
//            int maxProspectingCount = marketingCount * 10;
//            if (prospectingCount >= maxProspectingCount) {
//                return Result.error(String.format("%d个营销实例仅能创建%d个同平台拓客实例！",
//                    marketingCount, maxProspectingCount));
//            }
//
//            // 3. 设置创建者信息并创建实例
//            instance.setCreateBy(SecurityUtils.getCurrentUsername());
//            instance.setUserId(currentUserId);
//
//            AiInstance created = instanceService.insert(instance);
//
//            // 4. 扣除当天费用
//            Long parentUserId = userService.selectUserWithDept(currentUserId).getParentUserId();
//            deductDailyFee(created, parentUserId, "1"); // "1"表示拓客实例
//
//            return Result.success("创建拓客实例成功", created);
//
//        } catch (Exception e) {
//            log.error("创建拓客实例失败：名称={}", instance.getInstanceName(), e);
//            throw e;
//        }
//    }

    /**
     * 扣除当天实例费用
     *
     * @param instance 创建的实例
     * @param parentUserId 父用户ID
     * @param instanceType 实例类型（"0"营销，"1"拓客）
     */
    private void deductDailyFee(AiInstance instance, Long parentUserId, String instanceType) {
        try {
            // 获取当前小时
            int currentHour = java.time.LocalDateTime.now().getHour();

            // 计算剩余时间比例
            double remainingHours = 24 - currentHour;
            double feeRatio = remainingHours / 24.0;

            // 获取实例每天的价格
            DrPriceConfig priceConfig;
            if ("0".equals(instanceType)) {
                // 营销实例价格
                priceConfig = priceConfigService.selectDrPriceConfigByBusinessType(
                    DrPriceConfig.BUSINESS_TYPE_INSTANCE_MARKETING);
            } else {
                // 拓客实例价格
                priceConfig = priceConfigService.selectDrPriceConfigByBusinessType(
                    DrPriceConfig.BUSINESS_TYPE_INSTANCE_PROSPECTING);
            }

            if (priceConfig == null || !priceConfig.isActive()) {
                log.warn("价格配置未启用，跳过扣费：实例类型={}", instanceType);
                return;
            }

            // 计算当天费用
            BigDecimal dailyPrice = priceConfig.getDrPrice();
            BigDecimal todayFee = dailyPrice.multiply(BigDecimal.valueOf(feeRatio))
                                         .setScale(2, BigDecimal.ROUND_HALF_UP);

            log.info("实例 {} 当天扣费：{} DR，剩余时间比例：{}",
                instance.getInstanceName(), todayFee, feeRatio);

            // 创建扣费记录
            DrBillingRecord billingRecord = new DrBillingRecord();
            billingRecord.setUserId(parentUserId); // 实际扣费的是商家总账号
            billingRecord.setOperatorId(parentUserId); // 自动扣费
            billingRecord.setBillType(2); // 消费类型
            billingRecord.setBusinessType(priceConfig.getBusinessType());
            billingRecord.setDrAmount(todayFee);
            billingRecord.setDescription(String.format("实例创建当天费用 - %s（剩余%d小时）",
                instance.getInstanceName(), (int)remainingHours));
            billingRecord.setStatus(1); // 成功状态
            billingRecord.setCreateBy("system");

            // 调用扣费服务
            String result = balanceService.deduct(billingRecord);

            if (!"扣费成功".equals(result)) {
                log.error("实例创建当天扣费失败：{}, 错误：{}", instance.getInstanceName(), result);
                // 这里可以抛出异常或者记录错误，根据业务需求决定
            } else {
                log.info("实例创建当天扣费成功：{}, 金额：{} DR", instance.getInstanceName(), todayFee);
            }

        } catch (Exception e) {
            log.error("扣除实例当天费用失败：实例={}", instance.getInstanceName(), e);
            // 扣费失败不影响实例创建，记录错误日志即可
        }
    }

    /**
     * 创建营销类型实例
     *
     * @param instance 实例对象
     * @return 创建结果
     */
    @PostMapping("/marketing")
    @Log(title = "创建营销实例", businessType = BusinessType.INSERT)
    public Result createMarketingInstance(@Validated @RequestBody AiInstance instance) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            instance.setCreateBy(SecurityUtils.getCurrentUsername());
            AiInstance created = instanceService.createMarketingInstance(instance, currentUserId);
            return Result.success("创建营销实例成功", created);
        } catch (Exception e) {
            log.error("创建营销实例失败：名称={}", instance.getInstanceName(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建拓客类型实例
     *
     * @param instance 实例对象
     * @return 创建结果
     */
    @PostMapping("/prospecting")
    @Log(title = "创建拓客实例", businessType = BusinessType.INSERT)
    public Result createProspectingInstance(@Validated @RequestBody AiInstance instance) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            instance.setCreateBy(SecurityUtils.getCurrentUsername());
            AiInstance created = instanceService.createProspectingInstance(instance, currentUserId);
            return Result.success("创建拓客实例成功", created);
        } catch (InsufficientMarketingInstanceException e) {
            // 让全局异常处理器处理，返回403状态码
            throw e;
        } catch (Exception e) {
            log.error("创建拓客实例失败：名称={}", instance.getInstanceName(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 复制实例
     *
     * 基于现有实例创建副本
     *
     * @param sourceId 源实例ID
     * @param newInstanceName 新实例名称
     * @return 复制结果
     */
    @PostMapping("/copy/{sourceId}")
    @Log(title = "复制实例", businessType = BusinessType.INSERT)
    public Result copyInstance(@PathVariable Long sourceId, @RequestParam String newInstanceName) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            AiInstance copied = instanceService.copyInstance(sourceId, newInstanceName, currentUserId);
            return Result.success("复制实例成功", copied);
        } catch (Exception e) {
            log.error("复制实例失败：源实例ID={}, 新名称={}", sourceId, newInstanceName, e);
            return Result.error("复制实例失败：" + e.getMessage());
        }
    }

    // ==================== 更新接口 ====================

    /**
     * 更新实例信息
     *
     * @param instance 实例对象
     * @return 更新结果
     */
    @PutMapping
    @Log(title = "实例管理", businessType = BusinessType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    public Result edit(@Validated @RequestBody AiInstance instance) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instance.getInstanceId(), getCurrentUserId())) {
                return Result.error("无权限修改该实例");
            }

            // 设置更新者信息
            instance.setUpdateBy(SecurityUtils.getCurrentUsername());

            boolean success = instanceService.update(instance);
            if (success) {
                return Result.success("更新实例信息成功");
            } else {
                return Result.error("更新实例信息失败");
            }
        } catch (Exception e) {
            log.error("更新实例信息失败：实例ID={}", instance.getInstanceId(), e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("更新实例信息失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新实例人设绑定
     *
     * @param instanceId 实例ID
     * @param characterId AI人设ID
     * @return 更新结果
     */
    @PutMapping("/{instanceId}/character")
    @Log(title = "绑定人设", businessType = BusinessType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    public Result updateCharacterId(@PathVariable Long instanceId, @RequestBody java.util.Map<String, Integer> request) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instanceId, getCurrentUserId())) {
                return Result.error("无权限修改该实例");
            }

            Integer characterId = request.get("characterId");
            boolean success = instanceService.updateCharacterId(instanceId, characterId);
            if (success) {
                return Result.success("绑定人设成功");
            } else {
                return Result.error("绑定人设失败");
            }
        } catch (Exception e) {
            log.error("更新实例人设绑定失败：实例ID={}", instanceId, e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("绑定人设失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新实例代理ID
     *
     * @param instanceId 实例ID
     * @param proxyId 代理ID
     * @return 更新结果
     */
    @PutMapping("/{instanceId}/proxy")
    @Log(title = "更新代理", businessType = BusinessType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    public Result updateProxyId(@PathVariable Long instanceId, @RequestBody java.util.Map<String, Integer> request) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instanceId, getCurrentUserId())) {
                return Result.error("无权限修改该实例");
            }

            Integer proxyId = request.get("proxyId");
            boolean success = instanceService.updateProxyId(instanceId, proxyId);
            if (success) {
                return Result.success("更新代理ID成功");
            } else {
                return Result.error("更新代理地址失败");
            }
        } catch (Exception e) {
            log.error("更新实例代理地址失败：实例ID={}", instanceId, e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("更新代理地址失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新实例平台绑定
     *
     * @param instanceId 实例ID
     * @return 更新结果
     */
    @PutMapping("/{instanceId}/platform")
    @Log(title = "绑定平台", businessType = BusinessType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    public Result updatePlatformId(@PathVariable Long instanceId, @RequestBody java.util.Map<String, Integer> request) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instanceId, getCurrentUserId())) {
                return Result.error("无权限修改该实例");
            }

            Integer platformId = request.get("platformId");
            boolean success = instanceService.updatePlatformId(instanceId, platformId);
            if (success) {
                return Result.success("绑定平台成功");
            } else {
                return Result.error("绑定平台失败");
            }
        } catch (Exception e) {
            log.error("更新实例平台绑定失败：实例ID={}", instanceId, e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("绑定平台失败：" + e.getMessage(), e);
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除实例
     *
     * @param instanceId 实例ID
     * @return 删除结果
     */
    @DeleteMapping("/{instanceId}")
    @Log(title = "实例管理", businessType = BusinessType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    public Result remove(@PathVariable("instanceId") Long instanceId) {
        try {
            // 检查权限
            if (!instanceService.canDelete(instanceId, getCurrentUserId())) {
                return Result.error("无权限删除该实例");
            }

            boolean success = instanceService.deleteById(instanceId);
            if (success) {
                return Result.success("删除实例成功");
            } else {
                return Result.error("删除实例失败");
            }
        } catch (Exception e) {
            log.error("删除实例失败：实例ID={}", instanceId, e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("删除实例失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量删除实例
     *
     * @param instanceIds 实例ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    @Log(title = "实例管理", businessType = BusinessType.DELETE)
    @Transactional(rollbackFor = Exception.class)
    public Result removeBatch(@RequestBody List<Long> instanceIds) {
        try {
            // 过滤有权限删除的实例
            List<Long> validInstanceIds = new ArrayList<>();
            Long currentUserId = getCurrentUserId();

            for (Long instanceId : instanceIds) {
                if (instanceService.canDelete(instanceId, currentUserId)) {
                    validInstanceIds.add(instanceId);
                }
            }

            if (validInstanceIds.isEmpty()) {
                return Result.error("没有可删除的实例");
            }

            boolean success = instanceService.deleteByIds(validInstanceIds);
            if (success) {
                return Result.success("批量删除实例成功，删除数量：" + validInstanceIds.size());
            } else {
                return Result.error("批量删除实例失败");
            }
        } catch (Exception e) {
            log.error("批量删除实例失败：实例IDs={}", instanceIds, e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("批量删除实例失败：" + e.getMessage(), e);
        }
    }

    // ==================== 统计接口 ====================

//    /**
//     * 获取实例统计信息
//     *
//     * @return 统计信息
//     */
//    @GetMapping("/statistics")
//    public Result getStatistics() {
//        try {
//            Map<String, Object> statistics = instanceService.getStatistics();
//            return Result.success(statistics);
//        } catch (Exception e) {
//            log.error("获取实例统计信息失败", e);
//            return Result.error("获取统计信息失败：" + e.getMessage());
//        }
//    }

//    /**
//     * 获取我的实例统计信息
//     *
//     * @return 统计信息
//     */
//    @GetMapping("/my/statistics")
//    public Result getMyStatistics() {
//        try {
//            Long currentUserId = getCurrentUserId();
//            if (currentUserId == null) {
//                return Result.error("用户未登录");
//            }
//
//            Map<String, Object> statistics = instanceService.getUserStatistics(currentUserId);
//            return Result.success(statistics);
//        } catch (Exception e) {
//            log.error("获取用户实例统计信息失败", e);
//            return Result.error("获取统计信息失败：" + e.getMessage());
//        }
//    }

    /**
     * 获取实例类型统计
     *
     * @return 类型统计信息列表
     */
    @GetMapping("/statistics/type")
    public Result getTypeStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            List<InstanceTypeStatistics> statistics = instanceService.getTypeStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取实例类型统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取平台使用统计
     *
     * @return 平台统计信息列表
     */
    @GetMapping("/statistics/platform")
    public Result getPlatformUsageStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            List<PlatformUsageStatistics> statistics = instanceService.getPlatformUsageStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取平台使用统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取人设使用统计
     *
     * @return 人设统计信息列表
     */
    @GetMapping("/statistics/character")
    public Result getCharacterUsageStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            List<CharacterUsageStatistics> statistics = instanceService.getCharacterUsageStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取人设使用统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

//    /**
//     * 获取实例活动统计
//     *
//     * @param days 统计天数（可选）
//     * @return 活动统计信息
//     */
//    @GetMapping("/statistics/activity")
//    public Result getActivityStatistics(@RequestParam(required = false) Integer days) {
//        try {
//            Long currentUserId = getCurrentUserId();
//            Map<String, Object> statistics = instanceService.getActivityStatistics(days, currentUserId);
//            return Result.success(statistics);
//        } catch (Exception e) {
//            log.error("获取实例活动统计失败", e);
//            return Result.error("获取统计信息失败：" + e.getMessage());
//        }
//    }

    /**
     * 获取实例配置分析
     *
     * @return 配置分析结果
     */
    @GetMapping("/analysis/configuration")
    public Result getConfigurationAnalysis() {
        try {
            Long currentUserId = getCurrentUserId();
            Map<String, Object> analysis = instanceService.getConfigurationAnalysis(currentUserId);
            return Result.success(analysis);
        } catch (Exception e) {
            log.error("获取实例配置分析失败", e);
            return Result.error("获取分析失败：" + e.getMessage());
        }
    }

    /**
     * 获取实例运行状态
     *
     * @param instanceId 实例ID
     * @return 运行状态信息
     */
    @GetMapping("/{instanceId}/status")
    public Result getInstanceStatus(@PathVariable Long instanceId) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instanceId, getCurrentUserId())) {
                return Result.error("无权限访问该实例");
            }

            Map<String, Object> status = instanceService.getInstanceStatus(instanceId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取实例运行状态失败：实例ID={}", instanceId, e);
            return Result.error("获取状态失败：" + e.getMessage());
        }
    }

    // ==================== 验证接口 ====================

    /**
     * 检查实例名称是否唯一
     *
     * @param instanceName 实例名称
     * @param instanceId 排除的实例ID（用于更新验证）
     * @return 验证结果
     */
    @GetMapping("/check-name-unique")
    public Result checkInstanceNameUnique(@RequestParam String instanceName,
                                         @RequestParam(required = false) Long instanceId) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            boolean isUnique = instanceService.checkInstanceNameUnique(instanceName, currentUserId, instanceId);
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查实例名称唯一性失败：名称={}", instanceName, e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 检查实例是否可以运行
     *
     * @param instanceId 实例ID
     * @return 验证结果
     */
    @GetMapping("/{instanceId}/can-run")
    public Result canRun(@PathVariable Long instanceId) {
        try {
            // 检查权限
            if (!instanceService.hasAccessPermission(instanceId, getCurrentUserId())) {
                return Result.error("无权限访问该实例");
            }

            boolean canRun = instanceService.canRun(instanceId);
            return Result.success(canRun);
        } catch (Exception e) {
            log.error("检查实例运行条件失败：实例ID={}", instanceId, e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    // ==================== 批量操作接口 ====================

    /**
     * 批量更新实例配置
     *
     * @param instanceIds 实例ID列表
     * @param platformId 平台ID（可选）
     * @param characterId 人设ID（可选）
     * @return 更新结果
     */
    @PutMapping("/batch/config")
    @Log(title = "批量更新配置", businessType = BusinessType.UPDATE)
    @Transactional(rollbackFor = Exception.class)
    public Result batchUpdateConfig(@RequestBody List<Long> instanceIds,
                                   @RequestParam(required = false) Integer platformId,
                                   @RequestParam(required = false) Integer characterId) {
        try {
            // 验证用户权限
            Long currentUserId = getCurrentUserId();
            List<Long> validInstanceIds = new ArrayList<>();

            for (Long instanceId : instanceIds) {
                if (instanceService.hasAccessPermission(instanceId, currentUserId)) {
                    validInstanceIds.add(instanceId);
                }
            }

            if (validInstanceIds.isEmpty()) {
                return Result.error("没有可配置的实例");
            }

            boolean success = instanceService.batchUpdateInstanceConfig(validInstanceIds, platformId, characterId);
            if (success) {
                return Result.success("批量更新配置成功，更新数量：" + validInstanceIds.size());
            } else {
                return Result.error("批量更新配置失败");
            }
        } catch (Exception e) {
            log.error("批量更新实例配置失败", e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("批量更新失败：" + e.getMessage(), e);
        }
    }

    // ==================== 导入导出接口 ====================

    /**
     * 导入实例数据
     *
     * @param instances 实例列表
     * @param updateSupport 是否支持更新已存在的实例
     * @return 导入结果
     */
    @PostMapping("/import")
    @Log(title = "实例导入", businessType = BusinessType.IMPORT)
    @Transactional(rollbackFor = Exception.class)
    public Result importInstances(@RequestBody List<AiInstance> instances,
                                @RequestParam(defaultValue = "false") boolean updateSupport) {
        try {
            Map<String, Object> result = instanceService.importInstances(instances, updateSupport);
            return Result.success("导入实例成功", result);
        } catch (Exception e) {
            log.error("导入实例失败", e);
            // 事务会自动回滚，抛出运行时异常
            throw new RuntimeException("导入实例失败：" + e.getMessage(), e);
        }
    }

    /**
     * 导出实例数据
     *
     * @param instance 查询条件对象
     * @return 导出数据
     */
    @PostMapping("/export")
    @Log(title = "实例导出", businessType = BusinessType.EXPORT)
    public Result exportInstances(AiInstance instance) {
        try {
            List<AiInstance> list = instanceService.selectList(instance);
            String data = instanceService.exportInstances(list);

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("filename", "ai_instances_" + System.currentTimeMillis() + ".json");

            return Result.success("导出实例成功", result);
        } catch (Exception e) {
            log.error("导出实例失败", e);
            return Result.error("导出实例失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }

    // ==================== 分页辅助方法 ====================
    // 注意：这些方法需要在基类中实现

    /**
     * 启动分页
     */
    protected void startPage() {
        // 需要实现分页逻辑
    }

    /**
     * 构建分页数据表格
     */
    protected TableDataInfo getDataTable(List<?> list) {
        // 需要实现分页数据构建逻辑
        return new TableDataInfo(list, list.size());
    }
}
