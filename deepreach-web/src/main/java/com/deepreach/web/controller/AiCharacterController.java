package com.deepreach.web.controller;

import com.deepreach.web.entity.AiCharacter;
import com.deepreach.web.service.AiCharacterService;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI人设Controller
 *
 * AI人设管理RESTful API控制器，负责：
 * 1. 人设基本信息管理API
 * 2. 人设分类和来源管理API
 * 3. 人设搜索和推荐API
 * 4. 人设统计和分析API
 * 5. 人设导入导出API
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/character")
public class AiCharacterController extends BaseController {

    @Autowired
    private AiCharacterService characterService;

    // ==================== 查询接口 ====================

    /**
     * 获取人设列表
     *
     * 支持多条件查询和分页
     * 自动应用权限过滤
     *
     * @param character 查询条件对象
     * @return 分页人设列表
     */
    @GetMapping("/list")
    public TableDataInfo list(AiCharacter character) {
        try {
            // 获取当前用户ID
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return TableDataInfo.error("用户未登录");
            }

            startPage(); // 启动分页
            List<AiCharacter> list = characterService.selectListWithUserPermission(character, currentUserId);
            return getDataTable(list);
        } catch (Exception e) {
            log.error("查询人设列表失败", e);
            return TableDataInfo.error("查询人设列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据人设ID获取详细信息
     *
     * 获取人设的完整信息
     *
     * @param id 人设ID
     * @return 人设详细信息
     */
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Long id) {
        try {
            // 检查访问权限
            Long currentUserId = getCurrentUserId();
            if (!characterService.hasAccessPermission(id, currentUserId)) {
                return Result.error("无权限访问该人设");
            }

            AiCharacter character = characterService.selectCompleteInfo(id);
            if (character == null) {
                return Result.error("人设不存在");
            }

            return Result.success(character);
        } catch (Exception e) {
            log.error("获取人设信息失败：人设ID={}", id, e);
            return Result.error("获取人设信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的人设列表
     *
     * 查询当前用户创建的所有人设
     *
     * @return 我的人设列表
     */
    @GetMapping("/my")
    public Result getMyCharacters() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            List<AiCharacter> list = characterService.selectByUserId(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取我的人设列表失败", e);
            return Result.error("获取我的人设列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取可访问的人设列表
     *
     * 查询用户可以访问的所有人设（包括系统人设和自建人设）
     *
     * @return 可访问的人设列表
     */
    @GetMapping("/accessible")
    public Result getAccessibleCharacters() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            List<AiCharacter> list = characterService.selectAccessibleCharacters(currentUserId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取可访问人设列表失败", e);
            return Result.error("获取可访问人设列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据类型查询人设
     *
     * @param type 人设类型（emotion/business）
     * @return 人设列表
     */
    @GetMapping("/type/{type}")
    public Result getCharactersByType(@PathVariable String type) {
        try {
            List<AiCharacter> list = characterService.selectByType(type);
            return Result.success(list);
        } catch (Exception e) {
            log.error("按类型查询人设失败：类型={}", type, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取系统人设列表
     *
     * @return 系统人设列表
     */
    @GetMapping("/system")
    public Result getSystemCharacters() {
        try {
            List<AiCharacter> list = characterService.selectSystemCharacters();
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询系统人设失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 搜索人设
     *
     * @param keyword 关键词
     * @param limit 限制数量（可选）
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result searchCharacters(@RequestParam String keyword,
                                  @RequestParam(required = false) Integer limit) {
        try {
            Long currentUserId = getCurrentUserId();
            List<AiCharacter> list = characterService.searchCharacters(keyword, currentUserId, limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("搜索人设失败：关键词={}", keyword, e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取热门人设
     *
     * @param limit 限制数量（可选）
     * @return 热门人设列表
     */
    @GetMapping("/popular")
    public Result getPopularCharacters(@RequestParam(required = false) Integer limit) {
        try {
            List<AiCharacter> list = characterService.selectPopularCharacters(limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取热门人设失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取最新人设
     *
     * @param limit 限制数量（可选）
     * @return 最新人设列表
     */
    @GetMapping("/latest")
    public Result getLatestCharacters(@RequestParam(required = false) Integer limit) {
        try {
            List<AiCharacter> list = characterService.selectLatestCharacters(limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取最新人设失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    // ==================== 创建接口 ====================

    /**
     * 创建人设
     *
     * 用户创建自建人设接口
     *
     * @param character 人设对象
     * @return 创建结果
     */
    @PostMapping
    @Log(title = "人设管理", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody AiCharacter character) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 设置创建者信息
            character.setCreateBy(SecurityUtils.getCurrentUsername());
            character.setUserId(currentUserId);
            character.setIsSystem(false); // 用户创建的都是自建人设

            AiCharacter created = characterService.createUserCharacter(character, currentUserId);
            return Result.success("创建人设成功", created);
        } catch (Exception e) {
            log.error("创建人设失败：名称={}", character.getName(), e);
            return Result.error("创建人设失败：" + e.getMessage());
        }
    }

    /**
     * 复制人设
     *
     * 基于现有人设创建副本
     *
     * @param sourceId 源人设ID
     * @param newName 新人设名称
     * @return 复制结果
     */
    @PostMapping("/copy/{sourceId}")
    @Log(title = "复制人设", businessType = BusinessType.INSERT)
    public Result copyCharacter(@PathVariable Long sourceId, @RequestParam String newName) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            AiCharacter copied = characterService.copyCharacter(sourceId, newName, currentUserId);
            return Result.success("复制人设成功", copied);
        } catch (Exception e) {
            log.error("复制人设失败：源人设ID={}, 新名称={}", sourceId, newName, e);
            return Result.error("复制人设失败：" + e.getMessage());
        }
    }

    // ==================== 更新接口 ====================

    /**
     * 更新人设信息
     *
     * @param character 人设对象
     * @return 更新结果
     */
    @PutMapping
    @Log(title = "人设管理", businessType = BusinessType.UPDATE)
    public Result edit(@Validated @RequestBody AiCharacter character) {
        try {
            // 检查权限
            if (!characterService.hasAccessPermission(character.getId(), getCurrentUserId())) {
                return Result.error("无权限修改该人设");
            }

            // 数据库会自动设置updated_time

            boolean success = characterService.update(character);
            if (success) {
                return Result.success("更新人设信息成功");
            } else {
                return Result.error("更新人设信息失败");
            }
        } catch (Exception e) {
            log.error("更新人设信息失败：人设ID={}", character.getId(), e);
            return Result.error("更新人设信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新人设头像
     *
     * @param id 人设ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    @PutMapping("/{id}/avatar")
    @Log(title = "人设头像", businessType = BusinessType.UPDATE)
    public Result updateAvatar(@PathVariable Long id, @RequestParam String avatarUrl) {
        try {
            // 检查权限
            if (!characterService.hasAccessPermission(id, getCurrentUserId())) {
                return Result.error("无权限修改该人设");
            }

            boolean success = characterService.updateAvatar(id, avatarUrl);
            if (success) {
                return Result.success("更新头像成功");
            } else {
                return Result.error("更新头像失败");
            }
        } catch (Exception e) {
            log.error("更新人设头像失败：人设ID={}", id, e);
            return Result.error("更新头像失败：" + e.getMessage());
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除人设
     *
     * @param id 人设ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Log(title = "人设管理", businessType = BusinessType.DELETE)
    public Result remove(@PathVariable("id") Long id) {
        try {
            // 检查权限
            if (!characterService.canDelete(id, getCurrentUserId())) {
                return Result.error("无权限删除该人设");
            }

            boolean success = characterService.deleteById(id);
            if (success) {
                return Result.success("删除人设成功");
            } else {
                return Result.error("删除人设失败");
            }
        } catch (Exception e) {
            log.error("删除人设失败：人设ID={}", id, e);
            return Result.error("删除人设失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除人设
     *
     * @param ids 人设ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    @Log(title = "人设管理", businessType = BusinessType.DELETE)
    public Result removeBatch(@RequestBody List<Long> ids) {
        try {
            // 过滤有权限删除的人设
            List<Long> validIds = new ArrayList<>();
            Long currentUserId = getCurrentUserId();

            for (Long id : ids) {
                if (characterService.canDelete(id, currentUserId)) {
                    validIds.add(id);
                }
            }

            if (validIds.isEmpty()) {
                return Result.error("没有可删除的人设");
            }

            boolean success = characterService.deleteByIds(validIds);
            if (success) {
                return Result.success("批量删除人设成功，删除数量：" + validIds.size());
            } else {
                return Result.error("批量删除人设失败");
            }
        } catch (Exception e) {
            log.error("批量删除人设失败：人设IDs={}", ids, e);
            return Result.error("批量删除人设失败：" + e.getMessage());
        }
    }

    // ==================== 统计接口 ====================

    /**
     * 获取人设统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result getStatistics() {
        try {
            Map<String, Object> statistics = characterService.getStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取人设统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的人设统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/my/statistics")
    public Result getMyStatistics() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            Map<String, Object> statistics = characterService.getUserStatistics(currentUserId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户人设统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取人设分类统计
     *
     * @param userId 用户ID（可选）
     * @return 分类统计信息
     */
    @GetMapping("/statistics/type")
    public Result getTypeStatistics(@RequestParam(required = false) Long userId) {
        try {
            // 如果没有指定用户ID，使用当前用户ID
            if (userId == null) {
                userId = getCurrentUserId();
            }

            Map<String, Object> statistics = characterService.getTypeStatistics(userId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取人设分类统计失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 验证接口 ====================

    /**
     * 检查人设名称是否唯一
     *
     * @param name 人设名称
     * @param id 排除的人设ID（用于更新验证）
     * @return 验证结果
     */
    @GetMapping("/check-name-unique")
    public Result checkNameUnique(@RequestParam String name, @RequestParam(required = false) Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            boolean isUnique = characterService.checkNameUnique(name, currentUserId, id);
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查人设名称唯一性失败：名称={}", name, e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    // ==================== 导入导出接口 ====================

    /**
     * 导入人设数据
     *
     * @param characters 人设列表
     * @param updateSupport 是否支持更新已存在的人设
     * @return 导入结果
     */
    @PostMapping("/import")
    @Log(title = "人设导入", businessType = BusinessType.IMPORT)
    public Result importCharacters(@RequestBody List<AiCharacter> characters,
                                  @RequestParam(defaultValue = "false") boolean updateSupport) {
        try {
            Map<String, Object> result = characterService.importCharacters(characters, updateSupport);
            return Result.success("导入人设成功", result);
        } catch (Exception e) {
            log.error("导入人设失败", e);
            return Result.error("导入人设失败：" + e.getMessage());
        }
    }

    /**
     * 导出人设数据
     *
     * @param character 查询条件对象
     * @return 导出数据
     */
    @PostMapping("/export")
    @Log(title = "人设导出", businessType = BusinessType.EXPORT)
    public Result exportCharacters(AiCharacter character) {
        try {
            List<AiCharacter> list = characterService.selectList(character);
            String data = characterService.exportCharacters(list);

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("filename", "ai_characters_" + System.currentTimeMillis() + ".json");

            return Result.success("导出人设成功", result);
        } catch (Exception e) {
            log.error("导出人设失败", e);
            return Result.error("导出人设失败：" + e.getMessage());
        }
    }

  }