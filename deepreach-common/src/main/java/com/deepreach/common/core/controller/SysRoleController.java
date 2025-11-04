package com.deepreach.common.core.controller;

import com.deepreach.common.core.domain.entity.SysRole;
import com.deepreach.common.core.service.SysRoleService;
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
import java.util.Set;

/**
 * 角色管理Controller
 *
 * 基于角色身份的管理 RESTful API 控制器，负责：
 * 1. 角色基本信息管理API
 * 2. 角色权限关联管理API
 * 3. 角色数据权限管理API
 * 4. 角色身份分类接口
 * 5. 角色分配和权限验证API
 *
 * 设计理念：
 * - 角色身份绑定逻辑：角色只能分配给合规身份的用户
 * - 简化权限管理：通过角色身份自动确定权限范围
 * - 标准RBAC模式：角色作为权限载体，控制用户访问权限
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService roleService;

    // ==================== 查询接口 ====================

    /**
     * 获取角色列表
     *
     * 支持多条件查询和分页
     *
     * @param role 查询条件对象
     * @return 分页角色列表
     */
    @GetMapping("/list")
    // @PreAuthorize("@ss.hasPermi('system:role:list')")
    public TableDataInfo list(SysRole role) {
        try {
            startPage();
            List<SysRole> list = roleService.selectRoleList(role);
            return getDataTable(list);
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            return TableDataInfo.error("查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据角色ID获取详细信息
     *
     * 获取角色的完整信息，包括权限和部门关联
     *
     * @param roleId 角色ID
     * @return 角色详细信息
     */
    @GetMapping("/{roleId}")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getInfo(@PathVariable("roleId") Long roleId) {
        try {
            SysRole role = roleService.selectRoleById(roleId);
            if (role == null) {
                return Result.error("角色不存在");
            }

            // 获取角色统计信息
            Map<String, Object> statistics = roleService.getRoleStatistics(roleId);

            // 构建返回数据
            Map<String, Object> data = Map.of(
                "role", role,
                "statistics", statistics
            );

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取角色信息失败：角色ID={}", roleId, e);
            return Result.error("获取角色信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取角色列表
     *
     * 查询指定用户拥有的所有角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/by-user/{userId}")
    // @PreAuthorize("@ss.hasPermi('system:role:list')")
    public Result getRolesByUserId(@PathVariable Long userId) {
        try {
            List<SysRole> roles = roleService.selectRolesByUserId(userId);
            return Result.success(roles);
        } catch (Exception e) {
            log.error("查询用户角色失败：用户ID={}", userId, e);
            return Result.error("查询用户角色失败：" + e.getMessage());
        }
    }

    /**
     * 根据身份别名获取角色列表（兼容旧部门类型编码）。
     *
     * @param identity 身份别名或旧部门类型编码
     * @return 角色列表
     */
    @GetMapping("/identity/{identity}")
    // @PreAuthorize("@ss.hasPermi('system:role:list')")
    public Result getRolesByIdentity(@PathVariable String identity) {
        try {
            List<SysRole> roles = roleService.selectRolesByIdentity(identity);
            return Result.success(roles);
        } catch (Exception e) {
            log.error("根据身份查询角色失败：身份={}", identity, e);
            return Result.error("根据身份查询角色失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定身份的默认角色（兼容旧部门类型编码）。
     */
    @GetMapping("/default/{identity}")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getDefaultRole(@PathVariable String identity) {
        try {
            SysRole role = roleService.getDefaultRoleByIdentity(identity);
            if (role == null) {
                return Result.error("该身份没有默认角色");
            }
            return Result.success(role);
        } catch (Exception e) {
            log.error("获取默认角色失败：身份={}", identity, e);
            return Result.error("获取默认角色失败：" + e.getMessage());
        }
    }

    // ==================== 创建接口 ====================

    /**
     * 创建新角色
     *
     * @param role 角色对象
     * @return 创建结果
     */
    @PostMapping
    // @PreAuthorize("@ss.hasPermi('system:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody SysRole role) {
        try {
            SysRole createdRole = roleService.insertRole(role);
            return Result.success("创建角色成功", createdRole);
        } catch (Exception e) {
            log.error("创建角色失败：角色名称={}", role.getRoleName(), e);
            return Result.error("创建角色失败：" + e.getMessage());
        }
    }

    /**
     * 初始化默认角色
     *
     * 创建系统默认角色数据
     *
     * @return 初始化结果
     */
    @PostMapping("/init-default")
    // @PreAuthorize("@ss.hasPermi('system:role:add')")
    @Log(title = "初始化默认角色", businessType = BusinessType.INSERT)
    public Result initDefaultRoles() {
        try {
            boolean success = roleService.initDefaultRoles();
            if (success) {
                return Result.success("初始化默认角色成功");
            } else {
                return Result.error("初始化默认角色失败");
            }
        } catch (Exception e) {
            log.error("初始化默认角色失败", e);
            return Result.error("初始化默认角色失败：" + e.getMessage());
        }
    }

    // ==================== 更新接口 ====================

    /**
     * 更新角色信息
     *
     * @param role 角色对象
     * @return 更新结果
     */
    @PutMapping
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public Result edit(@Validated @RequestBody SysRole role) {
        try {
            boolean success = roleService.updateRole(role);
            if (success) {
                return Result.success("更新角色信息成功");
            } else {
                return Result.error("更新角色信息失败");
            }
        } catch (Exception e) {
            log.error("更新角色信息失败：角色ID={}", role.getRoleId(), e);
            return Result.error("更新角色信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新角色状态
     *
     * 启用或停用角色
     *
     * @param roleId 角色ID
     * @param status 状态（0正常 1停用）
     * @return 更新结果
     */
    @PutMapping("/{roleId}/status")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色状态", businessType = BusinessType.UPDATE)
    public Result updateStatus(@PathVariable("roleId") Long roleId, @RequestParam String status) {
        try {
            boolean success = roleService.updateRoleStatus(roleId, status);
            if (success) {
                String statusText = "0".equals(status) ? "启用" : "停用";
                return Result.success(statusText + "角色成功");
            } else {
                return Result.error("更新角色状态失败");
            }
        } catch (Exception e) {
            log.error("更新角色状态失败：角色ID={}, 状态={}", roleId, status, e);
            return Result.error("更新角色状态失败：" + e.getMessage());
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return 删除结果
     */
    @DeleteMapping("/{roleId}")
    // @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    public Result remove(@PathVariable("roleId") Long roleId) {
        try {
            boolean success = roleService.deleteRoleById(roleId);
            if (success) {
                return Result.success("删除角色成功");
            } else {
                return Result.error("删除角色失败");
            }
        } catch (Exception e) {
            log.error("删除角色失败：角色ID={}", roleId, e);
            return Result.error("删除角色失败：" + e.getMessage());
        }
    }
                            /// 只测到这里过
    /**
     * 批量删除角色
     *
     * @param roleIds 角色ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    // @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @Log(title = "批量删除角色", businessType = BusinessType.DELETE)
    public Result removeBatch(@RequestBody List<Long> roleIds) {
        try {
            boolean success = roleService.deleteRoleByIds(roleIds);
            if (success) {
                return Result.success("批量删除角色成功，删除数量：" + roleIds.size());
            } else {
                return Result.error("批量删除角色失败");
            }
        } catch (Exception e) {
            log.error("批量删除角色失败：角色IDs={}", roleIds, e);
            return Result.error("批量删除角色失败：" + e.getMessage());
        }
    }

    // ==================== 用户角色分配接口 ====================

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 分配结果
     */
    @PutMapping("/assign-user/{userId}")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "分配用户角色", businessType = BusinessType.GRANT)
    public Result assignUserRoles(@PathVariable("userId") Long userId, @RequestBody List<Long> roleIds) {
        try {
            boolean success = roleService.assignUserRoles(userId, roleIds);
            if (success) {
                return Result.success("分配用户角色成功");
            } else {
                return Result.error("分配用户角色失败");
            }
        } catch (Exception e) {
            log.error("分配用户角色失败：用户ID={}, 角色IDs={}", userId, roleIds, e);
            return Result.error("分配用户角色失败：" + e.getMessage());
        }
    }

    /**
     * 取消用户角色分配
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 取消结果
     */
    @PutMapping("/cancel-user/{userId}")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "取消用户角色", businessType = BusinessType.GRANT)
    public Result cancelUserRoles(@PathVariable("userId") Long userId, @RequestBody List<Long> roleIds) {
        try {
            boolean success = roleService.cancelUserRoles(userId, roleIds);
            if (success) {
                return Result.success("取消用户角色成功");
            } else {
                return Result.error("取消用户角色失败");
            }
        } catch (Exception e) {
            log.error("取消用户角色失败：用户ID={}, 角色IDs={}", userId, roleIds, e);
            return Result.error("取消用户角色失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @GetMapping("/user/{userId}/role-ids")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getUserRoleIds(@PathVariable Long userId) {
        try {
            List<Long> roleIds = roleService.getUserRoleIds(userId);
            return Result.success(roleIds);
        } catch (Exception e) {
            log.error("获取用户角色ID失败：用户ID={}", userId, e);
            return Result.error("获取用户角色ID失败：" + e.getMessage());
        }
    }

    // ==================== 角色菜单权限接口 ====================

    /**
     * 为角色分配菜单权限
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 分配结果
     */
    @PutMapping("/{roleId}/menus")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "分配角色菜单权限", businessType = BusinessType.GRANT)
    public Result assignRoleMenus(@PathVariable("roleId") Long roleId, @RequestBody List<Long> menuIds) {
        try {
            boolean success = roleService.assignRoleMenus(roleId, menuIds);
            if (success) {
                return Result.success("分配角色菜单权限成功");
            } else {
                return Result.error("分配角色菜单权限失败");
            }
        } catch (Exception e) {
            log.error("分配角色菜单权限失败：角色ID={}, 菜单IDs={}", roleId, menuIds, e);
            return Result.error("分配角色菜单权限失败：" + e.getMessage());
        }
    }

    /**
     * 取消角色菜单权限分配
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 取消结果
     */
    @DeleteMapping("/{roleId}/menus")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "取消角色菜单权限", businessType = BusinessType.GRANT)
    public Result cancelRoleMenus(@PathVariable("roleId") Long roleId, @RequestBody List<Long> menuIds) {
        try {
            boolean success = roleService.cancelRoleMenus(roleId, menuIds);
            if (success) {
                return Result.success("取消角色菜单权限成功");
            } else {
                return Result.error("取消角色菜单权限失败");
            }
        } catch (Exception e) {
            log.error("取消角色菜单权限失败：角色ID={}, 菜单IDs={}", roleId, menuIds, e);
            return Result.error("取消角色菜单权限失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @GetMapping("/{roleId}/menu-ids")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getRoleMenuIds(@PathVariable("roleId") Long roleId) {
        try {
            List<Long> menuIds = roleService.getRoleMenuIds(roleId);
            return Result.success(menuIds);
        } catch (Exception e) {
            log.error("获取角色菜单ID失败：角色ID={}", roleId, e);
            return Result.error("获取角色菜单ID失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色菜单权限标识列表
     *
     * @param roleId 角色ID
     * @return 菜单权限标识列表
     */
    @GetMapping("/{roleId}/permissions")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getRolePermissions(@PathVariable("roleId") Long roleId) {
        try {
            Set<String> permissions = roleService.getRolePermissions(roleId);
            return Result.success(permissions);
        } catch (Exception e) {
            log.error("获取角色权限失败：角色ID={}", roleId, e);
            return Result.error("获取角色权限失败：" + e.getMessage());
        }
    }

    // ==================== 角色数据权限接口 ====================

    /**
     * 为角色分配数据权限（部门）
     *
     * @param roleId 角色ID
     * @param deptIds 部门ID列表
     * @return 分配结果
     */
    @PutMapping("/{roleId}/depts")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "分配角色数据权限", businessType = BusinessType.GRANT)
    public Result assignRoleDepts(@PathVariable("roleId") Long roleId, @RequestBody List<Long> deptIds) {
        try {
            boolean success = roleService.assignRoleDepts(roleId, deptIds);
            if (success) {
                return Result.success("分配角色数据权限成功");
            } else {
                return Result.error("分配角色数据权限失败");
            }
        } catch (Exception e) {
            log.error("分配角色数据权限失败：角色ID={}, 部门IDs={}", roleId, deptIds, e);
            return Result.error("分配角色数据权限失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    @GetMapping("/{roleId}/dept-ids")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getRoleDeptIds(@PathVariable("roleId") Long roleId) {
        try {
            List<Long> deptIds = roleService.getRoleDeptIds(roleId);
            return Result.success(deptIds);
        } catch (Exception e) {
            log.error("获取角色部门ID失败：角色ID={}", roleId, e);
            return Result.error("获取角色部门ID失败：" + e.getMessage());
        }
    }

    // ==================== 查询验证接口 ====================

    /**
     * 检查角色名称是否唯一
     *
     * @param roleName 角色名称
     * @param roleId 排除的角色ID（用于更新验证）
     * @return 检查结果
     */
    @GetMapping("/check-name-unique")
    public Result checkRoleNameUnique(@RequestParam String roleName,
                                     @RequestParam(required = false) Long roleId) {
        try {
            int count = roleService.checkRoleNameUnique(roleName, roleId);
            boolean isUnique = count == 0;
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查角色名称唯一性失败：角色名称={}", roleName, e);
            return Result.error("检查角色名称唯一性失败：" + e.getMessage());
        }
    }

    /**
     * 检查角色标识是否唯一
     *
     * @param roleKey 角色标识
     * @param roleId 排除的角色ID（用于更新验证）
     * @return 检查结果
     */
    @GetMapping("/check-key-unique")
    public Result checkRoleKeyUnique(@RequestParam String roleKey,
                                    @RequestParam(required = false) Long roleId) {
        try {
            int count = roleService.checkRoleKeyUnique(roleKey, roleId);
            boolean isUnique = count == 0;
            return Result.success(isUnique);
        } catch (Exception e) {
            log.error("检查角色标识唯一性失败：角色标识={}", roleKey, e);
            return Result.error("检查角色标识唯一性失败：" + e.getMessage());
        }
    }

    /**
     * 检查角色下是否存在用户
     *
     * @param roleId 角色ID
     * @return 检查结果
     */
    @GetMapping("/{roleId}/has-users")
    public Result hasUsers(@PathVariable("roleId") Long roleId) {
        try {
            int count = roleService.countUsersByRoleId(roleId);
            boolean hasUsers = count > 0;
            Map<String, Object> result = Map.of(
                "hasUsers", hasUsers,
                "userCount", count
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查角色用户失败：角色ID={}", roleId, e);
            return Result.error("检查角色用户失败：" + e.getMessage());
        }
    }

    /**
     * 检查角色是否适用于指定身份
     *
     * @param roleId 角色ID
     * @param identity 身份别名
     * @return 检查结果
     */
    @GetMapping("/{roleId}/applicable-to-dept-type/{identity}")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result isApplicableToIdentity(@PathVariable("roleId") Long roleId, @PathVariable("identity") String identity) {
        try {
            boolean applicable = roleService.isRoleApplicableToIdentity(roleId, identity);
            return Result.success(applicable);
        } catch (Exception e) {
            log.error("检查角色部门类型适用性失败：角色ID={}, 部门类型={}", roleId, identity, e);
            return Result.error("检查角色身份适用性失败：" + e.getMessage());
        }
    }

    // ==================== 统计接口 ====================

    /**
     * 获取角色统计信息
     *
     * @param roleId 角色ID
     * @return 统计信息
     */
    @GetMapping("/{roleId}/statistics")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getStatistics(@PathVariable("roleId") Long roleId) {
        try {
            Map<String, Object> statistics = roleService.getRoleStatistics(roleId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取角色统计信息失败：角色ID={}", roleId, e);
            return Result.error("获取角色统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有角色的统计信息
     *
     * @return 角色统计信息
     */
    @GetMapping("/statistics/all")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result getAllStatistics() {
        try {
            Map<String, Object> statistics = roleService.getAllRoleStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取所有角色统计信息失败", e);
            return Result.error("获取所有角色统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 数据同步接口 ====================

    /**
     * 同步角色权限数据
     *
     * @param roleId 角色ID
     * @return 同步结果
     */
    @PutMapping("/{roleId}/sync-permissions")
    // @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "同步角色权限", businessType = BusinessType.UPDATE)
    public Result syncPermissions(@PathVariable("roleId") Long roleId) {
        try {
            boolean success = roleService.syncRolePermissions(roleId);
            if (success) {
                return Result.success("同步角色权限成功");
            } else {
                return Result.error("同步角色权限失败");
            }
        } catch (Exception e) {
            log.error("同步角色权限失败：角色ID={}", roleId, e);
            return Result.error("同步角色权限失败：" + e.getMessage());
        }
    }

    /**
     * 验证角色数据完整性
     *
     * @param roleId 角色ID
     * @return 验证结果
     */
    @GetMapping("/{roleId}/validate")
    // @PreAuthorize("@ss.hasPermi('system:role:query')")
    public Result validateRoleData(@PathVariable("roleId") Long roleId) {
        try {
            Map<String, Object> result = roleService.validateRoleData(roleId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证角色数据失败：角色ID={}", roleId, e);
            return Result.error("验证角色数据失败：" + e.getMessage());
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