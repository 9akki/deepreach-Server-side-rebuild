package com.deepreach.common.security;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.entity.SysRole;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.domain.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现类
 *
 * 基于部门类型的用户详情服务实现，实现 Spring Security 的 UserDetailsService 接口：
 * 1. 根据用户名加载用户详情
 * 2. 构建完整的用户认证信息
 * 3. 加载用户角色和权限
 * 4. 支持用户状态验证
 * 5. 基于部门类型的权限验证
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 简化权限逻辑：基于组织架构的权限控制
 * - 统一认证流程：所有用户都通过统一的认证流程
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 根据用户名加载用户详情
     *
     * 这是 Spring Security 认证过程中的核心方法：
     * 1. 查询用户基本信息
     * 2. 查询用户角色和权限
     * 3. 构建认证用的 UserDetails 对象
     * 4. 验证用户状态
     *
     * @param username 用户名
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        log.debug("正在加载用户详情: {}", username);

        try {
            // 1. 查询用户基本信息（包含部门信息）
            SysUser sysUser = userMapper.selectUserWithRolesAndPermissions(username);
            if (sysUser == null) {
                log.warn("用户不存在: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            // 2. 验证用户状态
            if ("1".equals(sysUser.getStatus())) {
                log.warn("用户已被停用: {}", username);
                throw new UsernameNotFoundException("用户已被停用: " + username);
            }

            // 3. 查询用户角色
            Set<SysRole> roles = userMapper.selectRolesByUserId(sysUser.getUserId());
            if (roles == null) {
                roles = Set.of();
            }

            // 4. 查询用户权限（超级管理员特殊处理）
            Set<String> permissions;
            if (isSuperAdmin(sysUser.getUserId(), roles)) {
                // 超级管理员赋予所有权限
                permissions = getAllSystemPermissions();
                log.info("超级管理员 {} 获取所有权限", username);
            } else {
                // 普通用户获取配置的权限
                permissions = userMapper.selectPermissionsByUserId(sysUser.getUserId());
                if (permissions == null) {
                    permissions = Set.of();
                }
                log.debug("普通用户 {} 获取权限: {}", username, permissions.size());
            }

            // 5. 构建 LoginUser 对象
            LoginUser loginUser = LoginUser.fromSysUser(sysUser,
                roles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet()),
                permissions);

            log.debug("用户详情加载成功: {}, 角色: {}, 权限数量: {}",
                username, roles.size(), permissions.size());

            return loginUser;

        } catch (UsernameNotFoundException e) {
            // 重新抛出用户不存在异常
            throw e;
        } catch (Exception e) {
            log.error("加载用户详情时发生异常: {}", username, e);
            throw new UsernameNotFoundException("加载用户详情失败: " + username, e);
        }
    }

    /**
     * 判断是否为超级管理员
     *
     * @param userId 用户ID
     * @param roles 用户角色
     * @return 是否为超级管理员
     */
    private boolean isSuperAdmin(Long userId, Set<SysRole> roles) {
        // 超级管理员用户ID为1且拥有ADMIN角色
        if (userId != null && userId == 1L && roles != null) {
            return roles.stream().anyMatch(role -> "ADMIN".equals(role.getRoleKey()));
        }
        return false;
    }

    /**
     * 获取系统所有权限
     *
     * @return 系统所有权限集合
     */
    private Set<String> getAllSystemPermissions() {
        Set<String> permissions = new HashSet<>();

        // 添加通配符权限（最高权限）
        permissions.add("*:*:*");

        // 添加系统基础权限
        permissions.add("system:user:list");
        permissions.add("system:user:add");
        permissions.add("system:user:edit");
        permissions.add("system:user:delete");
        permissions.add("system:user:remove");
        permissions.add("system:user:query");
        permissions.add("system:user:import");
        permissions.add("system:user:export");
        permissions.add("system:user:resetPwd");

        // 角色管理权限
        permissions.add("system:role:list");
        permissions.add("system:role:add");
        permissions.add("system:role:edit");
        permissions.add("system:role:delete");
        permissions.add("system:role:query");

        // 菜单管理权限
        permissions.add("system:menu:list");
        permissions.add("system:menu:add");
        permissions.add("system:menu:edit");
        permissions.add("system:menu:delete");
        permissions.add("system:menu:query");

        // 部门管理权限
        permissions.add("system:dept:list");
        permissions.add("system:dept:add");
        permissions.add("system:dept:edit");
        permissions.add("system:dept:delete");
        permissions.add("system:dept:query");

        // 字典管理权限
        permissions.add("system:dict:list");
        permissions.add("system:dict:add");
        permissions.add("system:dict:edit");
        permissions.add("system:dict:delete");
        permissions.add("system:dict:query");

        // 参数设置权限
        permissions.add("system:config:list");
        permissions.add("system:config:add");
        permissions.add("system:config:edit");
        permissions.add("system:config:delete");
        permissions.add("system:config:query");

        // 通知公告权限
        permissions.add("system:notice:list");
        permissions.add("system:notice:add");
        permissions.add("system:notice:edit");
        permissions.add("system:notice:delete");
        permissions.add("system:notice:query");

        // 日志管理权限
        permissions.add("system:log:list");
        permissions.add("system:log:delete");
        permissions.add("system:log:query");

        // 在线用户权限
        permissions.add("monitor:online:list");
        permissions.add("monitor:online:forceLogout");

        // 定时任务权限
        permissions.add("monitor:job:list");
        permissions.add("monitor:job:add");
        permissions.add("monitor:job:edit");
        permissions.add("monitor:job:delete");
        permissions.add("monitor:job:query");
        permissions.add("monitor:job:start");
        permissions.add("monitor:job:pause");

        // 服务监控权限
        permissions.add("monitor:server:list");
        permissions.add("monitor:server:query");

        // 缓存监控权限
        permissions.add("monitor:cache:list");
        permissions.add("monitor:cache:listKeys");
        permissions.add("monitor:cache:getName");
        permissions.add("monitor:cache:getValue");

        // 系统监控权限
        permissions.add("monitor:system:list");
        permissions.add("monitor:system:cpu");
        permissions.add("monitor:system:memory");
        permissions.add("monitor:system:disk");

        log.debug("加载系统所有权限，总计: {} 个", permissions.size());
        return permissions;
    }
}