package com.deepreach.web.controller;

import com.deepreach.common.core.dto.CloudComputerParameterDTO;
import com.deepreach.common.core.service.CloudComputerService;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 云电脑管理Controller
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Slf4j
@RestController
@RequestMapping("/cloud-computer")
public class CloudComputerController {

    @Autowired
    private CloudComputerService cloudComputerService;

    /**
     * 获取云电脑参数
     *
     * 通过用户ID判断是否分配云电脑，返回相应的参数信息
     *
     * @param userId 用户ID
     * @return 云电脑参数
     */
    @GetMapping("/parameter/{userId}")
    @Log(title = "云电脑参数查询", businessType = BusinessType.OTHER)
    public Result getCloudComputerParameter(@PathVariable("userId") Long userId) {
        try {
            log.info("收到获取云电脑参数请求，用户ID：{}", userId);

            CloudComputerParameterDTO result = cloudComputerService.getCloudComputerParameter(userId);

            if (result.getStatus() == 0) {
                return Result.success(result.getMessage(), result);
            } else {
                return Result.error(500, result.getMessage(), result);
            }

        } catch (Exception e) {
            log.error("获取云电脑参数失败：用户ID={}", userId, e);
            return Result.error("获取云电脑参数失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户的云电脑参数
     *
     * @return 当前用户的云电脑参数
     */
    @GetMapping("/parameter/current")
    @Log(title = "当前用户云电脑参数查询", businessType = BusinessType.OTHER)
    public Result getCurrentUserCloudComputerParameter() {
        try {
            // 获取当前登录用户ID（这里需要根据实际的安全框架实现）
            Long currentUserId = getCurrentUserId();

            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            log.info("获取当前用户{}的云电脑参数", currentUserId);

            CloudComputerParameterDTO result = cloudComputerService.getCloudComputerParameter(currentUserId);

            if (result.getStatus() == 0) {
                return Result.success(result.getMessage(), result);
            } else {
                return Result.error(500, result.getMessage(), result);
            }

        } catch (Exception e) {
            log.error("获取当前用户云电脑参数失败", e);
            return Result.error("获取云电脑参数失败：" + e.getMessage());
        }
    }

    /**
     * 获取云电脑登录令牌
     *
     * @param endUserId 终端用户ID
     * @param password 用户密码
     * @return 云电脑登录数据
     */
//    @PostMapping("/login-token")
//    @Log(title = "获取云电脑登录令牌", businessType = BusinessType.OTHER)
//    public Result getLoginToken(@RequestParam("endUserId") String endUserId,
//                             @RequestParam("password") String password) {
//        try {
//            log.info("收到获取云电脑登录令牌请求，终端用户ID：{}", endUserId);
//
//            com.deepreach.web.domain.dto.CloudComputerData result = cloudComputerService.getLoginToken(endUserId, password);
//
//            if (result.getSuccess()) {
//                return Result.success("获取登录令牌成功", result);
//            } else {
//                return Result.error(500, result.getMessage(), result);
//            }
//
//        } catch (Exception e) {
//            log.error("获取云电脑登录令牌失败：终端用户ID={}", endUserId, e);
//            return Result.error("获取云电脑登录令牌失败：" + e.getMessage());
//        }
//    }

    /**
     * 获取当前登录用户ID
     *
     * 注意：这里需要根据实际的安全框架实现
     * 示例中使用占位符，实际使用时需要替换为正确的实现
     */
    private Long getCurrentUserId() {
        // TODO: 根据实际的安全框架实现获取当前用户ID
        // 例如：SecurityUtils.getCurrentLoginUser().getUserId()

        // 临时返回null，需要根据实际项目实现
        return null;
    }
}