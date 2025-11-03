package com.deepreach.web.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import com.deepreach.web.service.BuyerInstanceStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 买家实例统计接口。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/buyer/instances")
@RequiredArgsConstructor
public class BuyerInstanceStatisticsController extends BaseController {

    private final BuyerInstanceStatisticsService buyerInstanceStatisticsService;

    /**
     * 根据买家总账户用户ID统计实例。
     */
    @GetMapping("/by-user-id/{buyerMainUserId}")
    @Log(title = "买家实例统计", businessType = BusinessType.OTHER)
    public Result<Map<String, Object>> getBuyerInstancesByUserId(@PathVariable Long buyerMainUserId) {
        try {
            Map<String, Object> statistics = buyerInstanceStatisticsService.getBuyerSubInstanceStatistics(buyerMainUserId);
            return success(statistics);
        } catch (Exception e) {
            log.error("根据用户ID获取买家实例统计失败：buyerMainUserId={}", buyerMainUserId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据买家总账户用户名统计实例。
     */
    @GetMapping("/by-username")
    @Log(title = "买家实例统计", businessType = BusinessType.OTHER)
    public Result<Map<String, Object>> getBuyerInstancesByUsername(@RequestParam("username") String username) {
        try {
            Map<String, Object> statistics = buyerInstanceStatisticsService.getBuyerSubInstanceStatistics(username);
            return success(statistics);
        } catch (Exception e) {
            log.error("根据用户名获取买家实例统计失败：username={}", username, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据买家总账户用户ID获取综合统计信息。
     */
    @GetMapping("/overview/{buyerMainUserId}")
    @Log(title = "买家实例综合统计", businessType = BusinessType.OTHER)
    public Result<Map<String, Object>> getBuyerHierarchyOverview(@PathVariable Long buyerMainUserId) {
        try {
            Map<String, Object> overview = buyerInstanceStatisticsService.getBuyerHierarchyOverview(buyerMainUserId);
            return success(overview);
        } catch (Exception e) {
            log.error("根据用户ID获取买家层级综合统计失败：buyerMainUserId={}", buyerMainUserId, e);
            return Result.error(e.getMessage());
        }
    }
}
