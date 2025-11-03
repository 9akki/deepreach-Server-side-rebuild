package com.deepreach.common.core.service.impl;

import com.deepreach.common.core.domain.entity.SysDept;
import com.deepreach.common.core.domain.entity.SysOperLog;
import com.deepreach.common.core.domain.entity.SysRole;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.domain.dto.DeptUserGroupDTO;
import com.deepreach.common.core.mapper.SysUserMapper;
import com.deepreach.common.core.mapper.SysRoleMapper;
import com.deepreach.common.core.service.SysDeptService;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统用户Service实现类
 *
 * 基于部门类型的简化用户Service实现，包含：
 * 1. 用户基本信息管理业务逻辑
 * 2. 用户认证和授权业务处理
 * 3. 用户角色和权限管理
 * 4. 基于组织架构的数据权限控制实现
 * 5. 安全相关业务处理
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定，无需手动设置
 * - 简化业务逻辑：移除复杂的业务字段操作，专注于组织架构管理
 * - 权限控制优化：基于部门类型和层级进行细粒度的权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SysDeptService deptService;

    @Autowired
    private SysRoleMapper roleMapper;

    // ==================== 基础查询方法 ====================

    /**
     * 根据用户ID查询用户
     */
    @Override
    public SysUser selectUserById(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询用户失败：用户ID无效 - {}", userId);
            return null;
        }

        try {
            SysUser user = userMapper.selectUserById(userId);
            if (user != null) {
                log.debug("查询用户成功：用户ID={}, 用户名={}", userId, user.getUsername());
            } else {
                log.debug("查询用户失败：用户不存在 - {}", userId);
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户异常：用户ID={}", userId, e);
            throw new RuntimeException("查询用户信息失败", e);
        }
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public SysUser selectUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("查询用户失败：用户名为空");
            return null;
        }

        try {
            SysUser user = userMapper.selectUserByUsername(username.trim());
            if (user != null) {
                log.debug("查询用户成功：用户名={}, 用户ID={}", username, user.getUserId());
            } else {
                log.debug("查询用户失败：用户名不存在 - {}", username);
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户异常：用户名={}", username, e);
            throw new RuntimeException("查询用户信息失败", e);
        }
    }

    /**
     * 根据条件查询用户列表
     */
    @Override
    public List<SysUser> selectUserList(SysUser user) {
        if (user == null) {
            user = new SysUser();
        }

        try {
            // 应用数据权限过滤
            applyDataPermissionFilter(user);

            List<SysUser> userList = userMapper.selectUserList(user);
            log.debug("查询用户列表成功：查询条件={}, 结果数量={}",
                    getQueryCondition(user), userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询用户列表异常：查询条件={}", getQueryCondition(user), e);
            throw new RuntimeException("查询用户列表失败", e);
        }
    }

    /**
     * 根据部门ID查询用户列表
     */
    @Override
    public List<SysUser> selectUsersByDeptId(Long deptId) {
        return selectUsersByDeptId(deptId, null);
    }

    @Override
    public List<SysUser> selectUsersByDeptId(Long deptId, SysUser query) {
        if (deptId == null || deptId <= 0) {
            log.warn("查询部门用户失败：部门ID无效 - {}", deptId);
            return new ArrayList<>();
        }

        try {
            SysUser filter = query;
            List<SysUser> userList = userMapper.selectUsersByDeptId(deptId, filter);
            log.debug("查询部门用户成功：部门ID={}, 查询条件={}, 结果数量={}",
                    deptId, getQueryCondition(filter), userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询部门用户异常：部门ID={}, 查询条件={}", deptId, getQueryCondition(query), e);
            throw new RuntimeException("查询部门用户失败", e);
        }
    }

    @Override
    public List<SysUser> selectUsersByDeptOnly(Long deptId, SysUser user) {
        if (deptId == null || deptId <= 0) {
            log.warn("查询部门用户失败：部门ID无效 - {}", deptId);
            return new ArrayList<>();
        }

        try {
            SysUser filter = user;
            List<SysUser> userList = userMapper.selectUsersByDeptOnly(deptId, filter);
            log.debug("查询部门用户（仅当前部门）成功：部门ID={}, 查询条件={}, 结果数量={}",
                    deptId, getQueryCondition(filter), userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询部门用户（仅当前部门）异常：部门ID={}, 查询条件={}", deptId, getQueryCondition(user), e);
            throw new RuntimeException("查询部门用户失败", e);
        }
    }

    @Override
    public List<SysUser> searchUsersByDept(Long deptId, String deptType, SysUser query) {
        String normalizedDeptType = StringUtils.trimToNull(deptType);
        if ((deptId == null || deptId <= 0) && normalizedDeptType == null) {
            log.warn("条件查询用户失败：部门ID和部门类型不能同时为空");
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.searchUsersByDept(deptId, normalizedDeptType, query);
            log.debug("条件查询部门用户成功：部门ID={}, 部门类型={}, 查询条件={}, 结果数量={}",
                    deptId, normalizedDeptType, getQueryCondition(query), userList.size());
            return userList;
        } catch (Exception e) {
            log.error("条件查询部门用户异常：部门ID={}, 部门类型={}, 查询条件={}",
                    deptId, normalizedDeptType, getQueryCondition(query), e);
            throw new RuntimeException("条件查询部门用户失败", e);
        }
    }

    @Override
    public List<DeptUserGroupDTO> listUsersByLeaderDirectDepts(Long leaderUserId) {
        if (leaderUserId == null || leaderUserId <= 0) {
            log.warn("根据负责人查询部门用户失败：负责人ID无效 - {}", leaderUserId);
            return Collections.emptyList();
        }

        List<SysDept> directDepts = deptService.selectDeptsByLeaderUserId(leaderUserId);
        if (directDepts == null || directDepts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> deptIds = directDepts.stream()
            .map(SysDept::getDeptId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysUser> users = userMapper.selectUsersByDeptIds(deptIds);
        Map<Long, List<SysUser>> grouped = new LinkedHashMap<>();
        for (SysUser user : users) {
            Long deptId = user.getDeptId();
            if (deptId == null) {
                continue;
            }
            grouped.computeIfAbsent(deptId, k -> new ArrayList<>()).add(user);
        }

        List<DeptUserGroupDTO> result = new ArrayList<>();
        for (SysDept dept : directDepts) {
            Long deptId = dept.getDeptId();
            if (deptId == null) {
                continue;
            }
            DeptUserGroupDTO dto = new DeptUserGroupDTO();
            dto.setDeptId(deptId);
            dto.setDeptName(dept.getDeptName());
            dto.setDeptType(dept.getDeptType());
            dto.setLevel(dept.getLevel());

            List<SysUser> deptUsers = grouped.getOrDefault(deptId, Collections.emptyList());
            List<DeptUserGroupDTO.UserSummary> summaries = deptUsers.stream()
                .map(this::buildUserSummary)
                .collect(Collectors.toList());
            dto.setUsers(summaries);
            result.add(dto);
        }
        return result;
    }

    @Override
    public boolean hasDeptDataPermission(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return false;
        }
        if (SecurityUtils.hasPermission("system:user:list")) {
            return true;
        }
        return deptService.hasDeptDataPermission(deptId);
    }

    // ==================== CUD操作方法 ====================

    /**
     * 创建新用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser insertUser(SysUser user) throws Exception {
        normalizeOptionalFields(user);
        // 参数验证
        validateUserForInsert(user);

        // 验证用户创建权限（基于部门类型的权限控制）
        validateUserCreatePermission(user);

        try {
            // 设置默认值
            setDefaultValues(user);

            // 密码加密
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            // 插入用户
            int result = userMapper.insertUser(user);
            if (result <= 0) {
                throw new RuntimeException("创建用户失败：数据库操作失败");
            }

            // MyBatis 在部分驱动上不会自动回填主键，手动兜底查询一次
            if (user.getUserId() == null) {
                SysUser persistedUser = userMapper.selectUserByUsername(user.getUsername());
                if (persistedUser != null) {
                    user.setUserId(persistedUser.getUserId());
                }
            }

            // 根据部门类型自动分配角色
            assignRoleByDeptType(user);

            // 设置简化的角色和部门信息
            setSimplifiedUserInfo(user);

            log.info("创建用户成功：用户ID={}, 用户名={}, 部门ID={}, 创建者={}",
                    user.getUserId(), user.getUsername(), user.getDeptId(), user.getCreateBy());

            return user;
        } catch (Exception e) {
            log.error("创建用户异常：用户名={}", user.getUsername(), e);
            throw new RuntimeException("创建用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) throws Exception {
        // 参数验证
        validateUserForUpdate(user);

        // 检查数据权限
        if (!hasUserDataPermission(user.getUserId())) {
            throw new RuntimeException("无权限修改该用户信息");
        }

        try {
            // 清除敏感字段
            user.setPassword(null);
            user.setUsername(null); // 用户名通常不允许修改

            int result = userMapper.updateUser(user);
            if (result > 0) {
                log.info("更新用户成功：用户ID={}, 更新者={}", user.getUserId(), user.getUpdateBy());
                return true;
            } else {
                log.warn("更新用户失败：用户不存在或无变更 - {}", user.getUserId());
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户异常：用户ID={}", user.getUserId(), e);
            throw new RuntimeException("更新用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long userId) throws Exception {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限删除该用户");
        }

        // 检查用户是否存在
        SysUser user = selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否为超级管理员
        if (user.isAdmin()) {
            throw new RuntimeException("不能删除超级管理员");
        }

        try {
            // 删除用户角色关联
            userMapper.deleteUserRoles(userId);

            // 删除用户
            int result = userMapper.deleteUserById(userId);
            if (result > 0) {
                log.info("删除用户成功：用户ID={}, 用户名={}", userId, user.getUsername());
                return true;
            } else {
                log.warn("删除用户失败：用户ID={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("删除用户异常：用户ID={}", userId, e);
            throw new RuntimeException("删除用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(List<Long> userIds) throws Exception {
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("用户ID列表为空");
        }

        // 过滤无效ID并检查权限
        List<Long> validIds = userIds.stream()
                .filter(id -> id != null && id > 0)
                .filter(this::hasUserDataPermission)
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            throw new RuntimeException("没有可删除的有效用户");
        }

        try {
            // 批量删除用户角色关联
            for (Long userId : validIds) {
                userMapper.deleteUserRoles(userId);
            }

            // 批量删除用户
            int result = userMapper.deleteUserByIds(validIds);
            if (result > 0) {
                log.info("批量删除用户成功：删除数量={}, 用户IDs={}", result, validIds);
                return true;
            } else {
                log.warn("批量删除用户失败：用户IDs={}", validIds);
                return false;
            }
        } catch (Exception e) {
            log.error("批量删除用户异常：用户IDs={}", validIds, e);
            throw new RuntimeException("批量删除用户失败：" + e.getMessage(), e);
        }
    }

    // ==================== 密码管理方法 ====================

    /**
     * 重置用户密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限重置该用户密码");
        }

        // 密码强度验证
        validatePasswordStrength(newPassword);

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            int result = userMapper.updateUserPassword(userId, encryptedPassword);

            if (result > 0) {
                log.info("重置用户密码成功：用户ID={}, 操作者={}", userId, SecurityUtils.getCurrentUsername());
                return true;
            } else {
                log.warn("重置用户密码失败：用户不存在 - {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("重置用户密码异常：用户ID={}", userId, e);
            throw new RuntimeException("重置密码失败：" + e.getMessage(), e);
        }
    }

    /**
     * 修改用户密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new RuntimeException("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }

        // 检查是否为自己修改密码
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null || !userId.equals(currentUser.getUserId())) {
            throw new RuntimeException("只能修改自己的密码");
        }

        // 获取用户信息
        SysUser user = selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 验证新密码强度
        validatePasswordStrength(newPassword);

        // 检查新密码是否与原密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            int result = userMapper.updateUserPassword(userId, encryptedPassword);

            if (result > 0) {
                log.info("修改用户密码成功：用户ID={}", userId);
                return true;
            } else {
                log.warn("修改用户密码失败：用户ID={}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("修改用户密码异常：用户ID={}", userId, e);
            throw new RuntimeException("修改密码失败：" + e.getMessage(), e);
        }
    }

    // ==================== 角色权限管理方法 ====================

    /**
     * 分配用户角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignUserRoles(Long userId, List<Long> roleIds) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (roleIds == null) {
            roleIds = new ArrayList<>();
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限修改该用户角色");
        }

        try {
            // 清除原有角色
            userMapper.deleteUserRoles(userId);

            // 分配新角色
            if (!roleIds.isEmpty()) {
                int result = userMapper.assignUserRoles(userId, roleIds);
                if (result <= 0) {
                    throw new RuntimeException("分配角色失败");
                }
            }

            log.info("分配用户角色成功：用户ID={}, 角色数量={}", userId, roleIds.size());
            return true;
        } catch (Exception e) {
            log.error("分配用户角色异常：用户ID={}, 角色IDs={}", userId, roleIds, e);
            throw new RuntimeException("分配角色失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取用户角色ID列表
     */
    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        try {
            return userMapper.selectUserRoleIds(userId);
        } catch (Exception e) {
            log.error("获取用户角色ID异常：用户ID={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户权限标识集合
     */
    @Override
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null || userId <= 0) {
            return new HashSet<>();
        }

        try {
            return userMapper.selectPermissionsByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户权限异常：用户ID={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 获取用户角色标识集合
     */
    @Override
    public Set<String> getUserRoles(Long userId) {
        if (userId == null || userId <= 0) {
            return new HashSet<>();
        }

        try {
            return userMapper.selectRoleKeysByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户角色异常：用户ID={}", userId, e);
            return new HashSet<>();
        }
    }

    // ==================== 认证相关方法 ====================

    /**
     * 根据用户ID获取登录用户信息
     */
    @Override
    public LoginUser selectLoginUserById(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            // 查询用户信息
            SysUser user = userMapper.selectUserById(userId);
            if (user == null) {
                return null;
            }

            // 查询用户角色和权限
            Set<String> roles = getUserRoles(userId);
            Set<String> permissions = getUserPermissions(userId);

            // 构建LoginUser对象
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getUserId());
            loginUser.setUsername(user.getUsername());
            loginUser.setDeptId(user.getDeptId());
            loginUser.setRoles(roles);
            loginUser.setPermissions(permissions);

            // 设置其他必要的字段
            loginUser.setNickname(user.getNickname());
            loginUser.setEmail(user.getEmail());
            loginUser.setPhone(user.getPhone());

            // 注意：LoginUser会自动计算isAdmin()，无需手动设置

            return loginUser;
        } catch (Exception e) {
            log.error("根据用户ID获取登录用户信息失败：用户ID={}", userId, e);
            return null;
        }
    }

    @Override
    public com.deepreach.common.core.domain.vo.UserVO getCompleteUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            // 查询用户基本信息（包含部门信息）
            com.deepreach.common.core.domain.vo.UserVO userVO = userMapper.selectCompleteUserInfo(userId);
            if (userVO == null) {
                return null;
            }

            // 获取用户角色和权限
            Set<String> roles = getUserRoles(userId);
            Set<String> permissions = getUserPermissions(userId);

            // 设置角色和权限信息
            userVO.setRoles(roles);
            userVO.setPermissions(permissions);

            // 设置基于部门类型的字段
            SysUser user = selectUserById(userId);
            if (user != null) {
                // 获取部门信息
                com.deepreach.common.core.domain.entity.SysDept dept = user.getDept();
                if (dept != null) {
                    userVO.setDeptType(dept.getDeptType());
                    userVO.setDeptName(dept.getDeptName());
                    userVO.setAgentLevel(dept.getLevel());
                }

                userVO.setParentUserId(user.getParentUserId());
                userVO.setLeaderId(user.getLeaderId());
                userVO.setLeaderNickname(user.getLeaderNickname());
                // TODO: 可以设置父用户名称
            }

            // 构建包含显示字段的完整用户信息
            return userVO.buildWithDisplayFields();
        } catch (Exception e) {
            log.error("获取用户完整信息失败，用户ID：{}", userId, e);
            return null;
        }
    }

    /**
     * 用户登录认证
     */
    @Override
    public LoginUser authenticate(String username, String password, String loginIp) throws Exception {
        // 参数验证
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        try {
            // 查询用户信息
            SysUser user = userMapper.selectUserWithRolesAndPermissions(username.trim());
            if (user == null) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 检查用户状态
            if (!user.isNormal()) {
                throw new RuntimeException("用户账号已被停用");
            }

            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }

            // 获取用户角色和权限
            Set<String> roles = getUserRoles(user.getUserId());
            Set<String> permissions = getUserPermissions(user.getUserId());

            // 构建登录用户对象
            LoginUser loginUser = LoginUser.fromSysUser(user, roles, permissions);
            loginUser.setIpaddr(loginIp);
            loginUser.setLoginTime(LocalDateTime.now());

            // 设置基于部门类型的字段
            // LoginUser.fromSysUser方法中已经处理了部门类型的字段设置
            // 这里只需要设置父用户信息（如果有的话）
            if (user.getParentUserId() != null) {
                loginUser.setParentUserId(user.getParentUserId());
            }

            // 记录登录信息
            recordLoginInfo(user.getUserId(), loginIp, null);

            log.info("用户登录成功：用户ID={}, 用户名={}, 登录IP={}",
                    user.getUserId(), user.getUsername(), loginIp);

            return loginUser;
        } catch (RuntimeException e) {
            log.warn("用户登录失败：用户名={}, 登录IP={}, 失败原因={}",
                    username, loginIp, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("用户登录异常：用户名={}", username, e);
            throw new RuntimeException("登录失败，请稍后重试", e);
        }
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(SysUser user) throws Exception {
        normalizeOptionalFields(user);
        // 参数验证
        validateUserForRegister(user);

        try {
            // 设置默认值
            setDefaultValuesForRegister(user);

            // 密码加密
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            // 插入用户
            int result = userMapper.insertUser(user);
            if (result <= 0) {
                throw new RuntimeException("注册失败：数据库操作失败");
            }

            log.info("用户注册成功：用户ID={}, 用户名={}", user.getUserId(), user.getUsername());
            return user;
        } catch (Exception e) {
            log.error("用户注册异常：用户名={}", user.getUsername(), e);
            throw new RuntimeException("注册失败：" + e.getMessage(), e);
        }
    }

    // ==================== 唯一性检查方法 ====================

    /**
     * 检查用户名是否唯一
     */
    @Override
    public boolean checkUsernameUnique(String username, Long userId) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        try {
            int count = userMapper.checkUsernameUnique(username.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查用户名唯一性异常：用户名={}", username, e);
            return false;
        }
    }

    /**
     * 检查邮箱是否唯一
     */
    @Override
    public boolean checkEmailUnique(String email, Long userId) {
        if (email == null || email.trim().isEmpty()) {
            return true; // 邮箱为空时认为唯一
        }

        try {
            int count = userMapper.checkEmailUnique(email.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查邮箱唯一性异常：邮箱={}", email, e);
            return false;
        }
    }

    /**
     * 检查手机号是否唯一
     */
    @Override
    public boolean checkPhoneUnique(String phone, Long userId) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // 手机号为空时认为唯一
        }

        try {
            int count = userMapper.checkPhoneUnique(phone.trim(), userId);
            return count == 0;
        } catch (Exception e) {
            log.error("检查手机号唯一性异常：手机号={}", phone, e);
            return false;
        }
    }

    // ==================== 其他业务方法 ====================

    /**
     * 记录用户登录信息
     *
     * 记录用户的最后登录信息，并手动记录登录操作日志
     * 由于数据库权限限制，不再使用触发器，改为代码层面记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginInfo(Long userId, String loginIp, String loginLocation) {
        if (userId == null || userId <= 0) {
            return;
        }

        try {
            // 获取用户信息
            SysUser user = selectUserById(userId);
            if (user == null) {
                log.warn("记录登录信息失败：用户不存在 - {}", userId);
                return;
            }

            // 更新用户最后登录信息
            userMapper.updateUserLoginInfo(userId, loginIp, LocalDateTime.now());

            // 手动记录登录操作日志
            recordLoginLog(user, loginIp, loginLocation);

            log.debug("记录用户登录信息成功：用户ID={}, 用户名={}, 登录IP={}",
                     userId, user.getUsername(), loginIp);
        } catch (Exception e) {
            log.error("记录用户登录信息异常：用户ID={}", userId, e);
        }
    }

    /**
     * 记录用户登录操作日志
     *
     * 手动记录用户的登录操作到操作日志表
     * 替代原来的数据库触发器功能
     *
     * @param user 登录用户信息
     * @param loginIp 登录IP地址
     * @param loginLocation 登录地点
     */
    private void recordLoginLog(SysUser user, String loginIp, String loginLocation) {
        try {
            // 使用SysOperLog的静态方法创建登录日志
            Integer operatorType = user.isBackendUser() ? 1 : 2;
            SysOperLog operLog = SysOperLog.createLoginLog(user.getUsername(), loginIp, operatorType);

            // 设置额外信息
            operLog.setDeptName(user.getDept() != null ? user.getDept().getDeptName() : "");
            operLog.setOperLocation(loginLocation != null ? loginLocation : "");
            operLog.setOperUrl("/auth/login"); // 登录接口URL

            // 这里应该调用操作日志Service记录日志
            // operLogService.insertOperlog(operLog);

            // 暂时使用应用日志记录，后续可以集成专门的操作日志服务
            log.info("用户登录日志 - 用户ID: {}, 用户名: {}, 部门: {}, 登录IP: {}, 登录地点: {}, 登录时间: {}",
                    user.getUserId(), user.getUsername(),
                    operLog.getDeptName(), loginIp, operLog.getOperLocation(), LocalDateTime.now());

        } catch (Exception e) {
            // 记录登录日志失败不应该影响登录流程
            log.error("记录登录日志异常：用户ID={}, 用户名={}",
                     user.getUserId(), user.getUsername(), e);
        }
    }

    /**
     * 更新用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, String status) throws Exception {
        // 参数验证
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户ID无效");
        }
        if (status == null || (!"0".equals(status) && !"1".equals(status))) {
            throw new RuntimeException("状态值无效");
        }

        // 检查数据权限
        if (!hasUserDataPermission(userId)) {
            throw new RuntimeException("无权限修改该用户状态");
        }

        // 检查是否为超级管理员
        if (userId == 1L) {
            throw new RuntimeException("不能修改超级管理员状态");
        }

        try {
            int result = userMapper.updateUserStatus(userId, status);
            if (result > 0) {
                log.info("更新用户状态成功：用户ID={}, 状态={}, 操作者={}",
                        userId, status, SecurityUtils.getCurrentUsername());
                return true;
            } else {
                log.warn("更新用户状态失败：用户不存在 - {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新用户状态异常：用户ID={}, 状态={}", userId, status, e);
            throw new RuntimeException("更新用户状态失败：" + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证用户插入参数
     *
     * 基于部门类型的用户验证，确保用户必须归属于某个部门
     */
    private void validateUserForInsert(SysUser user) throws Exception {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 验证部门ID（在新设计中为必填项）
        if (user.getDeptId() == null || user.getDeptId() <= 0) {
            throw new RuntimeException("用户必须归属于某个部门");
        }

        // 验证部门是否存在
        if (deptService.selectDeptById(user.getDeptId()) == null) {
            throw new RuntimeException("指定的部门不存在");
        }

        // 验证买家子账户的父用户ID
        if (user.getParentUserId() != null && user.getParentUserId() > 0) {
            validateParentUserForSubAccount(user);
        }

        // 用户名格式验证
        String username = user.getUsername().trim();
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度必须在3-20个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("用户名只能包含字母、数字和下划线");
        }

        // 唯一性检查
        if (!checkUsernameUnique(username, null)) {
            throw new RuntimeException("用户名已存在");
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!checkEmailUnique(user.getEmail(), null)) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!checkPhoneUnique(user.getPhone(), null)) {
                throw new RuntimeException("手机号已被使用");
            }
        }

        // 密码强度验证
        validatePasswordStrength(user.getPassword());
    }

    /**
     * 验证用户更新参数
     */
    private void validateUserForUpdate(SysUser user) throws Exception {
        if (user == null || user.getUserId() == null || user.getUserId() <= 0) {
            throw new RuntimeException("用户ID无效");
        }

        // 检查用户是否存在
        SysUser existingUser = selectUserById(user.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 邮箱唯一性检查
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!checkEmailUnique(user.getEmail(), user.getUserId())) {
                throw new RuntimeException("邮箱已被使用");
            }
        }

        // 手机号唯一性检查
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (!checkPhoneUnique(user.getPhone(), user.getUserId())) {
                throw new RuntimeException("手机号已被使用");
            }
        }
    }

    /**
     * 验证用户注册参数
     */
    private void validateUserForRegister(SysUser user) throws Exception {
        if (user == null) {
            throw new RuntimeException("用户信息不能为空");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }

        // 使用插入验证逻辑
        validateUserForInsert(user);
    }

    /**
     * 验证密码强度
     */
    private void validatePasswordStrength(String password) throws Exception {
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        String trimmedPassword = password.trim();
        if (trimmedPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        if (trimmedPassword.length() > 20) {
            throw new RuntimeException("密码长度不能超过20位");
        }

        // 可以根据需要添加更复杂的密码强度验证规则
        // 例如：必须包含大小写字母、数字、特殊字符等
    }

    private DeptUserGroupDTO.UserSummary buildUserSummary(SysUser user) {
        DeptUserGroupDTO.UserSummary summary = new DeptUserGroupDTO.UserSummary();
        summary.setUserId(user.getUserId());
        summary.setUsername(user.getUsername());
        summary.setNickname(user.getNickname());
        summary.setRealName(user.getRealName());
        summary.setPhone(user.getPhone());
        summary.setEmail(user.getEmail());
        summary.setStatus(user.getStatus());
        summary.setUserType(user.getUserType());
        return summary;
    }

    /**
     * 设置用户默认值
     */
    private void setDefaultValues(SysUser user) {
        if (user.getStatus() == null) {
            user.setStatus("0"); // 默认正常状态
        }

        SysDept targetDept = null;
        if (user.getDeptId() != null) {
            targetDept = deptService.selectDeptById(user.getDeptId());
        }

        if (user.getUserType() == null) {
            if (targetDept != null && "4".equals(targetDept.getDeptType())) {
                user.setUserType(2); // 买家子账户用户类型
            } else {
                user.setUserType(1); // 默认后台用户
            }
        }

        if (user.getGender() == null) {
            user.setGender("2"); // 默认未知性别
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }

        // 根据子账号部门负责人自动设置父用户ID
        if (targetDept != null && "4".equals(targetDept.getDeptType())) {
            Long leaderUserId = targetDept.getLeaderUserId();
            if (leaderUserId != null && leaderUserId > 0) {
                if (user.getParentUserId() == null || user.getParentUserId() <= 0) {
                    user.setParentUserId(leaderUserId);
                    log.debug("自动为子账户用户 {} 设置父用户ID为部门负责人 {}", user.getUsername(), leaderUserId);
                }
            } else {
                log.warn("子账户部门 {} 缺少负责人，无法自动设置父用户ID", targetDept.getDeptName());
            }
        }

        // 🔑 只有买家总账户创建买家子账户时才设置parent_user_id为自己（若与部门负责人一致）
        com.deepreach.common.core.domain.model.LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser != null && currentUser.getDept() != null) {
            String currentUserDeptType = currentUser.getDept().getDeptType();
            if ("3".equals(currentUserDeptType) && targetDept != null && "4".equals(targetDept.getDeptType())) {
                if (user.getParentUserId() == null || user.getParentUserId() <= 0) {
                    user.setParentUserId(currentUser.getUserId());
                    log.debug("设置买家子账户用户 {} 的父用户ID为 {}", user.getUsername(), currentUser.getUserId());
                }
            }
        }
    }

    private void normalizeOptionalFields(SysUser user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            String email = user.getEmail().trim();
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (user.getPhone() != null) {
            String phone = user.getPhone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        if (user.getNickname() != null) {
            String nickname = user.getNickname().trim();
            user.setNickname(nickname.isEmpty() ? null : nickname);
        }
        if (user.getRealName() != null) {
            String realName = user.getRealName().trim();
            user.setRealName(realName.isEmpty() ? null : realName);
        }
    }

    /**
     * 验证买家子账户的父用户
     */
    private void validateParentUserForSubAccount(SysUser user) throws Exception {
        // 获取当前登录用户
        com.deepreach.common.core.domain.model.LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // 获取目标部门的部门类型
        com.deepreach.common.core.domain.entity.SysDept targetDept = deptService.selectDeptById(user.getDeptId());
        if (targetDept == null) {
            throw new RuntimeException("目标部门不存在");
        }

        // 如果目标部门是买家子账户部门（dept_type = 4），则需要验证父用户
        if ("4".equals(targetDept.getDeptType())) {
            boolean isSuperAdmin = currentUser.isSuperAdmin();
            boolean isSystemAdmin = currentUser.getDept() != null
                && "1".equals(currentUser.getDept().getDeptType());
            if (!isSuperAdmin && !isSystemAdmin) {
                throw new RuntimeException("只有管理员可以创建买家子账户用户");
            }

            Long expectedLeaderUserId = targetDept.getLeaderUserId();
            if (expectedLeaderUserId == null || expectedLeaderUserId <= 0) {
                throw new RuntimeException("子账户部门未设置负责人，无法创建子账户用户");
            }

            if (user.getParentUserId() == null || user.getParentUserId() <= 0) {
                throw new RuntimeException("买家子账户必须指定父用户");
            }

            if (!expectedLeaderUserId.equals(user.getParentUserId())) {
                throw new RuntimeException("买家子账户的父用户必须是该子账户部门的负责人");
            }

            // 验证父用户是否存在且为买家总账户
            SysUser parentUser = selectUserById(user.getParentUserId());
            if (parentUser == null) {
                throw new RuntimeException("指定的父用户不存在");
            }

            // 验证父用户是否为买家总账户类型
            com.deepreach.common.core.domain.entity.SysDept parentDept = deptService.selectDeptById(parentUser.getDeptId());
            if (parentDept == null || !"3".equals(parentDept.getDeptType())) {
                throw new RuntimeException("父用户必须归属于买家总账户部门");
            }
        }
    }

    /**
     * 验证用户创建权限（基于部门类型的权限控制）
     *
     * @param user 要创建的用户
     * @throws Exception 如果没有权限则抛出异常
     */
    public void validateUserCreatePermission(SysUser user) throws Exception {
        // 获取当前登录用户
        com.deepreach.common.core.domain.model.LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // 获取目标部门的部门类型
        com.deepreach.common.core.domain.entity.SysDept targetDept = deptService.selectDeptById(user.getDeptId());
        if (targetDept == null) {
            throw new RuntimeException("目标部门不存在");
        }

        String targetDeptType = targetDept.getDeptType();
        String currentUserDeptType = currentUser.getDept() != null ? currentUser.getDept().getDeptType() : null;
        Long currentDeptId = currentUser.getDeptId();
        boolean isSuperAdmin = currentUser.isSuperAdmin();
        boolean isSystemAdmin = "1".equals(currentUserDeptType);

        if (isSuperAdmin || isSystemAdmin) {
            if (user.getUserId() != null && user.getUserId() == 1L) {
                throw new RuntimeException("不能创建超级管理员账号");
            }
            log.info("管理员 {} 在部门 {} (类型: {}) 下创建用户",
                currentUser.getUsername(), targetDept.getDeptName(), targetDeptType);
            return;
        }

        if ("4".equals(targetDeptType)) {
            throw new RuntimeException("只有管理员可以在买家子账户部门创建用户");
        }

        if ("2".equals(currentUserDeptType)) {
            if (currentDeptId == null || currentDeptId <= 0) {
                throw new RuntimeException("当前用户部门信息异常");
            }
            java.util.List<Long> managedDeptIds = deptService.selectChildDeptIds(currentDeptId);
            if (managedDeptIds == null || !managedDeptIds.contains(targetDept.getDeptId())) {
                throw new RuntimeException("您没有权限在该部门创建用户");
            }
            log.info("代理用户 {} 在部门 {} (类型: {}) 下创建用户",
                currentUser.getUsername(), targetDept.getDeptName(), targetDeptType);
            return;
        }

        if ("3".equals(currentUserDeptType)) {
            if (targetDept.getLeaderUserId() == null || !targetDept.getLeaderUserId().equals(currentUser.getUserId())) {
                throw new RuntimeException("您没有权限在该部门创建用户");
            }
            log.info("买家总账号用户 {} 在所属部门 {} 下创建用户",
                currentUser.getUsername(), targetDept.getDeptName());
            return;
        }

        if ("4".equals(currentUserDeptType)) {
            throw new RuntimeException("买家子账户用户没有创建用户的权限");
        }

        if (targetDept.getLeaderUserId() == null || !targetDept.getLeaderUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("您没有权限在该部门创建用户");
        }
        log.info("用户 {} 在部门 {} (类型: {}) 下创建用户",
            currentUser.getUsername(), targetDept.getDeptName(), targetDeptType);
    }

    /**
     * 检查代理用户是否可以创建下级代理
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateSubAgent(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null || !user.hasRole("AGENT")) {
            return false;
        }

        Integer currentLevel = user.getDept() != null ? user.getDept().getLevel() : null;
        return currentLevel != null && currentLevel < 3; // 1-2级代理可以创建下级代理
    }

    /**
     * 检查代理用户是否可以创建买家总账户
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateBuyerAccount(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null) {
            return false;
        }

        // 系统管理员角色可以创建买家总账户
        if (user.hasRole("ADMIN") || user.hasRole("SYSTEM_ADMIN") ||
            user.hasRole("TECH_ADMIN") || user.hasRole("OPS_ADMIN")) {
            return true;
        }

        // 所有代理角色都可以创建买家总账户（不论层级）
        if (user.hasRole("AGENT")) {
            return true;
        }

        return false;
    }

    /**
     * 检查买家总账户用户是否可以创建子账户
     *
     * @param user 当前用户
     * @return true如果可以创建，false否则
     */
    public boolean canCreateSubAccount(com.deepreach.common.core.domain.model.LoginUser user) {
        if (user == null || !user.hasRole("BUYER_MAIN")) {
            return false;
        }

        // 买家总账户都可以创建子账户
        return true;
    }

    /**
     * 设置注册用户默认值
     */
    private void setDefaultValuesForRegister(SysUser user) {
        setDefaultValues(user);
        user.setUserType(2); // 注册用户默认为客户端用户
    }

    /**
     * 应用数据权限过滤
     */
    private void applyDataPermissionFilter(SysUser user) {
        // 获取当前用户可访问的部门ID列表
        List<Long> accessibleDeptIds = deptService.getAccessibleDeptIds();
        if (!accessibleDeptIds.isEmpty()) {
            // 这里可以设置查询条件中的部门过滤
            // 具体实现取决于查询方式
        }
    }

    /**
     * 获取查询条件描述
     */
    private String getQueryCondition(SysUser user) {
        if (user == null) {
            return "无条件";
        }

        StringBuilder sb = new StringBuilder();
        if (user.getUsername() != null) {
            sb.append("用户名:").append(user.getUsername()).append(",");
        }
        if (user.getNickname() != null) {
            sb.append("昵称:").append(user.getNickname()).append(",");
        }
        if (user.getStatus() != null) {
            sb.append("状态:").append(user.getStatus()).append(",");
        }
        if (user.getDeptId() != null) {
            sb.append("部门ID:").append(user.getDeptId()).append(",");
        }

        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "无条件";
    }

    /**
     * 检查用户数据权限
     */
    @Override
    public boolean hasUserDataPermission(Long targetUserId) {
        // 超级管理员拥有所有权限
        LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
        if (currentUser != null && currentUser.isAdmin()) {
            return true;
        }

        // 只能管理自己（如果普通用户）
        if (currentUser != null && targetUserId.equals(currentUser.getUserId())) {
            return true;
        }

        // 检查用户管理权限
        return SecurityUtils.hasPermission("system:user:edit");
    }

    // ==================== 其他业务方法实现 ====================

    @Override
    public java.util.Map<String, Object> getUserStatistics(Long userId) {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        // TODO: 实现用户统计逻辑
        return statistics;
    }

    @Override
    public List<Long> getAccessibleUserIds() {
        // TODO: 实现可访问用户ID获取逻辑
        return new ArrayList<>();
    }

    @Override
    public boolean updateUserAvatar(Long userId, String avatarUrl) throws Exception {
        // TODO: 实现头像更新逻辑
        return false;
    }

    @Override
    public boolean updateUserInfo(Long userId, SysUser user) throws Exception {
        // TODO: 实现用户信息更新逻辑
        return false;
    }

    @Override
    public java.util.Map<String, Object> importUsers(List<SysUser> users, boolean updateSupport) throws Exception {
        // TODO: 实现用户导入逻辑
        return new java.util.HashMap<>();
    }

    @Override
    public byte[] exportUsers(List<SysUser> users) throws Exception {
        // TODO: 实现用户导出逻辑
        return new byte[0];
    }

    @Override
    public boolean isUserOnline(Long userId) {
        // TODO: 实现用户在线状态检查逻辑
        return false;
    }

    @Override
    public boolean forceUserOffline(Long userId) throws Exception {
        // TODO: 实现强制用户下线逻辑
        return false;
    }

    // ==================== 账户体系业务方法实现 ====================

    
    
    /**
     * 根据父用户ID查询子账号列表
     */
    @Override
    public List<SysUser> selectSubAccountsByParentUserId(Long parentUserId) {
        if (parentUserId == null || parentUserId <= 0) {
            log.warn("查询子账号失败：父用户ID无效 - {}", parentUserId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectSubAccountsByParentUserId(parentUserId);
            log.debug("查询子账号成功：父用户ID={}, 结果数量={}", parentUserId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询子账号异常：父用户ID={}", parentUserId, e);
            throw new RuntimeException("查询子账号失败", e);
        }
    }

    /**
     * 查询下级用户列表（根据父用户ID）
     */
    @Override
    public List<SysUser> selectSubUsersByParentId(Long parentId) {
        if (parentId == null || parentId <= 0) {
            log.warn("查询下级用户失败：父用户ID无效 - {}", parentId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectSubUsersByParentId(parentId);
            log.debug("查询下级用户成功：父用户ID={}, 结果数量={}", parentId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询下级用户异常：父用户ID={}", parentId, e);
            throw new RuntimeException("查询下级用户失败", e);
        }
    }

    /**
     * 查询买家总账号及其子账号
     */
    @Override
    public List<SysUser> selectBuyerAccountTree(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("查询买家账号树失败：用户ID无效 - {}", userId);
            return new ArrayList<>();
        }

        try {
            List<SysUser> userList = userMapper.selectBuyerAccountTree(userId);
            log.debug("查询买家账号树成功：用户ID={}, 结果数量={}", userId, userList.size());
            return userList;
        } catch (Exception e) {
            log.error("查询买家账号树异常：用户ID={}", userId, e);
            throw new RuntimeException("查询买家账号树失败", e);
        }
    }

    
    
    
    
    
    
    
    
    
    /**
     * 检查用户是否可以创建子账号
     */
    @Override
    public boolean checkCanCreateSubAccount(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }

        try {
            return userMapper.checkCanCreateSubAccount(userId);
        } catch (Exception e) {
            log.error("检查用户是否可以创建子账号异常：用户ID={}", userId, e);
            return false;
        }
    }

    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createBuyerSubAccount(SysUser user, Long parentUserId) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        if (parentUserId == null || parentUserId <= 0) {
            throw new IllegalArgumentException("父用户ID不能为空");
        }

        // 验证父用户是否存在且为买家总账户用户
        SysUser parentUser = selectUserById(parentUserId);
        if (parentUser == null) {
            throw new Exception("父用户不存在");
        }

        // 验证父用户是否为买家总账户用户
        if (!parentUser.isBuyerMainAccountUser()) {
            throw new Exception("父用户必须为买家总账户用户");
        }

        // 验证父用户部门是否为买家总账户部门
        if (parentUser.getDept() == null || !"3".equals(parentUser.getDept().getDeptType())) {
            throw new Exception("父用户必须属于买家总账户部门");
        }

        // 设置父用户ID
        user.setParentUserId(parentUserId);

        // 验证用户部门是否为买家子账户部门
        if (user.getDeptId() != null) {
            SysDept dept = deptService.selectDeptById(user.getDeptId());
            if (dept == null) {
                throw new Exception("指定的部门不存在");
            }

            if (!"4".equals(dept.getDeptType())) {
                throw new Exception("用户必须属于买家子账户部门");
            }
        }

        // 设置用户类型为客户端用户
        user.setUserType(2);

        // 设置默认状态
        user.setStatus("0");

        // 插入用户
        SysUser result = insertUser(user);
        if (result == null) {
            throw new Exception("创建买家子账户失败");
        }

        log.info("创建买家子账户成功：用户ID={}, 父用户ID={}, 用户名={}",
                user.getUserId(), parentUserId, user.getUsername());

        return user;
    }

    @Override
    public java.util.Map<String, Object> getUserOrgInfo(Long userId) {
        java.util.Map<String, Object> orgInfo = new java.util.HashMap<>();

        if (userId == null || userId <= 0) {
            return orgInfo;
        }

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 检查用户数据权限
            if (!hasUserDataPermission(userId)) {
                throw new SecurityException("无权访问该用户的组织信息");
            }
            SysUser user = selectUserById(userId);
            if (user == null) {
                return orgInfo;
            }

            // 基本用户信息
            orgInfo.put("userId", user.getUserId());
            orgInfo.put("username", user.getUsername());
            orgInfo.put("nickname", user.getNickname());
            orgInfo.put("realName", user.getRealName());
            orgInfo.put("email", user.getEmail());
            orgInfo.put("phone", user.getPhone());
            orgInfo.put("status", user.getStatus());
            orgInfo.put("createTime", user.getCreateTime());

            // 组织架构信息
            if (user.getDeptId() != null) {
                orgInfo.put("deptId", user.getDeptId());

                SysDept dept = deptService.selectDeptById(user.getDeptId());
                if (dept != null) {
                    orgInfo.put("deptName", dept.getDeptName());
                    orgInfo.put("deptType", dept.getDeptType());
                    orgInfo.put("deptTypeDisplay", dept.getDeptTypeDisplay());
                    orgInfo.put("level", dept.getLevel());
                    orgInfo.put("levelDisplay", dept.getLevelDisplay());

                    // 获取部门层级路径
                    orgInfo.put("ancestors", dept.getAncestors());
                    orgInfo.put("fullPath", dept.getFullPath());
                }
            }

            // 父用户信息（买家子账户）
            if (user.getParentUserId() != null && user.getParentUserId() > 0) {
                orgInfo.put("parentUserId", user.getParentUserId());

                SysUser parentUser = selectUserById(user.getParentUserId());
                if (parentUser != null) {
                    orgInfo.put("parentUserName", parentUser.getNickname() != null ?
                        parentUser.getNickname() : parentUser.getUsername());
                }
            }

            // 用户类型判断
            orgInfo.put("isSystemDeptUser", user.isSystemDeptUser());
            orgInfo.put("isAgentDeptUser", user.isAgentDeptUser());
            orgInfo.put("isBuyerMainAccountUser", user.isBuyerMainAccountUser());
            orgInfo.put("isBuyerSubAccountUser", user.isBuyerSubAccountUser());
            orgInfo.put("isBuyerUser", user.isBuyerUser());
            orgInfo.put("isBackendUser", user.isBackendUser());
            orgInfo.put("isFrontendUser", user.isClientUser());
            orgInfo.put("hasParentUser", user.hasParentUser());

        } catch (Exception e) {
            log.error("获取用户组织架构信息异常：用户ID={}", userId, e);
        }

        return orgInfo;
    }

    @Override
    public java.util.Map<String, Object> getUserDeptTypeStatistics(Long deptId) throws Exception {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 排除买家子账户用户（客户端用户无权查看统计信息）
            SysUser currentUserObj = getCurrentUser();
            if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
                throw new SecurityException("客户端用户无权查看统计信息");
            }

            // 检查部门数据权限
            if (!deptService.hasDeptDataPermission(deptId)) {
                throw new SecurityException("无权访问该部门的统计信息");
            }

            // 获取指定部门信息
            SysDept dept = deptService.selectDeptById(deptId);
            if (dept == null) {
                return statistics;
            }

            // 当前部门基本信息
            statistics.put("deptId", dept.getDeptId());
            statistics.put("deptName", dept.getDeptName());
            statistics.put("deptType", dept.getDeptType());
            statistics.put("deptTypeDisplay", dept.getDeptTypeDisplay());
            statistics.put("level", dept.getLevel());
            statistics.put("levelDisplay", dept.getLevelDisplay());
            statistics.put("ancestors", dept.getAncestors());

            // 统计当前部门的用户（排除买家子账户）
            List<SysUser> deptUsers = selectUsersByDeptId(deptId);
            java.util.List<SysUser> managementUsers = new java.util.ArrayList<>();

            for (SysUser user : deptUsers) {
                // 排除买家子账户用户（deptType = "4"）
                if (!user.isBuyerSubAccountUser()) {
                    managementUsers.add(user);
                }
            }

            statistics.put("currentDeptUserCount", managementUsers.size());

            // 按用户类型统计当前部门
            java.util.Map<String, Integer> currentUserTypeCount = new java.util.HashMap<>();
            for (SysUser user : managementUsers) {
                String userType = user.getUserTypeDisplay();
                currentUserTypeCount.put(userType, currentUserTypeCount.getOrDefault(userType, 0) + 1);
            }
            statistics.put("currentUserTypeStatistics", currentUserTypeCount);

            // 递归统计子部门
            List<SysDept> childDepts = deptService.selectChildrenByParentId(deptId);
            statistics.put("childDeptCount", childDepts.size());

            java.util.List<java.util.Map<String, Object>> childStatistics = new java.util.ArrayList<>();
            java.util.Map<String, Integer> overallDeptTypeCount = new java.util.HashMap<>();
            java.util.Map<String, Integer> overallUserTypeCount = new java.util.HashMap<>();
            int totalUsers = managementUsers.size();
            int totalDepts = 1; // 包括当前部门

            // 当前部门类型计数
            String currentDeptTypeDisplay = dept.getDeptTypeDisplay();
            overallDeptTypeCount.put(currentDeptTypeDisplay, overallDeptTypeCount.getOrDefault(currentDeptTypeDisplay, 0) + 1);

            // 合并当前部门的用户类型统计
            for (java.util.Map.Entry<String, Integer> entry : currentUserTypeCount.entrySet()) {
                overallUserTypeCount.put(entry.getKey(), overallUserTypeCount.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }

            // 递归处理子部门
            for (SysDept childDept : childDepts) {
                java.util.Map<String, Object> childStats = getUserDeptTypeStatistics(childDept.getDeptId());
                childStatistics.add(childStats);

                // 累加统计信息
                totalUsers += (Integer) childStats.getOrDefault("totalUsers", 0);
                totalDepts += (Integer) childStats.getOrDefault("totalDepts", 0);

                // 合并部门类型统计
                @SuppressWarnings("unchecked")
                java.util.Map<String, Integer> childDeptTypeStats = (java.util.Map<String, Integer>) childStats.get("deptTypeStatistics");
                if (childDeptTypeStats != null) {
                    for (java.util.Map.Entry<String, Integer> entry : childDeptTypeStats.entrySet()) {
                        overallDeptTypeCount.put(entry.getKey(), overallDeptTypeCount.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }

                // 合并用户类型统计
                @SuppressWarnings("unchecked")
                java.util.Map<String, Integer> childUserTypeStats = (java.util.Map<String, Integer>) childStats.get("userTypeStatistics");
                if (childUserTypeStats != null) {
                    for (java.util.Map.Entry<String, Integer> entry : childUserTypeStats.entrySet()) {
                        overallUserTypeCount.put(entry.getKey(), overallUserTypeCount.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }
            }

            statistics.put("totalUsers", totalUsers);
            statistics.put("totalDepts", totalDepts);
            statistics.put("deptTypeStatistics", overallDeptTypeCount);
            statistics.put("userTypeStatistics", overallUserTypeCount);
            statistics.put("childDepartments", childStatistics);

            // 业务能力信息
            statistics.put("canCreateChildDept", dept.canCreateChildDept());
            statistics.put("canCreateBuyerAccount", dept.canCreateBuyerAccount());
            statistics.put("canCreateSubAccount", dept.canCreateSubAccount());

        } catch (Exception e) {
            log.error("获取部门类型统计信息异常：部门ID={}", deptId, e);
            throw new Exception("获取部门类型统计信息失败：" + e.getMessage(), e);
        }

        return statistics;
    }

    @Override
    public boolean checkCanCreateChildDept(Long userId) throws Exception {
        if (userId == null || userId <= 0) {
            return false;
        }

        try {
            SysUser user = selectUserById(userId);
            if (user == null || user.getDept() == null) {
                return false;
            }

            return user.getDept().canCreateChildDept();

        } catch (Exception e) {
            log.error("检查用户是否可以创建下级部门异常：用户ID={}", userId, e);
            return false;
        }
    }

    @Override
    public List<SysUser> selectUsersByDeptAndChildren(Long deptId) throws Exception {
        List<SysUser> result = new ArrayList<>();

        if (deptId == null || deptId <= 0) {
            return result;
        }

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 检查部门数据权限
            if (!deptService.hasDeptDataPermission(deptId)) {
                throw new SecurityException("无权访问该部门的用户信息");
            }

            // 获取当前部门的用户
            List<SysUser> currentDeptUsers = selectUsersByDeptId(deptId);
            result.addAll(currentDeptUsers);

            // 递归获取子部门的用户
            List<SysDept> childDepts = deptService.selectChildrenByParentId(deptId);
            for (SysDept childDept : childDepts) {
                List<SysUser> childUsers = selectUsersByDeptAndChildren(childDept.getDeptId());
                result.addAll(childUsers);
            }

        } catch (Exception e) {
            log.error("查询部门及子部门用户异常：部门ID={}", deptId, e);
            throw new Exception("查询部门及子部门用户失败：" + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 获取当前登录用户的SysUser对象
     *
     * 由于LoginUser中没有getUser()方法，通过userId查询数据库获取完整的SysUser信息
     *
     * @return 当前登录用户的SysUser对象，如果用户不存在则返回null
     */
    private SysUser getCurrentUser() {
        LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }

        return selectUserWithDept(loginUser.getUserId());
    }

    @Override
    public SysUser selectUserWithDept(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        try {
            return userMapper.selectUserWithDept(userId);
        } catch (Exception e) {
            log.error("查询用户完整信息异常：用户ID={}", userId, e);
            return null;
        }
    }

    @Override
    public List<SysUser> selectUsersByAgentLevel(Integer level) throws Exception {
        List<SysUser> result = new ArrayList<>();

        if (level == null || level <= 0) {
            return result;
        }

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 排除买家子账户用户
            SysUser currentUserObj = getCurrentUser();
            if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
                throw new SecurityException("客户端用户无权查看代理用户信息");
            }

            // 获取所有指定层级的代理部门
            SysDept queryDept = new SysDept();
            queryDept.setDeptType("2"); // 代理部门
            queryDept.setLevel(level);
            List<SysDept> agentDepts = deptService.selectDeptList(queryDept);

            // 获取这些部门下的所有用户
            for (SysDept agentDept : agentDepts) {
                List<SysUser> deptUsers = selectUsersByDeptId(agentDept.getDeptId());
                result.addAll(deptUsers);
            }

        } catch (Exception e) {
            log.error("查询指定层级代理用户异常：层级={}", level, e);
            throw new Exception("查询指定层级代理用户失败：" + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public List<SysUser> selectUsersByDeptType(String deptType) throws Exception {
        List<SysUser> result = new ArrayList<>();

        if (deptType == null || deptType.trim().isEmpty()) {
            return result;
        }

        try {
            // ===== 权限验证 =====
            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                throw new SecurityException("用户未登录");
            }

            // 排除买家子账户用户
            SysUser currentUserObj = getCurrentUser();
            if (currentUserObj != null && currentUserObj.isBuyerSubAccountUser()) {
                throw new SecurityException("客户端用户无权查看部门类型统计信息");
            }

            return userMapper.selectUsersByDeptType(deptType);

        } catch (Exception e) {
            log.error("查询指定部门类型用户异常：部门类型={}", deptType, e);
            throw new Exception("查询指定部门类型用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 根据部门类型自动分配角色
     *
     * @param user 已创建的用户对象
     */
    private void assignRoleByDeptType(SysUser user) throws Exception {
        if (user == null || user.getUserId() == null || user.getDeptId() == null) {
            log.warn("用户信息不完整，无法自动分配角色：用户ID={}, 部门ID={}",
                user != null ? user.getUserId() : null,
                user != null ? user.getDeptId() : null);
            return;
        }

        try {
            // 获取部门信息
            SysDept dept = deptService.selectDeptById(user.getDeptId());
            if (dept == null) {
                log.warn("部门不存在，无法自动分配角色：部门ID={}", user.getDeptId());
                return;
            }

            String roleKey = getRoleKeyByDept(dept);

            if (roleKey != null) {
                // 查找角色ID
                Long roleId = roleMapper.selectRoleIdByKey(roleKey);
                if (roleId != null) {
                    // 分配角色
                    userMapper.insertUserRole(user.getUserId(), roleId);
                    log.info("自动分配角色成功：用户ID={}, 部门类型={}, 部门层级={}, 角色Key={}",
                        user.getUserId(), dept.getDeptType(), dept.getLevel(), roleKey);
                } else {
                    log.warn("角色不存在，无法自动分配：角色Key={}", roleKey);
                }
            } else {
                log.warn("未知部门类型，无法自动分配角色：部门类型={}", dept.getDeptType());
            }

        } catch (Exception e) {
            log.error("根据部门类型自动分配角色异常：用户ID={}, 部门ID={}",
                user.getUserId(), user.getDeptId(), e);
            // 不抛出异常，避免影响用户创建流程
        }
    }

    /**
     * 设置简化的用户信息（用于前端显示）
     *
     * @param user 用户对象
     */
    private void setSimplifiedUserInfo(SysUser user) {
        try {
            // 查询用户的角色信息
            Set<SysRole> roles = userMapper.selectRolesByUserId(user.getUserId());
            if (roles != null && !roles.isEmpty()) {
                // 直接设置角色标识列表
                Set<String> roleIdentifiers = roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toSet());
                user.setRoles(roleIdentifiers);
            }

            // 设置部门显示名称
            if (user.getDept() != null) {
                user.setDeptDisplayName(user.getDept().getDeptName());
            } else {
                // 如果部门信息为空，根据部门ID查询
                if (user.getDeptId() != null) {
                    SysDept dept = deptService.selectDeptById(user.getDeptId());
                    if (dept != null) {
                        user.setDeptDisplayName(dept.getDeptName());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("设置简化用户信息失败：用户ID={}", user.getUserId(), e);
        }
    }

    /**
     * 根据部门信息获取对应的角色Key
     *
     * @param dept 部门信息
     * @return 角色Key
     */
    private String getRoleKeyByDept(SysDept dept) {
        if (dept == null) {
            return null;
        }

        String deptType = dept.getDeptType();

        // 部门类型与角色对应关系
        // 1-系统部门 -> admin
        // 2-代理部门 -> agent
        // 3-买家总账户 -> buyer_main
        // 4-买家子账户 -> buyer_sub

        switch (deptType) {
            case "1":
                return "admin";
            case "2":
                return "agent";
            case "3":
                return "buyer_main";
            case "4":
                return "buyer_sub";
            default:
                return null;
        }
    }

    // ==================== 统计方法实现 ====================

    @Override
    public Map<String, Object> getManagedUsersStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            if (userId == null) {
                return statistics;
            }

            // 获取用户管理的所有部门ID
            Set<Long> managedDeptIds = getManagedDeptIdsByUserId(userId);
            if (managedDeptIds.isEmpty()) {
                initializeUserStatistics(statistics, 0L, 0L, 0L, 0L);
                return statistics;
            }

            // 统计各部门用户数量
            List<Map<String, Object>> userStatsList = userMapper.countUsersByDeptIds(managedDeptIds);

            // 初始化计数器
            Long systemUserCount = 0L;
            Long agentUserCount = 0L;
            Long buyerMainUserCount = 0L;
            Long buyerSubUserCount = 0L;

            // 处理查询结果
            for (Map<String, Object> stat : userStatsList) {
                Object userTypeObj = stat.get("user_type");
                String userType = null;

                // 处理user_type字段可能的类型
                if (userTypeObj instanceof String) {
                    userType = (String) userTypeObj;
                } else if (userTypeObj instanceof Number) {
                    userType = String.valueOf(userTypeObj);
                }

                Object countObj = stat.get("count");
                Long count = 0L;

                if (countObj instanceof Number) {
                    count = ((Number) countObj).longValue();
                } else if (countObj instanceof String) {
                    try {
                        count = Long.parseLong((String) countObj);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析用户数量: {}", countObj);
                    }
                }

                if (userType != null) {
                    switch (userType) {
                        case "system_users":
                            systemUserCount = count;
                            break;
                        case "agent_users":
                            agentUserCount = count;
                            break;
                        case "buyer_main_users":
                            buyerMainUserCount = count;
                            break;
                        case "buyer_sub_users":
                            buyerSubUserCount = count;
                            break;
                        default:
                            log.warn("未知的用户类型: {}", userType);
                    }
                }
            }

            // 构建统计结果
            initializeUserStatistics(statistics, systemUserCount, agentUserCount, buyerMainUserCount, buyerSubUserCount);

            statistics.put("totalUsers", systemUserCount + agentUserCount + buyerMainUserCount + buyerSubUserCount);
            statistics.put("managedDeptIds", managedDeptIds);

            log.info("用户 {} 管理的用户统计完成: 系统={}, 代理={}, 买家总={}, 买家子={}",
                    userId, systemUserCount, agentUserCount, buyerMainUserCount, buyerSubUserCount);

        } catch (Exception e) {
            log.error("获取管理用户统计信息失败：userId={}", userId, e);
            initializeUserStatistics(statistics, 0L, 0L, 0L, 0L);
        }

        return statistics;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户管理的所有部门ID
     */
    private Set<Long> getManagedDeptIdsByUserId(Long userId) {
        Set<Long> managedDeptIds = new HashSet<>();

        // 查询用户作为负责人的部门
        List<SysDept> managedDepts = deptService.selectDeptsByLeaderUserId(userId);
        for (SysDept dept : managedDepts) {
            managedDeptIds.add(dept.getDeptId());
            // 递归获取子部门ID
            List<Long> childIds = deptService.selectChildDeptIds(dept.getDeptId());
            managedDeptIds.addAll(childIds);
        }

        return managedDeptIds;
    }

    /**
     * 初始化用户统计信息
     */
    private void initializeUserStatistics(Map<String, Object> statistics,
                                        Long systemUsers, Long agentUsers, Long buyerMainUsers, Long buyerSubUsers) {
        statistics.put("systemUserCount", systemUsers);
        statistics.put("agentUserCount", agentUsers);
        statistics.put("buyerMainUserCount", buyerMainUsers);
        statistics.put("buyerSubUserCount", buyerSubUsers);
    }
}
