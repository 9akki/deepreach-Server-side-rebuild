package com.deepreach.web.service.impl;

import com.deepreach.common.core.service.impl.BaseServiceImpl;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.web.entity.DrPriceConfig;
import com.deepreach.web.mapper.DrPriceConfigMapper;
import com.deepreach.web.service.DrPriceConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DR价格配置Service业务层处理
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrPriceConfigServiceImpl extends BaseServiceImpl<DrPriceConfigMapper, DrPriceConfig> implements DrPriceConfigService {

    private final DrPriceConfigMapper drPriceConfigMapper;

    /**
     * 查询DR价格配置列表
     *
     * @param drPriceConfig DR价格配置
     * @return DR价格配置
     */
    @Override
    public List<DrPriceConfig> selectDrPriceConfigList(DrPriceConfig drPriceConfig) {
        return drPriceConfigMapper.selectDrPriceConfigList(drPriceConfig);
    }

    /**
     * 查询DR价格配置分页列表
     *
     * @param drPriceConfig DR价格配置
     * @return DR价格配置
     */
    @Override
    public List<DrPriceConfig> selectDrPriceConfigPage(DrPriceConfig drPriceConfig) {
        return drPriceConfigMapper.selectDrPriceConfigPage(drPriceConfig);
    }

    /**
     * 查询DR价格配置
     *
     * @param priceId DR价格配置主键
     * @return DR价格配置
     */
    @Override
    public DrPriceConfig selectDrPriceConfigByPriceId(Long priceId) {
        return drPriceConfigMapper.selectDrPriceConfigByPriceId(priceId);
    }

    /**
     * 根据业务类型查询价格配置
     *
     * @param businessType 业务类型
     * @return DR价格配置
     */
    @Override
    public DrPriceConfig selectDrPriceConfigByBusinessType(String businessType) {
        return drPriceConfigMapper.selectDrPriceConfigByBusinessType(businessType);
    }

    /**
     * 根据状态查询价格配置列表
     *
     * @param status 状态
     * @return DR价格配置列表
     */
    @Override
    public List<DrPriceConfig> selectDrPriceConfigByStatus(String status) {
        return drPriceConfigMapper.selectDrPriceConfigByStatus(status);
    }

    /**
     * 根据结算类型查询价格配置列表
     *
     * @param billingType 结算类型
     * @return DR价格配置列表
     */
    @Override
    public List<DrPriceConfig> selectDrPriceConfigByBillingType(Integer billingType) {
        return drPriceConfigMapper.selectDrPriceConfigByBillingType(billingType);
    }

    /**
     * 新增DR价格配置
     *
     * @param drPriceConfig DR价格配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDrPriceConfig(DrPriceConfig drPriceConfig) {
        // 验证价格配置
        String validation = validatePriceConfig(drPriceConfig);
        if (validation != null) {
            throw new ServiceException(validation);
        }

        // 检查业务类型唯一性
        if (!checkBusinessTypeUnique(drPriceConfig.getBusinessType(), null)) {
            throw new ServiceException("业务类型已存在");
        }

        drPriceConfig.setStatus("0"); // 默认启用
        return drPriceConfigMapper.insertDrPriceConfig(drPriceConfig);
    }

    /**
     * 修改DR价格配置
     *
     * @param drPriceConfig DR价格配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDrPriceConfig(DrPriceConfig drPriceConfig) {
        // 验证价格配置
        String validation = validatePriceConfig(drPriceConfig);
        if (validation != null) {
            throw new ServiceException(validation);
        }

        // 检查业务类型唯一性
        if (!checkBusinessTypeUnique(drPriceConfig.getBusinessType(), drPriceConfig.getPriceId())) {
            throw new ServiceException("业务类型已存在");
        }

        return drPriceConfigMapper.updateDrPriceConfig(drPriceConfig);
    }

    /**
     * 批量删除DR价格配置
     *
     * @param priceIds 需要删除的DR价格配置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDrPriceConfigByPriceIds(Long[] priceIds) {
        return drPriceConfigMapper.deleteDrPriceConfigByPriceIds(priceIds);
    }

    /**
     * 删除DR价格配置信息
     *
     * @param priceId DR价格配置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDrPriceConfigByPriceId(Long priceId) {
        return drPriceConfigMapper.deleteDrPriceConfigByPriceId(priceId);
    }

    /**
     * 检查业务类型是否唯一
     *
     * @param businessType 业务类型
     * @param excludePriceId 排除的价格ID
     * @return true:唯一 false:不唯一
     */
    @Override
    public boolean checkBusinessTypeUnique(String businessType, Long excludePriceId) {
        int count = drPriceConfigMapper.checkBusinessTypeUnique(businessType, excludePriceId);
        return count == 0;
    }

    /**
     * 启用/禁用价格配置
     *
     * @param priceId 价格ID
     * @param status 状态
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(Long priceId, String status) {
        return drPriceConfigMapper.updateStatus(priceId, status);
    }

    /**
     * 获取所有启用的价格配置
     *
     * @return DR价格配置列表
     */
    @Override
    public List<DrPriceConfig> selectActivePriceConfigs() {
        return drPriceConfigMapper.selectActivePriceConfigs();
    }

    /**
     * 批量更新价格配置状态
     *
     * @param priceIds 价格ID数组
     * @param status 状态
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(Long[] priceIds, String status) {
        return drPriceConfigMapper.batchUpdateStatus(priceIds, status);
    }

    /**
     * 初始化默认价格配置
     *
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initDefaultPriceConfigs() {
        List<DrPriceConfig> defaultConfigs = new ArrayList<>();
        defaultConfigs.add(DrPriceConfig.createInstancePreDeductConfig());
        defaultConfigs.add(DrPriceConfig.createMarketingInstanceConfig());
        defaultConfigs.add(DrPriceConfig.createProspectingInstanceConfig());
        defaultConfigs.add(DrPriceConfig.createAiCharacterConfig());
        defaultConfigs.add(DrPriceConfig.createSmsConfig());
        defaultConfigs.add(DrPriceConfig.createTokenConfig());

        int successCount = 0;
        for (DrPriceConfig config : defaultConfigs) {
            try {
                // 检查是否已存在
                DrPriceConfig existing = selectDrPriceConfigByBusinessType(config.getBusinessType());
                if (existing == null) {
                    insertDrPriceConfig(config);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("初始化价格配置失败: {}", config.getBusinessType(), e);
            }
        }
        return successCount;
    }

    /**
     * 重置为默认价格配置
     *
     * @param businessType 业务类型
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetToDefault(String businessType) {
        DrPriceConfig defaultConfig = null;

        switch (businessType) {
            case DrPriceConfig.BUSINESS_TYPE_INSTANCE_PRE_DEDUCT:
                defaultConfig = DrPriceConfig.createInstancePreDeductConfig();
                break;
            case DrPriceConfig.BUSINESS_TYPE_INSTANCE_MARKETING:
                defaultConfig = DrPriceConfig.createMarketingInstanceConfig();
                break;
            case DrPriceConfig.BUSINESS_TYPE_INSTANCE_PROSPECTING:
                defaultConfig = DrPriceConfig.createProspectingInstanceConfig();
                break;
            case DrPriceConfig.BUSINESS_TYPE_AI_CHARACTER:
                defaultConfig = DrPriceConfig.createAiCharacterConfig();
                break;
            case DrPriceConfig.BUSINESS_TYPE_SMS:
                defaultConfig = DrPriceConfig.createSmsConfig();
                break;
            case DrPriceConfig.BUSINESS_TYPE_TOKEN:
                defaultConfig = DrPriceConfig.createTokenConfig();
                break;
            default:
                throw new ServiceException("不支持的业务类型");
        }

        DrPriceConfig existing = selectDrPriceConfigByBusinessType(businessType);
        if (existing != null) {
            defaultConfig.setPriceId(existing.getPriceId());
            return updateDrPriceConfig(defaultConfig);
        } else {
            return insertDrPriceConfig(defaultConfig);
        }
    }

    /**
     * 批量导入价格配置
     *
     * @param priceConfigs 价格配置列表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportPriceConfigs(List<DrPriceConfig> priceConfigs) {
        int successCount = 0;
        for (DrPriceConfig config : priceConfigs) {
            try {
                String validation = validatePriceConfig(config);
                if (validation != null) {
                    log.warn("价格配置验证失败: {}, 跳过", validation);
                    continue;
                }

                DrPriceConfig existing = selectDrPriceConfigByBusinessType(config.getBusinessType());
                if (existing != null) {
                    config.setPriceId(existing.getPriceId());
                    updateDrPriceConfig(config);
                } else {
                    insertDrPriceConfig(config);
                }
                successCount++;
            } catch (Exception e) {
                log.error("导入价格配置失败: {}", config.getBusinessType(), e);
            }
        }
        return successCount;
    }

    /**
     * 验证价格配置数据
     *
     * @param drPriceConfig 价格配置
     * @return 验证结果
     */
    @Override
    public String validatePriceConfig(DrPriceConfig drPriceConfig) {
        if (drPriceConfig == null) {
            return "价格配置不能为空";
        }

        if (drPriceConfig.getBusinessType() == null || drPriceConfig.getBusinessType().trim().isEmpty()) {
            return "业务类型不能为空";
        }

        if (drPriceConfig.getBusinessName() == null || drPriceConfig.getBusinessName().trim().isEmpty()) {
            return "业务名称不能为空";
        }

        if (drPriceConfig.getPriceUnit() == null || drPriceConfig.getPriceUnit().trim().isEmpty()) {
            return "计价单位不能为空";
        }

        if (drPriceConfig.getDrPrice() == null) {
            return "DR积分单价不能为空";
        }

        if (drPriceConfig.getDrPrice().compareTo(BigDecimal.ZERO) < 0) {
            return "DR积分单价不能为负数";
        }

        if (drPriceConfig.getBillingType() == null) {
            return "结算类型不能为空";
        }

        if (drPriceConfig.getBillingType() != 1 && drPriceConfig.getBillingType() != 2) {
            return "结算类型必须为1(秒结秒扣)或2(日结日扣)";
        }

        return null; // 验证通过
    }
}
