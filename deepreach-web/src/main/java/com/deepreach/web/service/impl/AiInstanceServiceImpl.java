package com.deepreach.web.service.impl;

import com.deepreach.web.entity.AiInstance;
import com.deepreach.web.entity.Proxy;
import com.deepreach.web.entity.dto.InstanceTypeStatistics;
import com.deepreach.web.entity.dto.PlatformUsageStatistics;
import com.deepreach.web.entity.dto.CharacterUsageStatistics;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.exception.InsufficientMarketingInstanceException;
import com.deepreach.web.entity.UserDrBalance;
import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.entity.DrPriceConfig;
import com.deepreach.web.mapper.AiInstanceMapper;
import com.deepreach.web.mapper.ProxyMapper;
import com.deepreach.web.service.AiInstanceService;
import com.deepreach.web.service.UserDrBalanceService;
import com.deepreach.web.service.DrBillingRecordService;
import com.deepreach.web.service.DrPriceConfigService;
import com.deepreach.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI实例Service实现类
 *
 * 实现AI实例相关的业务逻辑，包括：
 * 1. 实例基本信息管理
 * 2. 实例权限控制
 * 3. 实例搜索和推荐
 * 4. 实例统计和分析
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@Service
public class AiInstanceServiceImpl implements AiInstanceService {

    @Autowired
    private AiInstanceMapper instanceMapper;

    @Autowired
    private ProxyMapper proxyMapper;

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

    @Override
    public AiInstance selectById(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        try {
            return instanceMapper.selectById(instanceId);
        } catch (Exception e) {
            log.error("查询实例失败：实例ID={}", instanceId, e);
            return null;
        }
    }

    @Override
    public AiInstance selectCompleteInfo(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        try {
            return instanceMapper.selectCompleteInfo(instanceId);
        } catch (Exception e) {
            log.error("查询实例完整信息失败：实例ID={}", instanceId, e);
            return null;
        }
    }

    @Override
    public List<AiInstance> selectByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectByUserId(userId);
        } catch (Exception e) {
            log.error("查询用户实例列表失败：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AiInstance> selectList(AiInstance instance) {
        if (instance == null) {
            instance = new AiInstance();
        }
        try {
            return instanceMapper.selectList(instance);
        } catch (Exception e) {
            log.error("查询实例列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AiInstance> selectListWithUserPermission(AiInstance instance, Long currentUserId) {
        if (instance == null) {
            instance = new AiInstance();
        }
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectListWithUserPermission(instance, currentUserId);
        } catch (Exception e) {
            log.error("查询实例列表失败（带权限控制）", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<com.deepreach.web.domain.vo.AiInstanceVO> selectListVOWithUserPermission(AiInstance instance, Long currentUserId) {
        if (instance == null) {
            instance = new AiInstance();
        }
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectListVOWithUserPermission(instance, currentUserId);
        } catch (Exception e) {
            log.error("查询实例列表失败（带权限控制和VO）", e);
            return new ArrayList<>();
        }
    }

    @Override
    public com.deepreach.web.domain.vo.AiInstanceVO selectCompleteInfoVO(Long instanceId, Long currentUserId) {
        if (instanceId == null) {
            return null;
        }
        if (currentUserId == null) {
            return null;
        }
        try {
            // 先检查权限
            if (!hasAccessPermission(instanceId, currentUserId)) {
                log.warn("用户{}无权限访问实例{}", currentUserId, instanceId);
                return null;
            }
            // 暂时返回基础信息，后续可以在Mapper中添加完整信息的VO查询
            AiInstance instance = instanceMapper.selectById(instanceId);
            if (instance == null) {
                return null;
            }
            // 转换为VO对象 - 确保所有字段都正确设置
            com.deepreach.web.domain.vo.AiInstanceVO vo = new com.deepreach.web.domain.vo.AiInstanceVO();
            vo.setInstanceId(instance.getInstanceId());
            vo.setInstanceName(instance.getInstanceName());
            vo.setInstanceType(instance.getInstanceType());
            vo.setPlatformId(instance.getPlatformId());
            vo.setCharacterId(instance.getCharacterId());
            vo.setProxyId(instance.getProxyId());
            vo.setUserId(instance.getUserId());
            vo.setCreateTime(instance.getCreateTime());
            vo.setUpdateTime(instance.getUpdateTime());
            vo.setCreateBy(instance.getCreateBy());
            vo.setUpdateBy(instance.getUpdateBy());

            // 设置平台名称和人设名称
            vo.setPlatformName("平台信息");
            vo.setCharacterName("人设信息");

            // 设置所有必要字段，确保没有null值
            vo.setValid(true);
            vo.setSummary("实例：" + instance.getInstanceName() + " | 类型：" +
                         ("0".equals(instance.getInstanceType()) ? "营销" : "拓客") +
                         " | 平台ID：" + instance.getPlatformId() +
                         " | 人设ID：" + instance.getCharacterId() +
                         (instance.getProxyId() != null ? " | 代理ID：" + instance.getProxyId() : ""));
            vo.setStatusDisplay(vo.getCharacterName() + " | " +
                         (instance.getProxyId() != null ? "使用代理" : "未使用代理"));
            vo.setInstanceNameValid(true);
            vo.setInstanceTypeValid(true);
            vo.setProxyIdValid(instance.getProxyId() != null);
            vo.setFullyConfigured(instance.getProxyId() != null && instance.getCharacterId() != null);
            vo.setMarketingType("0".equals(instance.getInstanceType()));
            vo.setProspectingType("1".equals(instance.getInstanceType()));
            vo.setConfigurationCompleteness(vo.getFullyConfigured() ? 100 : 80);
            vo.setParamSize(0);

            // 设置实例类型显示
            vo.setInstanceTypeDisplay("0".equals(instance.getInstanceType()) ? "营销" : "拓客");

            // 设置创建者和更新者信息
            vo.setCreateBy("admin");
            vo.setUpdateBy("admin");

            // 设置所有业务计算字段，确保没有null值
            vo.setValid(true);
            vo.setInstanceNameValid(true);
            vo.setInstanceTypeValid(true);
            vo.setProxyIdValid(instance.getProxyId() != null);
            vo.setFullyConfigured(instance.getProxyId() != null && instance.getCharacterId() != null);
            vo.setMarketingType("0".equals(instance.getInstanceType()));
            vo.setProspectingType("1".equals(instance.getInstanceType()));
            vo.setConfigurationCompleteness(vo.getFullyConfigured() ? 100 : 80);

            // 设置摘要和状态显示
            vo.setSummary("实例：" + instance.getInstanceName() + " | 类型：" +
                         ("0".equals(instance.getInstanceType()) ? "营销" : "拓客") +
                         " | 平台ID：" + instance.getPlatformId() +
                         " | 人设ID：" + instance.getCharacterId() +
                         (instance.getProxyId() != null ? " | 代理ID：" + instance.getProxyId() : ""));
            vo.setStatusDisplay(vo.getCharacterName() + " | " +
                         (instance.getProxyId() != null ? "使用代理" : "未使用代理"));

            // 获取代理信息并设置proxyAddress
            if (instance.getProxyId() != null) {
                try {
                    Proxy proxy = proxyMapper.selectProxyById(Long.valueOf(instance.getProxyId()));
                    if (proxy != null) {
                        // 构建包含代理类型的完整地址字符串
                        String proxyTypeStr = "";
                        if (proxy.getProxyType() != null) {
                            switch (proxy.getProxyType()) {
                                case 0:
                                    proxyTypeStr = "HTTP";
                                    break;
                                case 1:
                                    proxyTypeStr = "SOCKS5";
                                    break;
                                default:
                                    proxyTypeStr = "未知类型";
                                    break;
                            }
                        }

                        // 构建完整地址：host:port (type)
                        StringBuilder addressBuilder = new StringBuilder();
                        if (proxy.getProxyHost() != null && proxy.getProxyPort() != null) {
                            addressBuilder.append(proxy.getProxyHost())
                                         .append(":")
                                         .append(proxy.getProxyPort());
                            if (!proxyTypeStr.isEmpty()) {
                                addressBuilder.append(" (").append(proxyTypeStr).append(")");
                            }
                            vo.setProxyAddress(addressBuilder.toString());
                        } else {
                            vo.setProxyAddress("");
                        }
                    } else {
                        vo.setProxyAddress("");
                    }
                } catch (Exception e) {
                    log.warn("获取代理信息失败，代理ID={}", instance.getProxyId(), e);
                    vo.setProxyAddress("");
                }
            } else {
                vo.setProxyAddress("");
            }

            return vo;
        } catch (Exception e) {
            log.error("查询实例完整信息失败（VO）：实例ID={}", instanceId, e);
            return null;
        }
    }

    @Override
    public List<com.deepreach.web.domain.vo.AiInstanceVO> selectByUserIdVO(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            // 暂时使用基础查询，然后转换为VO
            List<AiInstance> instances = instanceMapper.selectByUserId(userId);
            List<com.deepreach.web.domain.vo.AiInstanceVO> voList = new ArrayList<>();
            for (AiInstance instance : instances) {
                com.deepreach.web.domain.vo.AiInstanceVO vo = new com.deepreach.web.domain.vo.AiInstanceVO();
                vo.setInstanceId(instance.getInstanceId());
                vo.setInstanceName(instance.getInstanceName());
                vo.setInstanceType(instance.getInstanceType());
                vo.setPlatformId(instance.getPlatformId());
                vo.setCharacterId(instance.getCharacterId());
                vo.setProxyId(instance.getProxyId());
                vo.setUserId(instance.getUserId());
                vo.setCreateTime(instance.getCreateTime());
                vo.setUpdateTime(instance.getUpdateTime());
                // PlatformName和CharacterName将在Mapper的VO查询中设置
                vo.setPlatformName("平台信息");
                vo.setCharacterName("人设信息");
                voList.add(vo);
            }
            return voList;
        } catch (Exception e) {
            log.error("查询用户实例列表失败（VO）：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInstance insert(AiInstance instance) throws Exception {
        // 参数验证
        if (instance == null) {
            throw new Exception("实例信息不能为空");
        }

        if (!instance.isInstanceNameValid()) {
            throw new Exception("实例名称无效，长度应在1-20个字符之间");
        }

        if (!instance.isInstanceTypeValid()) {
            throw new Exception("实例类型无效，只支持0（营销）或1（拓客）");
        }

        if (instance.getUserId() == null) {
            throw new Exception("用户ID不能为空");
        }

        if (instance.getPlatformId() == null) {
            throw new Exception("平台ID不能为空");
        }

        // 检查名称唯一性
        if (!checkInstanceNameUnique(instance.getInstanceName(), instance.getUserId(), null)) {
            throw new Exception("实例名称已存在");
        }

        // 检查代理地址格式
        if (!instance.isProxyIdValid()) {
            throw new Exception("代理地址格式无效，应为IP:PORT格式");
        }

        // 设置创建时间
        LocalDateTime now = LocalDateTime.now();
        instance.setCreateTime(now);
        instance.setUpdateTime(now);

        try {
            int result = instanceMapper.insert(instance);
            if (result <= 0) {
                throw new Exception("创建实例失败");
            }
            log.info("创建实例成功：实例ID={}, 名称={}", instance.getInstanceId(), instance.getInstanceName());
            return instance;
        } catch (Exception e) {
            log.error("创建实例失败：名称={}", instance.getInstanceName(), e);
            throw new Exception(e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(AiInstance instance) throws Exception {
        if (instance == null || instance.getInstanceId() == null) {
            throw new Exception("实例ID不能为空");
        }

        // 检查实例是否存在
        AiInstance existing = selectById(instance.getInstanceId());
        if (existing == null) {
            throw new Exception("实例不存在");
        }

        // 参数验证
        if (instance.getInstanceName() != null && !instance.isInstanceNameValid()) {
            throw new Exception("实例名称无效，长度应在1-20个字符之间");
        }

        if (instance.getInstanceType() != null && !instance.isInstanceTypeValid()) {
            throw new Exception("实例类型无效，只支持0（营销）或1（拓客）");
        }

        if (instance.getProxyId() != null && !instance.isProxyIdValid()) {
            throw new Exception("代理地址格式无效，应为IP:PORT格式");
        }

        // 检查名称唯一性
        if (instance.getInstanceName() != null && !instance.getInstanceName().equals(existing.getInstanceName())) {
            if (!checkInstanceNameUnique(instance.getInstanceName(), existing.getUserId(), instance.getInstanceId())) {
                throw new Exception("实例名称已存在");
            }
        }

        // 设置更新时间
        instance.setUpdateTime(LocalDateTime.now());

        try {
            int result = instanceMapper.update(instance);
            if (result <= 0) {
                throw new Exception("更新实例失败");
            }
            log.info("更新实例成功：实例ID={}", instance.getInstanceId());
            return true;
        } catch (Exception e) {
            log.error("更新实例失败：实例ID={}", instance.getInstanceId(), e);
            throw new Exception("更新实例失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long instanceId) throws Exception {
        if (instanceId == null) {
            throw new Exception("实例ID不能为空");
        }

        // 检查实例是否存在
        AiInstance existing = selectById(instanceId);
        if (existing == null) {
            throw new Exception("实例不存在");
        }

        try {
            int result = instanceMapper.deleteById(instanceId);
            if (result <= 0) {
                throw new Exception("删除实例失败");
            }
            log.info("删除实例成功：实例ID={}, 名称={}", instanceId, existing.getInstanceName());
            return true;
        } catch (Exception e) {
            log.error("删除实例失败：实例ID={}", instanceId, e);
            throw new Exception("删除实例失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> instanceIds) throws Exception {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return true;
        }

        try {
            int result = instanceMapper.deleteByIds(instanceIds);
            if (result <= 0) {
                throw new Exception("批量删除实例失败");
            }
            log.info("批量删除实例成功：删除数量={}", result);
            return true;
        } catch (Exception e) {
            log.error("批量删除实例失败：实例IDs={}", instanceIds, e);
            throw new Exception("批量删除实例失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectByInstanceType(String instanceType) throws Exception {
        if (instanceType == null || instanceType.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectByInstanceType(instanceType);
        } catch (Exception e) {
            log.error("按类型查询实例失败：类型={}", instanceType, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectByPlatformId(Integer platformId) throws Exception {
        if (platformId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectByPlatformId(platformId);
        } catch (Exception e) {
            log.error("按平台查询实例失败：平台ID={}", platformId, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectByCharacterId(Integer characterId) throws Exception {
        if (characterId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectByCharacterId(characterId);
        } catch (Exception e) {
            log.error("按人设查询实例失败：人设ID={}", characterId, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectAccessibleInstances(Long userId) throws Exception {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            return instanceMapper.selectAccessibleInstances(userId);
        } catch (Exception e) {
            log.error("查询可访问实例失败：用户ID={}", userId, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<com.deepreach.web.domain.vo.AiInstanceVO> selectAccessibleInstancesVO(Long userId) throws Exception {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            // 暂时使用基础查询，然后转换为VO
            List<AiInstance> instances = instanceMapper.selectAccessibleInstances(userId);
            List<com.deepreach.web.domain.vo.AiInstanceVO> voList = new ArrayList<>();
            for (AiInstance instance : instances) {
                com.deepreach.web.domain.vo.AiInstanceVO vo = new com.deepreach.web.domain.vo.AiInstanceVO();
                vo.setInstanceId(instance.getInstanceId());
                vo.setInstanceName(instance.getInstanceName());
                vo.setInstanceType(instance.getInstanceType());
                vo.setPlatformId(instance.getPlatformId());
                vo.setCharacterId(instance.getCharacterId());
                vo.setProxyId(instance.getProxyId());
                vo.setUserId(instance.getUserId());
                vo.setCreateTime(instance.getCreateTime());
                vo.setUpdateTime(instance.getUpdateTime());
                // PlatformName和CharacterName将在Mapper的VO查询中设置
                vo.setPlatformName("平台信息");
                vo.setCharacterName("人设信息");
                voList.add(vo);
            }
            return voList;
        } catch (Exception e) {
            log.error("查询可访问实例失败（VO）：用户ID={}", userId, e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public boolean checkInstanceNameUnique(String instanceName, Long userId, Long instanceId) {
        if (instanceName == null || instanceName.trim().isEmpty() || userId == null) {
            return false;
        }
        try {
            int count = instanceMapper.checkInstanceNameUnique(instanceName, userId, instanceId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查实例名称唯一性失败：名称={}, 用户ID={}", instanceName, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCharacterId(Long instanceId, Integer characterId) throws Exception {
        if (instanceId == null) {
            throw new Exception("实例ID不能为空");
        }

        // 检查实例是否存在
        AiInstance existing = selectById(instanceId);
        if (existing == null) {
            throw new Exception("实例不存在");
        }

        try {
            int result = instanceMapper.updateCharacterId(instanceId, characterId);
            if (result <= 0) {
                throw new Exception("更新人设绑定失败");
            }
            log.info("更新实例人设绑定成功：实例ID={}, 人设ID={}", instanceId, characterId);
            return true;
        } catch (Exception e) {
            log.error("更新实例人设绑定失败：实例ID={}", instanceId, e);
            throw new Exception("更新人设绑定失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProxyId(Long instanceId, Integer proxyId) throws Exception {
        if (instanceId == null) {
            throw new Exception("实例ID不能为空");
        }

        // 验证代理ID格式
        if (proxyId != null && proxyId <= 0) {
            throw new Exception("代理ID必须为正整数");
        }

        // 检查实例是否存在
        AiInstance existing = selectById(instanceId);
        if (existing == null) {
            throw new Exception("实例不存在");
        }

        try {
            int result = instanceMapper.updateProxyId(instanceId, proxyId);
            if (result <= 0) {
                throw new Exception("更新代理ID失败");
            }
            log.info("更新实例代理ID成功：实例ID={}, 代理ID={}", instanceId, proxyId);
            return true;
        } catch (Exception e) {
            log.error("更新实例代理地址失败：实例ID={}", instanceId, e);
            throw new Exception("更新代理地址失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePlatformId(Long instanceId, Integer platformId) throws Exception {
        if (instanceId == null || platformId == null) {
            throw new Exception("实例ID和平台ID不能为空");
        }

        // 检查实例是否存在
        AiInstance existing = selectById(instanceId);
        if (existing == null) {
            throw new Exception("实例不存在");
        }

        try {
            int result = instanceMapper.updatePlatformId(instanceId, platformId);
            if (result <= 0) {
                throw new Exception("更新平台绑定失败");
            }
            log.info("更新实例平台绑定成功：实例ID={}, 平台ID={}", instanceId, platformId);
            return true;
        } catch (Exception e) {
            log.error("更新实例平台绑定失败：实例ID={}", instanceId, e);
            throw new Exception("更新平台绑定失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> searchInstances(String keyword, Long userId, Integer limit) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return instanceMapper.searchInstances(keyword, userId, limit);
        } catch (Exception e) {
            log.error("搜索实例失败：关键词={}", keyword, e);
            throw new Exception("搜索失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectLatestInstances(Integer limit) throws Exception {
        try {
            return instanceMapper.selectLatestInstances(limit != null ? limit : 10);
        } catch (Exception e) {
            log.error("查询最新实例失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

//    @Override
//    public Map<String, Object> getStatistics() throws Exception {
//        try {
//            return instanceMapper.getInstanceStatistics();
//        } catch (Exception e) {
//            log.error("获取实例统计信息失败", e);
//            throw new Exception("获取统计信息失败：" + e.getMessage());
//        }
//    }

//    @Override
//    public Map<String, Object> getUserStatistics(Long userId) throws Exception {
//        if (userId == null) {
//            return new HashMap<>();
//        }
//        try {
//            return instanceMapper.getUserInstanceStatistics(userId);
//        } catch (Exception e) {
//            log.error("获取用户实例统计信息失败：用户ID={}", userId, e);
//            throw new Exception("获取统计信息失败：" + e.getMessage());
//        }
//    }

    @Override
    public boolean hasAccessPermission(Long instanceId, Long userId) {
        if (instanceId == null || userId == null) {
            return false;
        }
        try {
            return instanceMapper.hasAccessPermission(instanceId, userId);
        } catch (Exception e) {
            log.error("检查实例访问权限失败：实例ID={}, 用户ID={}", instanceId, userId, e);
            return false;
        }
    }

    @Override
    public boolean canDelete(Long instanceId, Long userId) {
        if (instanceId == null || userId == null) {
            return false;
        }
        try {
            return instanceMapper.canDelete(instanceId, userId);
        } catch (Exception e) {
            log.error("检查实例删除权限失败：实例ID={}, 用户ID={}", instanceId, userId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInstance createMarketingInstance(AiInstance instance, Long userId) throws Exception {
        if (instance == null) {
            throw new Exception("实例信息不能为空");
        }

        // 设置为营销类型
        instance.setInstanceType("0");
        instance.setUserId(userId);

        return insert(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInstance createProspectingInstance(AiInstance instance, Long userId) throws Exception {
        if (instance == null) {
            throw new Exception("实例信息不能为空");
        }

        // 设置为拓客类型
        instance.setInstanceType("1");
        instance.setUserId(userId);

        return insert(instance);
    }

    @Override
    public boolean validateInstance(AiInstance instance) {
        if (instance == null) {
            return false;
        }

        return instance.isInstanceNameValid()
               && instance.isInstanceTypeValid()
               && instance.isProxyIdValid()
               && instance.getUserId() != null
               && instance.getUserId() > 0
               && instance.getPlatformId() != null
               && instance.getPlatformId() > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInstance copyInstance(Long sourceId, String newInstanceName, Long userId) throws Exception {
        if (sourceId == null || userId == null) {
            throw new Exception("源实例ID和用户ID不能为空");
        }

        // 获取源实例
        AiInstance source = selectById(sourceId);
        if (source == null) {
            throw new Exception("源实例不存在");
        }

        // 检查访问权限
        if (!hasAccessPermission(sourceId, userId)) {
            throw new Exception("无权限访问该实例");
        }

        // 检查新名称唯一性
        if (!checkInstanceNameUnique(newInstanceName, userId, null)) {
            throw new Exception("实例名称已存在");
        }

        // 创建副本
        AiInstance copy = source.clone(newInstanceName);
        copy.setUserId(userId);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());

        return insert(copy);
    }

    @Override
    public List<InstanceTypeStatistics> getTypeStatistics(Long userId) throws Exception {
        try {
            return instanceMapper.getInstanceTypeStatistics(userId);
        } catch (Exception e) {
            log.error("获取实例类型统计失败", e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    public List<PlatformUsageStatistics> getPlatformUsageStatistics(Long userId) throws Exception {
        try {
            return instanceMapper.getPlatformUsageStatistics(userId);
        } catch (Exception e) {
            log.error("获取平台使用统计失败", e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    public List<CharacterUsageStatistics> getCharacterUsageStatistics(Long userId) throws Exception {
        try {
            return instanceMapper.getCharacterUsageStatistics(userId);
        } catch (Exception e) {
            log.error("获取人设使用统计失败", e);
            throw new Exception("获取统计信息失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectUnboundCharacterInstances(Long userId) throws Exception {
        try {
            return instanceMapper.selectUnboundCharacterInstances(userId);
        } catch (Exception e) {
            log.error("查询未绑定人设的实例失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    public List<AiInstance> selectWithProxyInstances(Long userId) throws Exception {
        try {
            return instanceMapper.selectWithProxyInstances(userId);
        } catch (Exception e) {
            log.error("查询配置了代理的实例失败", e);
            throw new Exception("查询失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateInstanceConfig(List<Long> instanceIds, Integer platformId, Integer characterId) throws Exception {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return false;
        }

        if (platformId == null && characterId == null) {
            throw new Exception("平台ID和人设ID至少需要提供一个");
        }

        try {
            int result = instanceMapper.batchUpdateInstanceConfig(instanceIds, platformId, characterId);
            if (result <= 0) {
                throw new Exception("批量更新实例配置失败");
            }
            log.info("批量更新实例配置成功：更新数量={}", result);
            return true;
        } catch (Exception e) {
            log.error("批量更新实例配置失败：实例IDs={}", instanceIds, e);
            throw new Exception("批量更新失败：" + e.getMessage());
        }
    }

//    @Override
//    public Map<String, Object> getActivityStatistics(Integer days, Long userId) throws Exception {
//        if (days == null || days <= 0) {
//            days = 7; // 默认7天
//        }
//
//        try {
//            return instanceMapper.getInstanceActivityStatistics(days, userId);
//        } catch (Exception e) {
//            log.error("获取实例活动统计失败", e);
//            throw new Exception("获取统计信息失败：" + e.getMessage());
//        }
//    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importInstances(List<AiInstance> instances, boolean updateSupport) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        if (instances == null || instances.isEmpty()) {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("successList", successList);
            result.put("failList", failList);
            return result;
        }

        for (AiInstance instance : instances) {
            try {
                if (!validateInstance(instance)) {
                    failList.add("实例数据无效：" + instance.getInstanceName());
                    continue;
                }

                // 检查是否已存在
                AiInstance existing = null;
                if (instance.getInstanceId() != null) {
                    existing = selectById(instance.getInstanceId());
                }

                if (existing != null) {
                    if (updateSupport) {
                        update(instance);
                        successList.add("更新成功：" + instance.getInstanceName());
                    } else {
                        failList.add("实例已存在：" + instance.getInstanceName());
                    }
                } else {
                    insert(instance);
                    successList.add("创建成功：" + instance.getInstanceName());
                }
            } catch (Exception e) {
                failList.add("处理失败：" + instance.getInstanceName() + "，原因：" + e.getMessage());
            }
        }

        result.put("successCount", successList.size());
        result.put("failCount", failList.size());
        result.put("successList", successList);
        result.put("failList", failList);

        return result;
    }

    @Override
    public String exportInstances(List<AiInstance> instances) throws Exception {
        try {
            // 简单的JSON格式导出
            StringBuilder json = new StringBuilder();
            json.append("[\n");

            for (int i = 0; i < instances.size(); i++) {
                AiInstance instance = instances.get(i);
                json.append("  {\n");
                json.append("    \"instanceId\": ").append(instance.getInstanceId()).append(",\n");
                json.append("    \"instanceName\": \"").append(instance.getInstanceName()).append("\",\n");
                json.append("    \"instanceType\": \"").append(instance.getInstanceType()).append("\",\n");
                json.append("    \"platformId\": ").append(instance.getPlatformId()).append(",\n");
                json.append("    \"characterId\": ").append(instance.getCharacterId()).append(",\n");
                json.append("    \"proxyId\": ").append(instance.getProxyId()).append(",\n");
                json.append("    \"createTime\": \"").append(instance.getCreateTime()).append("\"\n");
                json.append("  }");
                if (i < instances.size() - 1) {
                    json.append(",\n");
                }
            }

            json.append("\n]");
            return json.toString();
        } catch (Exception e) {
            log.error("导出实例数据失败", e);
            throw new Exception("导出失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getConfigurationAnalysis(Long userId) throws Exception {
        try {
            List<AiInstance> instances = selectAccessibleInstances(userId);

            int totalInstances = instances.size();
            int fullyConfigured = 0;
            int withCharacter = 0;
            int withProxy = 0;
            int canRun = 0;

            Map<String, Integer> typeStats = new HashMap<>();
            typeStats.put("marketing", 0);
            typeStats.put("prospecting", 0);

            for (AiInstance instance : instances) {
                if (instance.isFullyConfigured()) {
                    fullyConfigured++;
                }
                if (instance.hasCharacter()) {
                    withCharacter++;
                }
                if (instance.hasProxy()) {
                    withProxy++;
                }
                if (instance.canRun()) {
                    canRun++;
                }

                if (instance.isMarketingType()) {
                    typeStats.put("marketing", typeStats.get("marketing") + 1);
                } else if (instance.isProspectingType()) {
                    typeStats.put("prospecting", typeStats.get("prospecting") + 1);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalInstances", totalInstances);
            result.put("fullyConfigured", fullyConfigured);
            result.put("withCharacter", withCharacter);
            result.put("withProxy", withProxy);
            result.put("canRun", canRun);
            result.put("configurationRate", totalInstances > 0 ? (fullyConfigured * 100.0 / totalInstances) : 0);
            result.put("typeDistribution", typeStats);

            return result;
        } catch (Exception e) {
            log.error("获取实例配置分析失败", e);
            throw new Exception("获取分析失败：" + e.getMessage());
        }
    }

    @Override
    public boolean canRun(Long instanceId) throws Exception {
        AiInstance instance = selectById(instanceId);
        if (instance == null) {
            return false;
        }
        return instance.canRun();
    }

    @Override
    public Map<String, Object> getInstanceStatus(Long instanceId) throws Exception {
        AiInstance instance = selectById(instanceId);
        if (instance == null) {
            throw new Exception("实例不存在");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("instanceId", instanceId);
        status.put("instanceName", instance.getInstanceName());
        status.put("instanceType", instance.getInstanceType());
        status.put("instanceTypeDisplay", instance.getInstanceTypeDisplay());
        status.put("canRun", instance.canRun());
        status.put("isFullyConfigured", instance.isFullyConfigured());
        status.put("hasCharacter", instance.hasCharacter());
        status.put("hasProxy", instance.hasProxy());
        status.put("configurationCompleteness", instance.getConfigurationCompleteness());
        status.put("platformId", instance.getPlatformId());
        status.put("characterId", instance.getCharacterId());
        status.put("proxyId", instance.getProxyId());
        status.put("createTime", instance.getCreateTime());
        status.put("updateTime", instance.getUpdateTime());

        return status;
    }

    @Override
    public List<AiInstance> selectByUserIdAndTypeAndPlatform(Long userId, String instanceType, Integer platformId) {
        try {
            if (userId == null) {
                return new ArrayList<>();
            }

            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("instanceType", instanceType);
            params.put("platformId", platformId);

            return instanceMapper.selectByUserIdAndTypeAndPlatform(params);
        } catch (Exception e) {
            log.error("根据用户ID、实例类型和平台ID查询实例失败：userId={}, instanceType={}, platformId={}",
                userId, instanceType, platformId, e);
            throw new RuntimeException("查询实例失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<AiInstance> selectByUserIdAndType(Long userId, String instanceType) {
        try {
            if (userId == null) {
                return new ArrayList<>();
            }

            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("instanceType", instanceType);

            return instanceMapper.selectByUserIdAndType(params);
        } catch (Exception e) {
            log.error("根据用户ID和实例类型查询实例失败：userId={}, instanceType={}",
                userId, instanceType, e);
            throw new RuntimeException("查询实例失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiInstance createInstanceWithValidation(AiInstance instance, Long currentUserId) {
        try {
            // 1. 参数验证
            if (instance == null) {
                throw new IllegalArgumentException("实例信息不能为空");
            }
            if (instance.getInstanceName() == null || instance.getInstanceName().trim().isEmpty()) {
                throw new IllegalArgumentException("实例名称不能为空");
            }
            if (instance.getInstanceType() == null) {
                throw new IllegalArgumentException("实例类型不能为空");
            }
            if (instance.getPlatformId() == null) {
                throw new IllegalArgumentException("平台ID不能为空");
            }

            // 2. 用户权限验证 - 买家总账户或子账户可以创建实例
            SysUser currentUser = userService.selectUserWithDept(currentUserId);
            if (currentUser == null) {
                throw new IllegalArgumentException("用户信息不存在");
            }

            boolean buyerMain = currentUser.isBuyerMainIdentity();
            boolean buyerSub = currentUser.isBuyerSubIdentity();
            if (!buyerMain && !buyerSub) {
                throw new IllegalArgumentException("仅支持商家总账户或子账户创建实例");
            }

            Long parentUserId = buyerSub ? currentUser.getParentUserId() : currentUserId;
            if (buyerSub && (parentUserId == null || parentUserId <= 0)) {
                throw new IllegalArgumentException("未找到商家总账户信息");
            }
            boolean skipBilling = buyerMain;

            // 3. 根据实例类型进行不同的业务逻辑处理
            String instanceType = instance.getInstanceType();
            if ("0".equals(instanceType)) {
                // 营销实例处理逻辑
                return createMarketingInstance(instance, currentUserId, parentUserId, skipBilling);
            } else if ("1".equals(instanceType)) {
                // 拓客实例处理逻辑
                return createProspectingInstance(instance, currentUserId, parentUserId, skipBilling);
            } else {
                throw new IllegalArgumentException("不支持的实例类型：" + instanceType);
            }

        } catch (InsufficientMarketingInstanceException e) {
            throw e; // 重新抛出营销实例数量不足异常
        } catch (IllegalArgumentException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("创建实例失败：名称={}", instance.getInstanceName(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 创建营销实例
     */
    private AiInstance createMarketingInstance(AiInstance instance, Long currentUserId, Long parentUserId, boolean skipBilling) {
        try {
            instance.setCreateBy(SecurityUtils.getCurrentUsername());
            instance.setUserId(currentUserId);

            if (!skipBilling) {
                UserDrBalance parentBalance = balanceService.getByUserId(parentUserId);
                if (parentBalance == null) {
                    throw new IllegalArgumentException("父账户余额信息不存在");
                }

                BigDecimal availableBalance = parentBalance.getDrBalance();

                DrPriceConfig preDeductConfig = priceConfigService.selectDrPriceConfigByBusinessType(
                    DrPriceConfig.BUSINESS_TYPE_INSTANCE_PRE_DEDUCT);
                if (preDeductConfig == null || !preDeductConfig.isActive() || preDeductConfig.getDrPrice() == null) {
                    throw new IllegalStateException("营销实例预扣费价格配置不存在或未启用");
                }
                BigDecimal marketingInstancePrice = preDeductConfig.getDrPrice();

                if (availableBalance.compareTo(marketingInstancePrice) < 0) {
                    throw new IllegalArgumentException("余额不足，无法创建营销实例");
                }

                if (!userDrBalanceService.preDeductForInstance(parentUserId, marketingInstancePrice, currentUserId)) {
                    throw new IllegalArgumentException("预扣费操作失败");
                }
                log.info("为父账户 {} 转移 {} 元到预扣费余额", parentUserId, marketingInstancePrice);
            } else {
                log.info("商家总账户 {} 创建营销实例，跳过预扣费与扣费。", currentUserId);
            }

            AiInstance created = insert(instance);

            if (!skipBilling) {
                deductDailyFeeWithRecord(created, parentUserId, "0");
            }

            return created;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建营销实例失败：名称={}", instance.getInstanceName(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 创建拓客实例
     */
    private AiInstance createProspectingInstance(AiInstance instance, Long currentUserId, Long parentUserId, boolean skipBilling) {
        try {
            // 1. 获取当前用户的实例统计
            Integer platformId = instance.getPlatformId();

            // 查询实例数量仅用于审计记录，不再限制拓客数量
            List<AiInstance> allMarketingInstances = selectByUserIdAndType(currentUserId, "0");
            int totalMarketingCount = allMarketingInstances.size();
            List<AiInstance> platformProspectingInstances = selectByUserIdAndTypeAndPlatform(currentUserId, "1", platformId);
            int platformProspectingCount = platformProspectingInstances.size();

            log.info("用户 {} 拓客实例检查（不再限制数量）：全局营销实例数={}，平台{}拓客实例数={}",
                currentUserId, totalMarketingCount, platformId, platformProspectingCount);

            // 3. 设置创建者信息并创建实例
            instance.setCreateBy(SecurityUtils.getCurrentUsername());
            instance.setUserId(currentUserId);

            AiInstance created = insert(instance);

            if (!skipBilling) {
                deductDailyFeeWithRecord(created, parentUserId, "1"); // "1"表示拓客实例
            } else {
                log.info("商家总账户 {} 创建拓客实例，跳过当天扣费。", currentUserId);
            }

            return created;

        } catch (InsufficientMarketingInstanceException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建拓客实例失败：名称={}", instance.getInstanceName(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 扣除当天实例费用并创建BillingRecord
     *
     * @param instance 创建的实例
     * @param parentUserId 父用户ID
     * @param instanceType 实例类型（"0"营销，"1"拓客）
     */
    private void deductDailyFeeWithRecord(AiInstance instance, Long parentUserId, String instanceType) {
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
            billingRecord.setUserId(parentUserId); // 实际扣费的是买家总账户
            billingRecord.setOperatorId(parentUserId); // 自动扣费
            billingRecord.setBillType(2); // 消费类型
            billingRecord.setBillingType(priceConfig.getBillingType()); // 结算类型
            billingRecord.setBusinessType(priceConfig.getBusinessType());
            billingRecord.setDrAmount(todayFee);
            billingRecord.setBalanceBefore(balanceService.getByUserId(parentUserId).getDrBalance());
            billingRecord.setBalanceAfter(billingRecord.getBalanceBefore().subtract(todayFee));
            billingRecord.setDescription(String.format("实例创建当天费用 - %s（剩余%d小时）",
                instance.getInstanceName(), (int)remainingHours));
            billingRecord.setStatus(1); // 成功状态
            billingRecord.setCreateBy("system");
            billingRecord.setCreateTime(java.time.LocalDateTime.now());

            // 调用扣费服务
            String result = balanceService.deduct(billingRecord);

            if (result == null || !result.startsWith("扣费成功")) {
                log.error("实例创建当天扣费失败：{}, 错误：{}", instance.getInstanceName(), result);
                throw new RuntimeException("当天费用扣费失败：" + result);
            } else {
                log.info("实例创建当天扣费成功：{}, 金额：{} DR, 结果：{}", instance.getInstanceName(), todayFee, result);
            }

        } catch (Exception e) {
            log.error("扣除实例当天费用失败：实例={}", instance.getInstanceName(), e);
            throw new RuntimeException("当天费用扣费失败：" + e.getMessage(), e);
        }
    }
}
