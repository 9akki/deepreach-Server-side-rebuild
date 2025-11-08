package com.deepreach.common.core.service;

import com.deepreach.common.core.domain.entity.DrPriceConfig;
import java.util.List;

public interface DrPriceConfigService extends BaseService<DrPriceConfig> {

    List<DrPriceConfig> selectDrPriceConfigList(DrPriceConfig drPriceConfig);

    List<DrPriceConfig> selectDrPriceConfigPage(DrPriceConfig drPriceConfig);

    DrPriceConfig selectDrPriceConfigByPriceId(Long priceId);

    DrPriceConfig selectDrPriceConfigByBusinessType(String businessType);

    List<DrPriceConfig> selectDrPriceConfigByStatus(String status);

    List<DrPriceConfig> selectDrPriceConfigByBillingType(Integer billingType);

    int insertDrPriceConfig(DrPriceConfig drPriceConfig);

    int updateDrPriceConfig(DrPriceConfig drPriceConfig);

    int deleteDrPriceConfigByPriceIds(Long[] priceIds);

    int deleteDrPriceConfigByPriceId(Long priceId);

    boolean checkBusinessTypeUnique(String businessType, Long excludePriceId);

    int updateStatus(Long priceId, String status);

    List<DrPriceConfig> selectActivePriceConfigs();

    int batchUpdateStatus(Long[] priceIds, String status);

    int initDefaultPriceConfigs();

    int resetToDefault(String businessType);

    int batchImportPriceConfigs(List<DrPriceConfig> priceConfigs);

    String validatePriceConfig(DrPriceConfig drPriceConfig);
}
