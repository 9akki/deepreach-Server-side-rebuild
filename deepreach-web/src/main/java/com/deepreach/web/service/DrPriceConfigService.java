package com.deepreach.web.service;

import com.deepreach.common.core.service.BaseService;
import com.deepreach.web.entity.DrPriceConfig;

import java.util.List;

/**
 * DR价格配置Service接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
public interface DrPriceConfigService extends BaseService<DrPriceConfig> {

    /**
     * 查询DR价格配置列表
     *
     * @param drPriceConfig DR价格配置
     * @return DR价格配置集合
     */
    List<DrPriceConfig> selectDrPriceConfigList(DrPriceConfig drPriceConfig);

    /**
     * 查询DR价格配置分页列表
     *
     * @param drPriceConfig DR价格配置
     * @return DR价格配置集合
     */
    List<DrPriceConfig> selectDrPriceConfigPage(DrPriceConfig drPriceConfig);

    /**
     * 查询DR价格配置
     *
     * @param priceId DR价格配置主键
     * @return DR价格配置
     */
    DrPriceConfig selectDrPriceConfigByPriceId(Long priceId);

    /**
     * 根据业务类型查询价格配置
     *
     * @param businessType 业务类型
     * @return DR价格配置
     */
    DrPriceConfig selectDrPriceConfigByBusinessType(String businessType);

    /**
     * 根据状态查询价格配置列表
     *
     * @param status 状态
     * @return DR价格配置列表
     */
    List<DrPriceConfig> selectDrPriceConfigByStatus(String status);

    /**
     * 根据结算类型查询价格配置列表
     *
     * @param billingType 结算类型
     * @return DR价格配置列表
     */
    List<DrPriceConfig> selectDrPriceConfigByBillingType(Integer billingType);

    /**
     * 新增DR价格配置
     *
     * @param drPriceConfig DR价格配置
     * @return 结果
     */
    int insertDrPriceConfig(DrPriceConfig drPriceConfig);

    /**
     * 修改DR价格配置
     *
     * @param drPriceConfig DR价格配置
     * @return 结果
     */
    int updateDrPriceConfig(DrPriceConfig drPriceConfig);

    /**
     * 批量删除DR价格配置
     *
     * @param priceIds 需要删除的DR价格配置主键集合
     * @return 结果
     */
    int deleteDrPriceConfigByPriceIds(Long[] priceIds);

    /**
     * 删除DR价格配置信息
     *
     * @param priceId DR价格配置主键
     * @return 结果
     */
    int deleteDrPriceConfigByPriceId(Long priceId);

    /**
     * 检查业务类型是否唯一
     *
     * @param businessType 业务类型
     * @param excludePriceId 排除的价格ID
     * @return true:唯一 false:不唯一
     */
    boolean checkBusinessTypeUnique(String businessType, Long excludePriceId);

    /**
     * 启用/禁用价格配置
     *
     * @param priceId 价格ID
     * @param status 状态
     * @return 结果
     */
    int updateStatus(Long priceId, String status);

    /**
     * 获取所有启用的价格配置
     *
     * @return DR价格配置列表
     */
    List<DrPriceConfig> selectActivePriceConfigs();

    /**
     * 批量更新价格配置状态
     *
     * @param priceIds 价格ID数组
     * @param status 状态
     * @return 结果
     */
    int batchUpdateStatus(Long[] priceIds, String status);

    /**
     * 初始化默认价格配置
     *
     * @return 结果
     */
    int initDefaultPriceConfigs();

    /**
     * 重置为默认价格配置
     *
     * @param businessType 业务类型
     * @return 结果
     */
    int resetToDefault(String businessType);

    /**
     * 批量导入价格配置
     *
     * @param priceConfigs 价格配置列表
     * @return 结果
     */
    int batchImportPriceConfigs(List<DrPriceConfig> priceConfigs);

    /**
     * 验证价格配置数据
     *
     * @param drPriceConfig 价格配置
     * @return 验证结果
     */
    String validatePriceConfig(DrPriceConfig drPriceConfig);
}