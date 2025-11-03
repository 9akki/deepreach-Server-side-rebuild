package com.deepreach.web.mapper;

import com.deepreach.common.core.mapper.BaseMapper;
import com.deepreach.web.entity.DrPriceConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DR价格配置Mapper接口
 *
 * @author DeepReach Team
 * @version 1.0
 */
public interface DrPriceConfigMapper extends BaseMapper<DrPriceConfig> {

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
    DrPriceConfig selectDrPriceConfigByBusinessType(@Param("businessType") String businessType);

    /**
     * 根据状态查询价格配置列表
     *
     * @param status 状态
     * @return DR价格配置列表
     */
    List<DrPriceConfig> selectDrPriceConfigByStatus(@Param("status") String status);

    /**
     * 根据结算类型查询价格配置列表
     *
     * @param billingType 结算类型
     * @return DR价格配置列表
     */
    List<DrPriceConfig> selectDrPriceConfigByBillingType(@Param("billingType") Integer billingType);

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
     * 删除DR价格配置
     *
     * @param priceId DR价格配置主键
     * @return 结果
     */
    int deleteDrPriceConfigByPriceId(Long priceId);

    /**
     * 批量删除DR价格配置
     *
     * @param priceIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDrPriceConfigByPriceIds(Long[] priceIds);

    /**
     * 检查业务类型是否唯一
     *
     * @param businessType 业务类型
     * @param excludePriceId 排除的价格ID
     * @return 数量
     */
    int checkBusinessTypeUnique(@Param("businessType") String businessType,
                               @Param("excludePriceId") Long excludePriceId);

    /**
     * 启用/禁用价格配置
     *
     * @param priceId 价格ID
     * @param status 状态
     * @return 结果
     */
    int updateStatus(@Param("priceId") Long priceId, @Param("status") String status);

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
    int batchUpdateStatus(@Param("priceIds") Long[] priceIds, @Param("status") String status);
}