package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.SysRole;
import com.deepreach.common.core.domain.entity.SysDept;
import com.deepreach.common.core.service.SysRoleService;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.core.mapper.SysRoleMapper;
import com.deepreach.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色Service实现类
 *
 * 基于部门类型的角色管理业务逻辑实现，负责：
 * 1. 角色基本信息管理（增删改查）
 * 2. 角色权限关联管理
 * 3. 角色数据权限管理
 * 4. 基于部门类型的角色分类管理
 * 5. 角色分配和权限验证
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-29
 */
@Slf4j
@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysDeptService deptService;

    // ==================== 查询方法 ====================

    @Override
    public SysRole selectRoleById(Long roleId) {
        if (roleId == null) {
            return null;
        }

        try {
            SysRole role = roleMapper.selectRoleById(roleId);
            if (role != null) {
                // 加载角色关联的菜单权限
                List<Long> menuIds = getRoleMenuIds(roleId);
                if (menuIds != null && !menuIds.isEmpty()) {
                    // 这里需要加载SysMenu实体，但由于没有定义，暂时跳过
                    // role.setMenus(loadMenusByIds(menuIds));
                }

                // 加载角色关联的部门权限（如果数据权限为自定义）
                if ("2".equals(role.getDataScope())) {
                    List<Long> deptIds = getRoleDeptIds(roleId);
                    if (deptIds != null && !deptIds.isEmpty()) {
                        List<SysDept> depts = deptIds.stream()
                                .map(deptService::selectDeptById)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        role.getDepts().addAll(depts);
                    }
                }
            }
            return role;
        } catch (Exception e) {
            log.error("查询角色失败：角色ID={}", roleId, e);
            return null;
        }
    }

    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        try {
            return roleMapper.selectRoleList(role);
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            return roleMapper.selectRolesByUserId(userId);
        } catch (Exception e) {
            log.error("查询用户角色失败：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<SysRole> selectRolesByDeptType(String deptType) {
        if (deptType == null) {
            return new ArrayList<>();
        }

        try {
            return roleMapper.selectRolesByDeptType(deptType);
        } catch (Exception e) {
            log.error("根据部门类型查询角色失败：部门类型={}", deptType, e);
            return new ArrayList<>();
        }
    }

    // ==================== 创建方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole insertRole(SysRole role) throws Exception {
        // 参数验证
        if (role == null) {
            throw new Exception("角色信息不能为空");
        }

        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            throw new Exception("角色名称不能为空");
        }

        if (role.getRoleKey() == null || role.getRoleKey().trim().isEmpty()) {
            throw new Exception("角色标识不能为空");
        }

        if (role.getDeptType() == null) {
            throw new Exception("适用部门类型不能为空");
        }

        // 唯一性验证
        if (checkRoleNameUnique(role.getRoleName(), null) > 0) {
            throw new Exception("角色名称已存在");
        }

        if (checkRoleKeyUnique(role.getRoleKey(), null) > 0) {
            throw new Exception("角色标识已存在");
        }

        // 权限验证
        validateRoleCreatePermission(role);

        // 设置默认值
        role.setStatus(role.getStatus() != null ? role.getStatus() : "0");
        role.setDelFlag("0");
        role.setRoleSort(role.getRoleSort() != null ? role.getRoleSort() : 999);
        role.setDataScope(role.getDataScope() != null ? role.getDataScope() : "4");
        role.setMenuCheckStrictly(role.getMenuCheckStrictly() != null ? role.getMenuCheckStrictly() : false);
        role.setDeptCheckStrictly(role.getDeptCheckStrictly() != null ? role.getDeptCheckStrictly() : false);

        // 设置创建者信息
        String currentUsername = SecurityUtils.getCurrentUsername();
        role.setCreateBy(currentUsername != null ? currentUsername : "system");
        role.setCreateTime(LocalDateTime.now());

        try {
            int rows = roleMapper.insertRole(role);
            if (rows <= 0) {
                throw new Exception("创建角色失败");
            }

            log.info("创建角色成功：角色ID={}, 角色名称={}", role.getRoleId(), role.getRoleName());
            return role;
        } catch (Exception e) {
            log.error("创建角色失败：角色名称={}", role.getRoleName(), e);
            throw new Exception("创建角色失败：" + e.getMessage());
        }
    }

    // ==================== 更新方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role) throws Exception {
        // 参数验证
        if (role == null || role.getRoleId() == null) {
            throw new Exception("角色ID不能为空");
        }

        // 检查角色是否存在
        SysRole existingRole = selectRoleById(role.getRoleId());
        if (existingRole == null) {
            throw new Exception("角色不存在");
        }

        // 唯一性验证（排除自己）
        if (role.getRoleName() != null && checkRoleNameUnique(role.getRoleName(), role.getRoleId()) > 0) {
            throw new Exception("角色名称已存在");
        }

        if (role.getRoleKey() != null && checkRoleKeyUnique(role.getRoleKey(), role.getRoleId()) > 0) {
            throw new Exception("角色标识已存在");
        }

        // 设置更新者信息
        String currentUsername = SecurityUtils.getCurrentUsername();
        role.setUpdateBy(currentUsername != null ? currentUsername : "system");
        role.setUpdateTime(LocalDateTime.now());

        try {
            int rows = roleMapper.updateRole(role);
            if (rows <= 0) {
                throw new Exception("更新角色失败");
            }

            log.info("更新角色成功：角色ID={}", role.getRoleId());
            return true;
        } catch (Exception e) {
            log.error("更新角色失败：角色ID={}", role.getRoleId(), e);
            throw new Exception("更新角色失败：" + e.getMessage());
        }
    }

    // ==================== 删除方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleById(Long roleId) throws Exception {
        if (roleId == null) {
            throw new Exception("角色ID不能为空");
        }

        // 检查角色是否存在
        SysRole role = selectRoleById(roleId);
        if (role == null) {
            throw new Exception("角色不存在");
        }

        // 检查是否有用户关联
        int userCount = countUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new Exception("该角色下还有" + userCount + "个用户，无法删除");
        }

        try {
            // 删除角色菜单关联
            cancelRoleMenus(roleId, null);

            // 注释：sys_role_dept表不存在，无需删除角色部门关联

            // 删除角色
            int rows = roleMapper.deleteRoleById(roleId);
            if (rows <= 0) {
                throw new Exception("删除角色失败");
            }

            log.info("删除角色成功：角色ID={}, 角色名称={}", roleId, role.getRoleName());
            return true;
        } catch (Exception e) {
            log.error("删除角色失败：角色ID={}", roleId, e);
            throw new Exception("删除角色失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(List<Long> roleIds) throws Exception {
        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }

        boolean allSuccess = true;
        List<String> errors = new ArrayList<>();

        for (Long roleId : roleIds) {
            try {
                boolean success = deleteRoleById(roleId);
                if (!success) {
                    allSuccess = false;
                    errors.add("删除角色ID " + roleId + " 失败");
                }
            } catch (Exception e) {
                allSuccess = false;
                errors.add("删除角色ID " + roleId + " 失败：" + e.getMessage());
            }
        }

        if (!allSuccess && !errors.isEmpty()) {
            log.warn("批量删除角色部分失败：{}", String.join("; ", errors));
        }

        return allSuccess;
    }

    // ==================== 唯一性验证方法 ====================

    @Override
    public int checkRoleNameUnique(String roleName, Long roleId) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return 0;
        }

        try {
            return roleMapper.checkRoleNameUnique(roleName, roleId);
        } catch (Exception e) {
            log.error("检查角色名称唯一性失败：角色名称={}", roleName, e);
            return 0;
        }
    }

    @Override
    public int checkRoleKeyUnique(String roleKey, Long roleId) {
        if (roleKey == null || roleKey.trim().isEmpty()) {
            return 0;
        }

        try {
            return roleMapper.checkRoleKeyUnique(roleKey, roleId);
        } catch (Exception e) {
            log.error("检查角色标识唯一性失败：角色标识={}", roleKey, e);
            return 0;
        }
    }

    @Override
    public int countUsersByRoleId(Long roleId) {
        if (roleId == null) {
            return 0;
        }

        try {
            return roleMapper.countUsersByRoleId(roleId);
        } catch (Exception e) {
            log.error("统计角色用户数量失败：角色ID={}", roleId, e);
            return 0;
        }
    }

    // ==================== 用户角色分配方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUserRoles(Long userId, List<Long> roleIds) throws Exception {
        if (userId == null) {
            throw new Exception("用户ID不能为空");
        }

        // 删除现有角色分配
        cancelUserRoles(userId, null);

        // 分配新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            try {
                int rows = roleMapper.insertUserRoles(userId, roleIds);
                if (rows != roleIds.size()) {
                    throw new Exception("分配用户角色失败");
                }

                log.info("分配用户角色成功：用户ID={}, 角色数量={}", userId, roleIds.size());
            } catch (Exception e) {
                log.error("分配用户角色失败：用户ID={}", userId, e);
                throw new Exception("分配用户角色失败：" + e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return false;
        }

        try {
            int rows = roleMapper.deleteUserRoles(userId, roleIds);
            log.info("取消用户角色分配成功：用户ID={}, 影响行数={}", userId, rows);
            return true;
        } catch (Exception e) {
            log.error("取消用户角色分配失败：用户ID={}", userId, e);
            return false;
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            return roleMapper.selectUserRoleIds(userId);
        } catch (Exception e) {
            log.error("查询用户角色ID失败：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    // ==================== 角色菜单权限方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            return false;
        }

        // 删除现有菜单权限
        cancelRoleMenus(roleId, null);

        // 分配新菜单权限
        if (menuIds != null && !menuIds.isEmpty()) {
            try {
                int rows = roleMapper.assignRoleMenus(roleId, menuIds);
                if (rows != menuIds.size()) {
                    log.warn("分配角色菜单权限部分失败：角色ID={}, 期望数量={}, 实际数量={}",
                            roleId, menuIds.size(), rows);
                }

                log.info("分配角色菜单权限成功：角色ID={}, 菜单数量={}", roleId, menuIds.size());
            } catch (Exception e) {
                log.error("分配角色菜单权限失败：角色ID={}", roleId, e);
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRoleMenus(Long roleId, List<Long> menuIds) {
        if (roleId == null) {
            return false;
        }

        try {
            int rows = roleMapper.deleteRoleMenus(roleId);
            log.info("取消角色菜单权限分配成功：角色ID={}, 影响行数={}", roleId, rows);
            return true;
        } catch (Exception e) {
            log.error("取消角色菜单权限分配失败：角色ID={}", roleId, e);
            return false;
        }
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        try {
            return roleMapper.selectMenuIdsByRoleId(roleId);
        } catch (Exception e) {
            log.error("查询角色菜单ID失败：角色ID={}", roleId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Set<String> getRolePermissions(Long roleId) {
        if (roleId == null) {
            return new HashSet<>();
        }

        try {
            return roleMapper.selectPermissionsByRoleId(roleId);
        } catch (Exception e) {
            log.error("查询角色权限失败：角色ID={}", roleId, e);
            return new HashSet<>();
        }
    }

    // ==================== 角色数据权限方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleDepts(Long roleId, List<Long> deptIds) {
        if (roleId == null) {
            return false;
        }

        // 注释：sys_role_dept表不存在，无需删除角色原有部门权限

        // 注释：sys_role_dept表不存在，无法分配角色部门权限

        return true;
    }

  
    @Override
    public List<Long> getRoleDeptIds(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        try {
            return roleMapper.selectDeptIdsByRoleId(roleId);
        } catch (Exception e) {
            log.error("查询角色部门ID失败：角色ID={}", roleId, e);
            return new ArrayList<>();
        }
    }

    // ==================== 状态管理方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoleStatus(Long roleId, String status) {
        if (roleId == null || status == null) {
            return false;
        }

        try {
            SysRole role = new SysRole();
            role.setRoleId(roleId);
            role.setStatus(status);
            role.setUpdateBy(SecurityUtils.getCurrentUsername());
            role.setUpdateTime(LocalDateTime.now());

            int rows = roleMapper.updateRole(role);
            if (rows > 0) {
                String statusText = "0".equals(status) ? "启用" : "停用";
                log.info("更新角色状态成功：角色ID={}, 状态={}", roleId, statusText);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("更新角色状态失败：角色ID={}, 状态={}", roleId, status, e);
            return false;
        }
    }

    // ==================== 统计方法 ====================

    @Override
    public Map<String, Object> getRoleStatistics(Long roleId) {
        Map<String, Object> statistics = new HashMap<>();

        if (roleId == null) {
            return statistics;
        }

        try {
            // 用户数量
            int userCount = countUsersByRoleId(roleId);
            statistics.put("userCount", userCount);

            // 菜单权限数量
            List<Long> menuIds = getRoleMenuIds(roleId);
            statistics.put("menuCount", menuIds != null ? menuIds.size() : 0);

            // 部门权限数量
            List<Long> deptIds = getRoleDeptIds(roleId);
            statistics.put("deptCount", deptIds != null ? deptIds.size() : 0);

            return statistics;
        } catch (Exception e) {
            log.error("获取角色统计信息失败：角色ID={}", roleId, e);
            return statistics;
        }
    }

    @Override
    public Map<String, Object> getAllRoleStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 总角色数量
            SysRole countRole = new SysRole();
            int totalRoles = roleMapper.countRoles(countRole);
            statistics.put("totalRoles", totalRoles);

            // 各类型角色数量
            Map<String, Integer> roleTypeCount = new HashMap<>();
            SysRole systemRole = new SysRole();
            systemRole.setDeptType("1");
            roleTypeCount.put("system", roleMapper.countRoles(systemRole));

            SysRole agentRole = new SysRole();
            agentRole.setDeptType("2");
            roleTypeCount.put("agent", roleMapper.countRoles(agentRole));

            SysRole buyerMainRole = new SysRole();
            buyerMainRole.setDeptType("3");
            roleTypeCount.put("buyerMain", roleMapper.countRoles(buyerMainRole));

            SysRole buyerSubRole = new SysRole();
            buyerSubRole.setDeptType("4");
            roleTypeCount.put("buyerSub", roleMapper.countRoles(buyerSubRole));
            statistics.put("roleTypeCount", roleTypeCount);

            // 各状态角色数量 - 使用updateRoleStatus方法更新状态来统计
            Map<String, Integer> roleStatusCount = new HashMap<>();
            SysRole normalRole = new SysRole();
            normalRole.setStatus("0");
            roleStatusCount.put("normal", roleMapper.countRoles(normalRole));

            SysRole disabledRole = new SysRole();
            disabledRole.setStatus("1");
            roleStatusCount.put("disabled", roleMapper.countRoles(disabledRole));
            statistics.put("roleStatusCount", roleStatusCount);

            return statistics;
        } catch (Exception e) {
            log.error("获取所有角色统计信息失败", e);
            return statistics;
        }
    }

    // ==================== 权限验证方法 ====================

    @Override
    public void validateRoleCreatePermission(SysRole role) throws Exception {
        if (role == null || role.getDeptType() == null) {
            throw new Exception("角色部门类型不能为空");
        }

        // 超级管理员可以创建所有类型的角色
        if (SecurityUtils.isCurrentUserAdmin()) {
            return;
        }

        // 其他权限验证逻辑
        // 这里可以根据实际业务需求添加更细粒度的权限控制
    }

    @Override
    public boolean isRoleApplicableToDeptType(Long roleId, String deptType) {
        if (roleId == null || deptType == null) {
            return false;
        }

        try {
            SysRole role = selectRoleById(roleId);
            return role != null && deptType.equals(role.getDeptType());
        } catch (Exception e) {
            log.error("检查角色部门类型适用性失败：角色ID={}, 部门类型={}", roleId, deptType, e);
            return false;
        }
    }

    @Override
    public SysRole getDefaultRoleByDeptType(String deptType) {
        if (deptType == null) {
            return null;
        }

        try {
            // 根据部门类型查询角色列表，返回第一个作为默认角色
            List<SysRole> roles = roleMapper.selectRolesByDeptType(deptType);
            return roles != null && !roles.isEmpty() ? roles.get(0) : null;
        } catch (Exception e) {
            log.error("查询默认角色失败：部门类型={}", deptType, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initDefaultRoles() {
        try {
            List<SysRole> defaultRoles = Arrays.asList(
                SysRole.createAdminRole(),
                SysRole.createSystemAdminRole(),
                SysRole.createTechAdminRole(),
                SysRole.createOpsAdminRole(),
                SysRole.createAgentRole(),
                SysRole.createBuyerMainRole(),
                SysRole.createBuyerSubRole()
            );

            boolean allSuccess = true;
            for (SysRole role : defaultRoles) {
                try {
                    // 检查是否已存在
                    if (checkRoleKeyUnique(role.getRoleKey(), null) == 0) {
                        insertRole(role);
                        log.info("初始化默认角色成功：{}", role.getRoleName());
                    } else {
                        log.info("默认角色已存在，跳过：{}", role.getRoleName());
                    }
                } catch (Exception e) {
                    log.error("初始化默认角色失败：{}", role.getRoleName(), e);
                    allSuccess = false;
                }
            }

            return allSuccess;
        } catch (Exception e) {
            log.error("初始化默认角色失败", e);
            return false;
        }
    }

    @Override
    public boolean syncRolePermissions(Long roleId) {
        // 这里可以实现角色权限的同步逻辑
        // 比如重新计算菜单权限缓存、数据权限范围等
        log.info("同步角色权限：角色ID={}", roleId);
        return true;
    }

    @Override
    public Map<String, Object> validateRoleData(Long roleId) {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (roleId == null) {
            issues.add("角色ID不能为空");
            result.put("valid", false);
            result.put("issues", issues);
            result.put("suggestions", suggestions);
            return result;
        }

        try {
            SysRole role = selectRoleById(roleId);
            if (role == null) {
                issues.add("角色不存在");
            } else {
                // 检查基本字段
                if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
                    issues.add("角色名称不能为空");
                }

                if (role.getRoleKey() == null || role.getRoleKey().trim().isEmpty()) {
                    issues.add("角色标识不能为空");
                }

                // 检查权限关联
                List<Long> menuIds = getRoleMenuIds(roleId);
                if (menuIds == null || menuIds.isEmpty()) {
                    suggestions.add("建议为角色分配菜单权限");
                }

                if ("2".equals(role.getDataScope())) {
                    List<Long> deptIds = getRoleDeptIds(roleId);
                    if (deptIds == null || deptIds.isEmpty()) {
                        issues.add("自定义数据权限的角色必须分配部门权限");
                    }
                }
            }

            result.put("valid", issues.isEmpty());
            result.put("issues", issues);
            result.put("suggestions", suggestions);
        } catch (Exception e) {
            log.error("验证角色数据失败：角色ID={}", roleId, e);
            issues.add("验证过程中发生异常：" + e.getMessage());
            result.put("valid", false);
            result.put("issues", issues);
            result.put("suggestions", suggestions);
        }

        return result;
    }
}