package com.deepreach.common.core.controller;

import com.deepreach.common.core.domain.entity.SysDept;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.annotation.Log;
import com.deepreach.common.enums.BusinessType;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理Controller
 *
 * 基于部门类型的部门管理RESTful API控制器，负责：
 * 1. 部门基本信息管理API
 * 2. 部门树形结构管理API
 * 3. 部门层级关系管理API
 * 4. 基于部门类型的业务逻辑API
 * 5. 部门权限控制和统计API
 *
 * 设计理念：
 * - 部门类型决定权限：不同类型部门有不同的创建权限
 * - 树形结构优先：基于ancestors字段构建层级关系
 * - 业务逻辑分离：部门类型相关的业务逻辑在Service层处理
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService deptService;

    // ==================== 查询接口 ====================

    /**
     * 获取部门列表
     *
     * 支持多条件查询，返回平铺的部门列表
     *
     * @param dept 查询条件对象
     * @return 部门列表
     */
    @GetMapping("/list")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public TableDataInfo list(SysDept dept) {
        try {
            startPage();
            List<SysDept> list = deptService.selectDeptList(dept);
            return getDataTable(list);
        } catch (Exception e) {
            log.error("查询部门列表失败", e);
            return TableDataInfo.error("查询部门列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门树形结构
     *
     * 查询当前用户作为负责人的部门树形结构
     *
     * @param dept 查询条件对象
     * @return 部门树形结构列表
     */
    @GetMapping("/tree")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result tree(SysDept dept) {
        try {
            // 获取当前用户ID
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 查询当前用户作为负责人的部门树形结构
            List<SysDept> depts = deptService.selectManagedDeptTreeByUserId(currentUserId);
            return Result.success(depts);
        } catch (Exception e) {
            log.error("查询部门树形结构失败", e);
            return Result.error("查询部门树形结构失败：" + e.getMessage());
        }
    }

    /**
     * 根据部门ID获取详细信息
     *
     * @param deptId 部门ID
     * @return 部门详细信息
     */
    @GetMapping("/{deptId}")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result getInfo(@PathVariable("deptId") Long deptId) {
        try {
            // 检查数据权限
            if (!hasDeptDataPermission(deptId)) {
                return Result.error("无权限访问该部门信息");
            }

            SysDept dept = deptService.selectDeptById(deptId);
            if (dept == null) {
                return Result.error("部门不存在");
            }

            return Result.success(dept);
        } catch (Exception e) {
            log.error("获取部门信息失败：部门ID={}", deptId, e);
            return Result.error("获取部门信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据父部门ID获取子部门列表
     *
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    @GetMapping("/children/{parentId}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getChildren(@PathVariable Long parentId) {
        try {
            List<SysDept> children = deptService.selectChildrenByParentId(parentId);
            return Result.success(children);
        } catch (Exception e) {
            log.error("查询子部门失败：父部门ID={}", parentId, e);
            return Result.error("查询子部门失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门完整路径
     *
     * @param deptId 部门ID
     * @return 完整路径
     */
    @GetMapping("/{deptId}/path")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result getDeptPath(@PathVariable Long deptId) {
        try {
            String fullPath = deptService.getDeptFullPath(deptId);
            return Result.success(fullPath);
        } catch (Exception e) {
            log.error("获取部门路径失败：部门ID={}", deptId, e);
            return Result.error("获取部门路径失败：" + e.getMessage());
        }
    }

    // ==================== 创建接口 ====================

    /**
     * 创建新部门
     *
     * @param dept 部门对象
     * @return 创建结果
     */
    @PostMapping
    // @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody SysDept dept) {
        try {
            // 设置创建者信息
            dept.setCreateBy(SecurityUtils.getCurrentUsername());

            SysDept createdDept = deptService.insertDept(dept);
            return Result.success("创建部门成功", createdDept);
        } catch (Exception e) {
            log.error("创建部门失败：部门名称={}", dept.getDeptName(), e);
            return Result.error("创建部门失败：" + e.getMessage());
        }
    }

//    /**
//     * 创建代理部门
//     *
//     * @param dept 代理部门对象
//     * @param parentDeptId 父部门ID
//     * @return 创建结果
//     */
//    @PostMapping("/agent")
//    // @PreAuthorize("@ss.hasPermi('system:dept:add')")
//    @Log(title = "创建代理部门", businessType = BusinessType.INSERT)
//    public Result createAgent(@Validated @RequestBody SysDept dept, @RequestParam Long parentDeptId) {
//        try {
//            // 设置创建者信息
//            dept.setCreateBy(SecurityUtils.getCurrentUsername());
//
//            SysDept createdDept = deptService.createAgentDept(dept, parentDeptId);
//            return Result.success("创建代理部门成功", createdDept);
//        } catch (Exception e) {
//            log.error("创建代理部门失败：部门名称={}", dept.getDeptName(), e);
//            return Result.error("创建代理部门失败：" + e.getMessage());
//        }
//    }

//    /**
//     * 创建买家总账户部门
//     *
//     * @param dept 买家总账户部门对象
//     * @param parentAgentDeptId 代理部门ID
//     * @return 创建结果
//     */
//    @PostMapping("/buyer-main")
//    // @PreAuthorize("@ss.hasPermi('system:dept:add')")
//    @Log(title = "创建买家总账户", businessType = BusinessType.INSERT)
//    public Result createBuyerMain(@Validated @RequestBody SysDept dept, @RequestParam Long parentAgentDeptId) {
//        try {
//            // 设置创建者信息
//            dept.setCreateBy(SecurityUtils.getCurrentUsername());
//
//            SysDept createdDept = deptService.createBuyerMainAccountDept(dept, parentAgentDeptId);
//            return Result.success("创建买家总账户成功", createdDept);
//        } catch (Exception e) {
//            log.error("创建买家总账户失败：部门名称={}", dept.getDeptName(), e);
//            return Result.error("创建买家总账户失败：" + e.getMessage());
//        }
//    }

//    /**
//     * 创建买家子账户部门
//     *
//     * @param dept 买家子账户部门对象
//     * @param parentBuyerDeptId 买家总账户部门ID
//     * @return 创建结果
//     */
//    @PostMapping("/buyer-sub")
//    // @PreAuthorize("@ss.hasPermi('system:dept:add')")
//    @Log(title = "创建买家子账户", businessType = BusinessType.INSERT)
//    public Result createBuyerSub(@Validated @RequestBody SysDept dept, @RequestParam Long parentBuyerDeptId) {
//        try {
//            // 设置创建者信息
//            dept.setCreateBy(SecurityUtils.getCurrentUsername());
//
//            SysDept createdDept = deptService.createBuyerSubAccountDept(dept, parentBuyerDeptId);
//            return Result.success("创建买家子账户成功", createdDept);
//        } catch (Exception e) {
//            log.error("创建买家子账户失败：部门名称={}", dept.getDeptName(), e);
//            return Result.error("创建买家子账户失败：" + e.getMessage());
//        }
//    }

    // ==================== 更新接口 ====================

    /**
     * 更新部门信息
     *
     * @param dept 部门对象
     * @return 更新结果
     */
    @PutMapping
    // @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    public Result edit(@Validated @RequestBody SysDept dept) {
        try {
            // 检查数据权限
            if (!hasDeptDataPermission(dept.getDeptId())) {
                return Result.error("无权限修改该部门信息");
            }

            // 设置更新者信息
            dept.setUpdateBy(SecurityUtils.getCurrentUsername());

            boolean success = deptService.updateDept(dept);
            if (success) {
                return Result.success("更新部门信息成功");
            } else {
                return Result.error("更新部门信息失败");
            }
        } catch (Exception e) {
            log.error("更新部门信息失败：部门ID={}", dept.getDeptId(), e);
            return Result.error("更新部门信息失败：" + e.getMessage());
        }
    }

//    /**
//     * 移动部门
//     *
//     * 将部门移动到新的父部门下
//     *
//     * @param deptId 部门ID
//     * @param newParentId 新父部门ID
//     * @return 移动结果
//     */
//    @PutMapping("/{deptId}/move")
//    // @PreAuthorize("@ss.hasPermi('system:dept:edit')")
//    @Log(title = "移动部门", businessType = BusinessType.UPDATE)
//    public Result moveDept(@PathVariable Long deptId, @RequestParam Long newParentId) {
//        try {
//            // 检查数据权限
//            if (!hasDeptDataPermission(deptId)) {
//                return Result.error("无权限移动该部门");
//            }
//
//            boolean success = deptService.moveDept(deptId, newParentId);
//            if (success) {
//                return Result.success("移动部门成功");
//            } else {
//                return Result.error("移动部门失败");
//            }
//        } catch (Exception e) {
//            log.error("移动部门失败：部门ID={}, 新父部门ID={}", deptId, newParentId, e);
//            return Result.error("移动部门失败：" + e.getMessage());
//        }
//    }

    // ==================== 删除接口 ====================

    /**
     * 删除部门
     *
     * @param deptId 部门ID
     * @return 删除结果
     */
    @DeleteMapping("/{deptId}")
    // @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    public Result remove(@PathVariable("deptId") Long deptId) {
        try {
            // 检查数据权限
            if (!hasDeptDataPermission(deptId)) {
                return Result.error("无权限删除该部门");
            }

            boolean success = deptService.deleteDeptById(deptId);
            if (success) {
                return Result.success("删除部门成功");
            } else {
                return Result.error("删除部门失败");
            }
        } catch (Exception e) {
            log.error("删除部门失败：部门ID={}", deptId, e);
            return Result.error("删除部门失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除部门
     *
     * @param deptIds 部门ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    // @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "批量删除部门", businessType = BusinessType.DELETE)
    public Result removeBatch(@RequestBody List<Long> deptIds) {
        try {
            // 过滤有权限删除的部门
            List<Long> validDeptIds = deptIds.stream()
                    .filter(this::hasDeptDataPermission)
                    .collect(java.util.stream.Collectors.toList());

            if (validDeptIds.isEmpty()) {
                return Result.error("没有可删除的部门");
            }

            boolean success = deptService.deleteDeptByIds(validDeptIds);
            if (success) {
                return Result.success("批量删除部门成功，删除数量：" + validDeptIds.size());
            } else {
                return Result.error("批量删除部门失败");
            }
        } catch (Exception e) {
            log.error("批量删除部门失败：部门IDs={}", deptIds, e);
            return Result.error("批量删除部门失败：" + e.getMessage());
        }
    }

    // ==================== 状态管理接口 ====================

    /**
     * 更新部门状态
     *
     * @param deptId 部门ID
     * @param status 状态（0正常 1停用）
     * @return 更新结果
     */
    @PutMapping("/{deptId}/status")
    // @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "部门状态", businessType = BusinessType.UPDATE)
    public Result updateStatus(@PathVariable Long deptId, @RequestParam String status) {
        try {
            // 检查数据权限
            if (!hasDeptDataPermission(deptId)) {
                return Result.error("无权限修改该部门状态");
            }

            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            dept.setStatus(status);
            dept.setUpdateBy(SecurityUtils.getCurrentUsername());

            boolean success = deptService.updateDept(dept);
            if (success) {
                String statusText = "0".equals(status) ? "启用" : "停用";
                return Result.success(statusText + "部门成功");
            } else {
                return Result.error("更新部门状态失败");
            }
        } catch (Exception e) {
            log.error("更新部门状态失败：部门ID={}, 状态={}", deptId, status, e);
            return Result.error("更新部门状态失败：" + e.getMessage());
        }
    }

    // ==================== 查询验证接口 ====================

    /**
     * 检查部门名称是否唯一
     *
     * @param deptName 部门名称
     * @param parentId 父部门ID
     * @param deptId 排除的部门ID（用于更新验证）
     * @return 检查结果
     */
    @GetMapping("/check-name-unique")
    public Result checkDeptNameUnique(@RequestParam String deptName,
                                     @RequestParam Long parentId,
                                     @RequestParam(required = false) Long deptId) {
        try {
            int count = deptService.checkDeptNameUnique(deptName, parentId, deptId);
            boolean isUnique = count == 0;
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查部门名称唯一性失败：部门名称={}", deptName, e);
            return Result.error("检查部门名称唯一性失败：" + e.getMessage());
        }
    }

    /**
     * 检查是否存在子部门
     *
     * @param deptId 部门ID
     * @return 检查结果
     */
    @GetMapping("/{deptId}/has-children")
    public Result hasChildren(@PathVariable Long deptId) {
        try {
            int count = deptService.countChildrenByDeptId(deptId);
            boolean hasChildren = count > 0;
            return Result.success(hasChildren);
        } catch (Exception e) {
            log.error("检查子部门失败：部门ID={}", deptId, e);
            return Result.error("检查子部门失败：" + e.getMessage());
        }
    }

    /**
     * 检查部门下是否存在用户
     *
     * @param deptId 部门ID
     * @return 检查结果
     */
    @GetMapping("/{deptId}/has-users")
    public Result hasUsers(@PathVariable Long deptId) {
        try {
            int count = deptService.countUsersByDeptId(deptId);
            boolean hasUsers = count > 0;
            return Result.success(hasUsers);
        } catch (Exception e) {
            log.error("检查部门用户失败：部门ID={}", deptId, e);
            return Result.error("检查部门用户失败：" + e.getMessage());
        }
    }

    // ==================== 统计接口 ====================

    /**
     * 获取部门统计信息
     *
     * @param deptId 部门ID
     * @return 统计信息
     */
    @GetMapping("/{deptId}/statistics")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result getStatistics(@PathVariable Long deptId) {
        try {
            // 检查数据权限
            if (!hasDeptDataPermission(deptId)) {
                return Result.error("无权限查看该部门统计信息");
            }

            Map<String, Object> statistics = deptService.getDeptStatistics(deptId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取部门统计信息失败：部门ID={}", deptId, e);
            return Result.error("获取部门统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门类型统计信息
     *
     * @return 部门类型统计信息
     */
    @GetMapping("/statistics/dept-types")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result getDeptTypeStatistics() {
        try {
            Map<String, Object> statistics = deptService.getDeptTypeStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取部门类型统计信息失败", e);
            return Result.error("获取部门类型统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取代理层级统计信息
     *
     * @return 代理层级统计信息
     */
    @GetMapping("/statistics/agent-levels")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result getAgentLevelStatistics() {
        try {
            Map<String, Object> statistics = deptService.getAgentLevelStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取代理层级统计信息失败", e);
            return Result.error("获取代理层级统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 基于部门类型的查询接口 ====================

    /**
     * 根据部门类型查询部门列表
     *
     * @param deptType 部门类型（1系统 2代理 3买家总账户 4买家子账户）
     * @return 指定类型的部门列表
     */
    @GetMapping("/by-type/{deptType}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getDeptsByType(@PathVariable String deptType) {
        try {
            List<SysDept> depts = deptService.selectDeptsByDeptType(deptType);
            return Result.success(depts);
        } catch (Exception e) {
            log.error("根据类型查询部门失败：部门类型={}", deptType, e);
            return Result.error("根据类型查询部门失败：" + e.getMessage());
        }
    }

    /**
     * 根据部门类型和层级查询部门列表
     *
     * @param deptType 部门类型
     * @param level 层级
     * @return 指定类型和层级的部门列表
     */
    @GetMapping("/by-type/{deptType}/level/{level}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getDeptsByTypeAndLevel(@PathVariable String deptType, @PathVariable Integer level) {
        try {
            List<SysDept> depts = deptService.selectDeptsByDeptTypeAndLevel(deptType, level);
            return Result.success(depts);
        } catch (Exception e) {
            log.error("根据类型和层级查询部门失败：部门类型={}, 层级={}", deptType, level, e);
            return Result.error("根据类型和层级查询部门失败：" + e.getMessage());
        }
    }

    /**
     * 查询代理层级结构
     *
     * @return 代理层级结构列表
     */
    @GetMapping("/agent-hierarchy")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getAgentHierarchy() {
        try {
            List<SysDept> hierarchy = deptService.selectAgentHierarchy();
            return Result.success(hierarchy);
        } catch (Exception e) {
            log.error("查询代理层级结构失败", e);
            return Result.error("查询代理层级结构失败：" + e.getMessage());
        }
    }

    /**
     * 根据层级查询代理部门
     *
     * @param level 代理层级（1-3级）
     * @return 指定层级的代理部门列表
     */
    @GetMapping("/agent/level/{level}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getAgentsByLevel(@PathVariable Integer level) {
        try {
            List<SysDept> agents = deptService.selectAgentsByLevel(level);
            return Result.success(agents);
        } catch (Exception e) {
            log.error("根据层级查询代理部门失败：层级={}", level, e);
            return Result.error("根据层级查询代理部门失败：" + e.getMessage());
        }
    }

    /**
     * 查询指定代理部门的所有下级代理
     *
     * @param deptId 部门ID
     * @return 所有下级代理部门列表
     */
    @GetMapping("/agent/{deptId}/children")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getAgentChildren(@PathVariable Long deptId) {
        try {
            List<SysDept> children = deptService.selectChildAgentsRecursive(deptId);
            return Result.success(children);
        } catch (Exception e) {
            log.error("查询代理下级部门失败：部门ID={}", deptId, e);
            return Result.error("查询代理下级部门失败：" + e.getMessage());
        }
    }

    /**
     * 根据父部门ID查询买家总账户部门
     *
     * @param parentId 父部门ID（代理部门ID）
     * @return 买家总账户部门列表
     */
    @GetMapping("/buyer-main/by-parent/{parentId}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getBuyerMainByParent(@PathVariable Long parentId) {
        try {
            List<SysDept> buyerMains = deptService.selectBuyerMainAccountsByParentId(parentId);
            return Result.success(buyerMains);
        } catch (Exception e) {
            log.error("查询买家总账户失败：父部门ID={}", parentId, e);
            return Result.error("查询买家总账户失败：" + e.getMessage());
        }
    }

    /**
     * 根据买家总账户部门查询子账户部门
     *
     * @param parentBuyerDeptId 买家总账户部门ID
     * @return 买家子账户部门列表
     */
    @GetMapping("/buyer-sub/by-parent/{parentBuyerDeptId}")
    // @PreAuthorize("@ss.hasPermi('system:dept:list')")
    public Result getBuyerSubByParent(@PathVariable Long parentBuyerDeptId) {
        try {
            List<SysDept> buyerSubs = deptService.selectBuyerSubAccountsByParentId(parentBuyerDeptId);
            return Result.success(buyerSubs);
        } catch (Exception e) {
            log.error("查询买家子账户失败：父买家部门ID={}", parentBuyerDeptId, e);
            return Result.error("查询买家子账户失败：" + e.getMessage());
        }
    }

    // ==================== 权限检查接口 ====================

    /**
     * 检查是否可以创建下级代理
     *
     * @param deptId 当前部门ID
     * @return 检查结果
     */
    @GetMapping("/{deptId}/can-create-child-agent")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result canCreateChildAgent(@PathVariable Long deptId) {
        try {
            boolean canCreate = deptService.canCreateChildAgent(deptId);
            return Result.success(canCreate);
        } catch (Exception e) {
            log.error("检查创建下级代理权限失败：部门ID={}", deptId, e);
            return Result.error("检查创建下级代理权限失败：" + e.getMessage());
        }
    }

    /**
     * 检查是否可以创建买家总账户
     *
     * @param deptId 当前部门ID
     * @return 检查结果
     */
    @GetMapping("/{deptId}/can-create-buyer-account")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result canCreateBuyerAccount(@PathVariable Long deptId) {
        try {
            boolean canCreate = deptService.canCreateBuyerAccount(deptId);
            return Result.success(canCreate);
        } catch (Exception e) {
            log.error("检查创建买家总账户权限失败：部门ID={}", deptId, e);
            return Result.error("检查创建买家总账户权限失败：" + e.getMessage());
        }
    }

    /**
     * 检查是否可以创建买家子账户
     *
     * @param deptId 当前部门ID
     * @return 检查结果
     */
    @GetMapping("/{deptId}/can-create-sub-account")
    // @PreAuthorize("@ss.hasPermi('system:dept:query')")
    public Result canCreateSubAccount(@PathVariable Long deptId) {
        try {
            boolean canCreate = deptService.canCreateSubAccount(deptId);
            return Result.success(canCreate);
        } catch (Exception e) {
            log.error("检查创建买家子账户权限失败：部门ID={}", deptId, e);
            return Result.error("检查创建买家子账户权限失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查部门数据权限
     *
     * @param targetDeptId 目标部门ID
     * @return 是否有权限
     */
    private boolean hasDeptDataPermission(Long targetDeptId) {
        try {
            return deptService.hasDeptDataPermission(targetDeptId);
        } catch (Exception e) {
            log.error("检查部门数据权限失败：部门ID={}", targetDeptId, e);
            return false;
        }
    }

    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID，获取失败返回null
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

    /**
     * 启动分页
     */
    protected void startPage() {
        // 简单的分页实现，实际项目中应该使用PageHelper等分页插件
        // 这里只是占位实现
    }

    /**
     * 构建分页数据表格
     */
    protected TableDataInfo getDataTable(List<?> list) {
        return new TableDataInfo(list, list.size());
    }
}