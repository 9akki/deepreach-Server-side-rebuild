package com.deepreach.web.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.service.DrPriceConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DR价格配置控制器
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/dr/price/config")
@RequiredArgsConstructor
public class DrPriceConfigController extends BaseController {

    private final DrPriceConfigService drPriceConfigService;

    /**
     * 查询DR价格配置列表
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:list')")
    @GetMapping("/list")
    public TableDataInfo<DrPriceConfig> list(DrPriceConfig drPriceConfig) {
        com.deepreach.common.core.page.PageDomain pageDomain = com.deepreach.common.core.page.TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum() != null ? pageDomain.getPageNum() : 1;
        Integer pageSize = pageDomain.getPageSize() != null ? pageDomain.getPageSize() : 10;
        List<DrPriceConfig> all = drPriceConfigService.selectDrPriceConfigPage(drPriceConfig);
        List<DrPriceConfig> rows = com.deepreach.common.utils.PageUtils.manualPage(all, pageNum, pageSize);
        return getDataTable(rows);
    }

    /**
     * 导出DR价格配置列表
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:export')")
    @Log(title = "DR价格配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(DrPriceConfig drPriceConfig) {
        List<DrPriceConfig> list = drPriceConfigService.selectDrPriceConfigList(drPriceConfig);
        // 这里可以添加导出Excel的逻辑
        // exportExcel(list, "DR价格配置数据");
    }

    /**
     * 获取DR价格配置详细信息
     */
    @GetMapping(value = "/{priceId}")
    public Result<DrPriceConfig> getInfo(@PathVariable("priceId") Long priceId) {
        return Result.success(drPriceConfigService.selectDrPriceConfigByPriceId(priceId));
    }

    /**
     * 根据业务类型获取价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:query')")
    @GetMapping(value = "/business/{businessType}")
    public Result<DrPriceConfig> getByBusinessType(@PathVariable("businessType") String businessType) {
        return Result.success(drPriceConfigService.selectDrPriceConfigByBusinessType(businessType));
    }

    /**
     * 获取所有启用的价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:query')")
    @GetMapping(value = "/active")
    public Result<List<DrPriceConfig>> getActiveConfigs() {
        return Result.success(drPriceConfigService.selectActivePriceConfigs());
    }

    /**
     * 新增DR价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:add')")
    @Log(title = "DR价格配置", businessType = BusinessType.INSERT)
    @PostMapping
    public Result<String> add(@Validated @RequestBody DrPriceConfig drPriceConfig) {
        if (!drPriceConfigService.checkBusinessTypeUnique(drPriceConfig.getBusinessType(), null)) {
            return Result.error("新增价格配置'" + drPriceConfig.getBusinessName() + "'失败，业务类型已存在");
        }
        int result = drPriceConfigService.insertDrPriceConfig(drPriceConfig);
        return result > 0 ? Result.success("新增成功") : Result.error("新增失败");
    }

        /**
         * 修改DR价格配置
         */
        // @PreAuthorize("@ss.hasPermi('dr:price:edit')")
        @Log(title = "DR价格配置", businessType = BusinessType.UPDATE)
        @PutMapping
        public Result<String> edit(@Validated @RequestBody DrPriceConfig drPriceConfig) {
            if (!drPriceConfigService.checkBusinessTypeUnique(drPriceConfig.getBusinessType(), drPriceConfig.getPriceId())) {
                return Result.error("修改价格配置'" + drPriceConfig.getBusinessName() + "'失败，业务类型已存在");
            }
            int result = drPriceConfigService.updateDrPriceConfig(drPriceConfig);
            return result > 0 ? Result.success("修改成功") : Result.error("修改失败");
        }

    /**
     * 删除DR价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:remove')")
    @Log(title = "DR价格配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{priceIds}")
    public Result<String> remove(@PathVariable Long[] priceIds) {
        int result = drPriceConfigService.deleteDrPriceConfigByPriceIds(priceIds);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    /**
     * 启用/禁用价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:edit')")
    @Log(title = "DR价格配置", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public Result<String> changeStatus(@RequestBody DrPriceConfig drPriceConfig) {
        int result = drPriceConfigService.updateStatus(drPriceConfig.getPriceId(), drPriceConfig.getStatus());
        return result > 0 ? Result.success("状态更新成功") : Result.error("状态更新失败");
    }

    /**
     * 批量启用/禁用价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:edit')")
    @Log(title = "DR价格配置", businessType = BusinessType.UPDATE)
    @PutMapping("/status/batch")
    public Result<String> batchChangeStatus(@RequestParam Long[] priceIds, @RequestParam String status) {
        int result = drPriceConfigService.batchUpdateStatus(priceIds, status);
        return result > 0 ? Result.success("批量状态更新成功") : Result.error("批量状态更新失败");
    }

    /**
     * 初始化默认价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:add')")
    @Log(title = "DR价格配置", businessType = BusinessType.INSERT)
    @PostMapping("/init")
    public Result<String> initDefaultConfigs() {
        int count = drPriceConfigService.initDefaultPriceConfigs();
        return Result.success("成功初始化 " + count + " 个默认价格配置");
    }

    /**
     * 重置为默认价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:edit')")
    @Log(title = "DR价格配置", businessType = BusinessType.UPDATE)
    @PutMapping("/reset/{businessType}")
    public Result<String> resetToDefault(@PathVariable String businessType) {
        int result = drPriceConfigService.resetToDefault(businessType);
        return result > 0 ? Result.success("重置成功") : Result.error("重置失败");
    }

    /**
     * 批量导入价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:import')")
    @Log(title = "DR价格配置", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public Result<String> importData(@RequestBody List<DrPriceConfig> priceConfigs) {
        int successCount = drPriceConfigService.batchImportPriceConfigs(priceConfigs);
        return Result.success("成功导入 " + successCount + " 个价格配置");
    }

    /**
     * 根据结算类型查询价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:query')")
    @GetMapping("/billing/{billingType}")
    public Result<List<DrPriceConfig>> getByBillingType(@PathVariable Integer billingType) {
        return Result.success(drPriceConfigService.selectDrPriceConfigByBillingType(billingType));
    }

    /**
     * 根据状态查询价格配置
     */
    // @PreAuthorize("@ss.hasPermi('dr:price:query')")
    @GetMapping("/status/{status}")
    public Result<List<DrPriceConfig>> getByStatus(@PathVariable String status) {
        return Result.success(drPriceConfigService.selectDrPriceConfigByStatus(status));
    }

    /**
     * 检查业务类型唯一性
     */
    @GetMapping("/check-unique")
    public Result<Boolean> checkBusinessTypeUnique(@RequestParam String businessType,
                                                   @RequestParam(required = false) Long excludePriceId) {
        boolean unique = drPriceConfigService.checkBusinessTypeUnique(businessType, excludePriceId);
        return Result.success(unique);
    }

    /**
     * 验证价格配置
     */
    @PostMapping("/validate")
    public Result<String> validateConfig(@RequestBody DrPriceConfig drPriceConfig) {
        String validation = drPriceConfigService.validatePriceConfig(drPriceConfig);
        if (validation != null) {
            return Result.error(validation);
        }
        return Result.success("验证通过");
    }

    /**
     * 创建预扣费价格配置
     */
    @GetMapping("/create/pre-deduct")
    public Result<DrPriceConfig> createPreDeductConfig() {
        return Result.success(DrPriceConfig.createInstancePreDeductConfig());
    }

    /**
     * 创建营销实例价格配置
     */
    @GetMapping("/create/marketing")
    public Result<DrPriceConfig> createMarketingConfig() {
        return Result.success(DrPriceConfig.createMarketingInstanceConfig());
    }

    /**
     * 创建拓客实例价格配置
     */
    @GetMapping("/create/prospecting")
    public Result<DrPriceConfig> createProspectingConfig() {
        return Result.success(DrPriceConfig.createProspectingInstanceConfig());
    }

    /**
     * 创建短信服务价格配置
     */
    @GetMapping("/create/sms")
    public Result<DrPriceConfig> createSmsConfig() {
        return Result.success(DrPriceConfig.createSmsConfig());
    }

    /**
     * 创建AI服务Token价格配置
     */
    @GetMapping("/create/token")
    public Result<DrPriceConfig> createTokenConfig() {
        return Result.success(DrPriceConfig.createTokenConfig());
    }
}
