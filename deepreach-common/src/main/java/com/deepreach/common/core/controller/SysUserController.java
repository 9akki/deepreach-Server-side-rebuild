package com.deepreach.common.core.controller;

import com.deepreach.common.annotation.Log;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.dto.UserHierarchyGroupDTO;
import com.deepreach.common.core.domain.dto.UserListRequest;
import com.deepreach.common.core.domain.dto.UserSummaryResponse;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.page.PageDomain;
import com.deepreach.common.core.page.TableSupport;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.enums.BusinessType;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import com.deepreach.common.utils.StringUtils;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.domain.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.common.core.domain.vo.UserVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统用户Controller
 *
 * 基于部门类型的简化用户管理RESTful API控制器，负责：
 * 1. 用户基本信息管理API
 * 2. 用户认证相关API
 * 3. 基于组织架构的角色权限管理API
 * 4. 用户状态管理API
 * 5. 用户查询和统计API
 *
 * 设计理念：
 * - 部门决定用户类型：用户类型由部门类型自动决定
 * - 简化API接口：移除复杂的业务字段操作
 * - 组织架构优先：基于部门类型和层级进行权限控制
 *
 * @author DeepReach Team
 * @version 2.0
 * @since 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Autowired
    private SysUserService userService;

    // ==================== 查询接口 ====================

    /**
     * 获取用户列表
     *
     * 支持多条件查询和分页
     * 自动应用数据权限过滤
     *
     * @param user 查询条件对象
     * @return 分页用户列表
     */
    @PostMapping("/list")
    // @PreAuthorize("@ss.hasPermi('system:user:list')")
    public TableDataInfo list(@RequestBody(required = false) UserListRequest request) {
        return handleUserList(request);
    }

    private TableDataInfo handleUserList(UserListRequest request) {
        try {
            UserListRequest effective = request != null ? request : new UserListRequest();
            Long requestedParentId = effective.getUserId();
            if (requestedParentId != null && requestedParentId > 0) {
                effective.setRootUserId(null);
            } else {
                Long currentUserId = SecurityUtils.getCurrentUserId();
                if (currentUserId == null || currentUserId <= 0) {
                    LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
                    if (currentUser != null) {
                        currentUserId = currentUser.getUserId();
                    }
                }
                effective.setRootUserId(currentUserId);
            }

            List<SysUser> fullList = userService.searchUsers(effective);
            boolean filterBuyerSubs = requestedParentId == null || requestedParentId <= 0;
            if (filterBuyerSubs && fullList != null && !fullList.isEmpty()) {
                fullList = fullList.stream()
                    .filter(user -> {
                        Set<String> roles = user.getRoles();
                        if (roles == null || roles.isEmpty()) {
                            return true;
                        }
                        return roles.stream()
                            .filter(Objects::nonNull)
                            .map(role -> role.toLowerCase(Locale.ROOT))
                            .noneMatch(role -> "buyer_sub".equals(role));
                    })
                    .collect(Collectors.toList());
            }
            if (fullList == null) {
                fullList = Collections.emptyList();
            }
            int pageNum = effective.getPageNum() != null && effective.getPageNum() > 0 ? effective.getPageNum() : 1;
            int pageSize = effective.getPageSize() != null && effective.getPageSize() > 0 ? effective.getPageSize() : fullList.size();
            List<SysUser> pageList = com.deepreach.common.utils.PageUtils.manualPage(fullList, pageNum, pageSize);

            List<UserSummaryResponse> rows = pageList.stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
            com.deepreach.common.utils.PageUtils.PageState state = com.deepreach.common.utils.PageUtils.getCurrentPageState();
            long total = state != null ? state.getTotal() : fullList.size();
            int responsePageNum = state != null ? state.getPageNum() : pageNum;
            int responsePageSize = state != null ? state.getPageSize() : pageSize;
            com.deepreach.common.utils.PageUtils.clearManualPage();
            return TableDataInfo.success(rows, total, responsePageNum, responsePageSize);
        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            return TableDataInfo.error("查询用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据负责人ID获取其直属部门下的用户，按部门分组
     *
     * @param leaderId 负责人用户ID
     * @return 部门与用户分组信息
     */
    @GetMapping("/leader/{leaderId}/hierarchy-users")
    // @PreAuthorize("@ss.hasPermi('system:user:list')")
    public Result<List<UserHierarchyGroupDTO>> listHierarchyUsersByLeader(@PathVariable("leaderId") Long leaderId) {
        try {
            if (leaderId == null || leaderId <= 0) {
                return Result.error("负责人ID无效");
            }

            LoginUser currentUser = SecurityUtils.getCurrentLoginUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            if (!Objects.equals(currentUser.getUserId(), leaderId)
                && !currentUser.isAdminIdentity()
                && !userService.hasUserDataPermission(leaderId)) {
                return Result.error("无权访问该负责人的层级用户信息");
            }

            List<UserHierarchyGroupDTO> groups = userService.listUsersByLeaderDirectDepts(leaderId);
            return Result.success(groups);
        } catch (Exception e) {
            log.error("查询负责人直属部门用户失败：leaderId={}", leaderId, e);
            return Result.error("查询负责人直属部门用户失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取详细信息
     *
     * 获取用户的完整信息，包括角色和权限
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @GetMapping("/{userId}")
    // @PreAuthorize("@ss.hasPermi('system:user:query')")
    public Result getInfo(@PathVariable Long userId) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限访问该用户信息");
            }

            SysUser user = userService.selectUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 获取用户角色和权限信息
            List<Long> roleIds = userService.getUserRoleIds(userId);
            java.util.Set<String> permissions = userService.getUserPermissions(userId);

            // 构建返回数据
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("user", user);
            data.put("roleIds", roleIds);
            data.put("permissions", permissions);

            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户信息失败：用户ID={}", userId, e);
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * 返回当前登录用户的详细信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/profile")
    public Result getProfile() {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }

            SysUser user = userService.selectUserById(loginUser.getUserId());
            if (user == null) {
                return Result.error("用户信息不存在");
            }

            // 构建用户资料信息
            java.util.Map<String, Object> profile = new java.util.HashMap<>();
            profile.put("user", user);
            profile.put("roles", loginUser.getRoles());
            profile.put("permissions", loginUser.getPermissions());
            profile.put("loginInfo", loginUser.getLoginInfo());

            return Result.success(profile);
        } catch (Exception e) {
            log.error("获取用户资料失败", e);
            return Result.error("获取用户资料失败：" + e.getMessage());
        }
    }

    /**
     * 条件查询部门用户
     *
     * 支持通过部门ID、用户身份（部门类型）、用户账号和注册时间范围筛选
     *
     * @param deptId 部门ID，可选
     * @param identity 用户身份（根据部门类型区分），可选
     * @param username 用户账号，可选
     * @param beginTime 注册时间起始，可选（yyyy-MM-dd）
     * @param endTime 注册时间结束，可选（yyyy-MM-dd）
     * @return 用户列表
     */
    @GetMapping("/dept/query")
    // @PreAuthorize("@ss.hasPermi('system:user:list')")
    public TableDataInfo searchHierarchyUsers(@RequestParam(value = "rootUserId", required = false) Long rootUserId,
                                              @RequestParam(value = "identity", required = false) String identity,
                                              @RequestParam(value = "username", required = false) String username,
                                              @RequestParam(value = "beginTime", required = false) String beginTime,
                                              @RequestParam(value = "endTime", required = false) String endTime) {
        try {
            String resolvedDeptType = resolveDeptType(identity);

            if (rootUserId == null && resolvedDeptType == null) {
                return new TableDataInfo().error("根用户ID与用户身份至少需要提供一个查询条件");
            }

            PageDomain pageDomain = TableSupport.buildPageRequest();
            int pageNum = pageDomain.getPageNum() != null ? pageDomain.getPageNum() : 1;
            int pageSize = pageDomain.getPageSize() != null ? pageDomain.getPageSize() : 10;

            SysUser query = new SysUser();
            String usernameFilter = StringUtils.trimToNull(username);
            if (usernameFilter != null) {
                query.setUsername(usernameFilter);
            }
            String beginTimeFilter = StringUtils.trimToNull(beginTime);
            if (beginTimeFilter != null) {
                query.addParam("beginTime", beginTimeFilter);
            }
            String endTimeFilter = StringUtils.trimToNull(endTime);
            if (endTimeFilter != null) {
                query.addParam("endTime", endTimeFilter);
            }

            PageInfo<SysUser> pageInfo = PageHelper.startPage(pageNum, pageSize)
                    .doSelectPageInfo(() -> userService.selectUsersWithinHierarchy(rootUserId, resolvedDeptType, query));

            List<UserVO> voList = pageInfo.getList().stream()
                    .map(user -> userService.getCompleteUserInfo(user.getUserId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return TableDataInfo.success(voList, pageInfo.getTotal(),
                    pageInfo.getPageNum(), pageInfo.getPageSize());
        } catch (Exception e) {
            log.error("条件查询层级用户失败：rootUserId={}, identity={}", rootUserId, identity, e);
            return new TableDataInfo().error("查询层级用户失败：" + e.getMessage());
        }
    }

    /**
     * 根据部门ID获取部门用户列表
     *
     * 获取指定部门下的用户列表（不包含子部门用户）
     *
     * @param deptId 部门ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页后的部门用户列表
     */
// 移除旧 /dept 接口，改用层级查询方案

    /**
     * 根据商户ID获取下级用户列表
     *
     * 获取指定商户的所有下级用户（parent_id为该商户ID的用户）
     *
     * @param merchantId 商户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页后的下级用户列表
     */
    @GetMapping("/merchant/{merchantId}/sub-users")
    // @PreAuthorize("@ss.hasPermi('system:user:list')")
    public TableDataInfo getSubUsersByMerchantId(@PathVariable("merchantId") Long merchantId,
                                                 @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            if (merchantId == null || merchantId <= 0) {
                return TableDataInfo.error("商户ID无效");
            }

            // 检查数据权限 - 只有能访问该商户的用户才能查看其下级用户
            if (!hasUserDataPermission(merchantId)) {
                return TableDataInfo.error("无权访问该商户的下级用户信息");
            }

            PageInfo<SysUser> pageInfo = PageHelper.startPage(pageNum, pageSize)
                    .doSelectPageInfo(() -> userService.selectSubUsersByParentId(merchantId));

            List<UserVO> voList = pageInfo.getList().stream()
                    .map(user -> userService.getCompleteUserInfo(user.getUserId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return TableDataInfo.success(voList, pageInfo.getTotal(),
                    pageInfo.getPageNum(), pageInfo.getPageSize());
        } catch (Exception e) {
            log.error("查询商户下级用户失败：商户ID={}", merchantId, e);
            return TableDataInfo.error("查询商户下级用户失败：" + e.getMessage());
        }
    }

    /**
     * 将身份标识解析为部门类型编码
     *
     * @param identity 身份标识
     * @return 部门类型编码（1-系统、2-代理、3-买家总账户、4-买家子账户），无法解析返回null
     */
    private String resolveDeptType(String identity) {
        String trimmed = StringUtils.trimToNull(identity);
        if (trimmed == null) {
            return null;
        }

        String normalized = trimmed.toLowerCase();
        switch (normalized) {
            case "1":
            case "system":
            case "system_admin":
            case "admin":
            case "system-admin":
            case "系统":
                return "1";
            case "2":
            case "agent":
            case "agency":
            case "代理":
                return "2";
            case "3":
            case "buyer_main":
            case "buyer-main":
            case "buyer main":
            case "buyer-main-user":
            case "buyer_main_user":
            case "买家总账户":
            case "买家总账号":
                return "3";
            case "4":
            case "buyer_sub":
            case "buyer-sub":
            case "buyer sub":
            case "buyer_sub_user":
            case "buyer-sub-user":
            case "buyer_subaccount":
            case "买家子账户":
            case "买家子账号":
                return "4";
            default:
                return null;
        }
    }

    // ==================== 创建接口 ====================

    /**
     * 创建新用户
     *
     * 管理员创建用户接口
     *
     * @param user 用户对象
     * @return 创建结果
     */
    @PostMapping
    // @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    public Result add(@Validated @RequestBody SysUser user) {
        try {
            // 设置创建者信息
            user.setCreateBy(SecurityUtils.getCurrentUsername());

            SysUser createdUser = userService.insertUser(user);
            return Result.success("创建用户成功", createdUser);
        } catch (Exception e) {
            log.error("创建用户失败：用户名={}", user.getUsername(), e);
            return Result.error("创建用户失败：" + e.getMessage());
        }
    }

    /**
     * 用户注册
     *
     * 公开注册接口，无需权限验证
     *
     * @param user 用户对象
     * @return 注册结果
     */
    @PostMapping("/register")
    @Log(title = "用户注册", businessType = BusinessType.INSERT)
    public Result register(@Validated @RequestBody SysUser user) {
        try {
            // 设置创建者信息
            user.setCreateBy("register");

            SysUser registeredUser = userService.register(user);
            return Result.success("注册成功", registeredUser);
        } catch (Exception e) {
            log.error("用户注册失败：用户名={}", user.getUsername(), e);
            return Result.error("注册失败：" + e.getMessage());
        }
    }

    // ==================== 更新接口 ====================

    /**
     * 更新用户信息
     *
     * 管理员更新用户信息接口
     *
     * @param user 用户对象
     * @return 更新结果
     */
    @PutMapping
    // @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result edit(@Validated @RequestBody SysUser user) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(user.getUserId())) {
                return Result.error("无权限修改该用户信息");
            }

            // 设置更新者信息
            user.setUpdateBy(SecurityUtils.getCurrentUsername());

            boolean success = userService.updateUser(user);
            if (success) {
                return Result.success("更新用户信息成功");
            } else {
                return Result.error("更新用户信息失败");
            }
        } catch (Exception e) {
            log.error("更新用户信息失败：用户ID={}", user.getUserId(), e);
            return Result.error("更新用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新当前用户信息
     *
     * 用户自己更新基本信息接口
     *
     * @param user 用户对象
     * @return 更新结果
     */
    @PutMapping("/profile")
    @Log(title = "用户资料", businessType = BusinessType.UPDATE)
    public Result updateProfile(@Validated @RequestBody SysUser user) {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }

            // 只能更新自己的信息
            user.setUserId(loginUser.getUserId());
            user.setUsername(null); // 不允许修改用户名
            user.setPassword(null); // 不允许修改密码

            boolean success = userService.updateUserInfo(loginUser.getUserId(), user);
            if (success) {
                return Result.success("更新用户资料成功");
            } else {
                return Result.error("更新用户资料失败");
            }
        } catch (Exception e) {
            log.error("更新用户资料失败", e);
            return Result.error("更新用户资料失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户头像
     *
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    @PutMapping("/avatar")
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    public Result updateAvatar(@RequestParam String avatarUrl) {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }

            boolean success = userService.updateUserAvatar(loginUser.getUserId(), avatarUrl);
            if (success) {
                return Result.success("更新头像成功");
            } else {
                return Result.error("更新头像失败");
            }
        } catch (Exception e) {
            log.error("更新用户头像失败", e);
            return Result.error("更新头像失败：" + e.getMessage());
        }
    }

    // ==================== 删除接口 ====================

    /**
     * 删除用户
     *
     * 根据用户ID删除用户
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    // @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public Result remove(@PathVariable("userId") Long userId) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限删除该用户");
            }

            boolean success = userService.deleteUserById(userId);
            if (success) {
                return Result.success("删除用户成功");
            } else {
                return Result.error("删除用户失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败：用户ID={}", userId, e);
            return Result.error("删除用户失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除用户
     *
     * 根据用户ID列表批量删除用户
     *
     * @param userIds 用户ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    // @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public Result removeBatch(@RequestBody List<Long> userIds) {
        try {
            // 过滤有权限删除的用户
            List<Long> validUserIds = userIds.stream()
                    .filter(this::hasUserDataPermission)
                    .collect(java.util.stream.Collectors.toList());

            if (validUserIds.isEmpty()) {
                return Result.error("没有可删除的用户");
            }

            boolean success = userService.deleteUserByIds(validUserIds);
            if (success) {
                return Result.success("批量删除用户成功，删除数量：" + validUserIds.size());
            } else {
                return Result.error("批量删除用户失败");
            }
        } catch (Exception e) {
            log.error("批量删除用户失败：用户IDs={}", userIds, e);
            return Result.error("批量删除用户失败：" + e.getMessage());
        }
    }

    // ==================== 密码管理接口 ====================

    /**
     * 重置用户密码
     *
     * 管理员重置用户密码接口
     *
     * @param userId 用户ID
     * @param password 新密码
     * @return 重置结果
     */
    @PutMapping("/{userId}/reset-password")
    // @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "重置密码", businessType = BusinessType.UPDATE)
    public Result resetPassword(@PathVariable("userId") Long userId, @RequestParam("password") String password) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限重置该用户密码");
            }

            boolean success = userService.resetPassword(userId, password);
            if (success) {
                return Result.success("重置密码成功");
            } else {
                return Result.error("重置密码失败");
            }
        } catch (Exception e) {
            log.error("重置用户密码失败：用户ID={}", userId, e);
            return Result.error("重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码
     *
     * 用户自己修改密码接口
     *
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    @PutMapping("/change-password")
    @Log(title = "修改密码", businessType = BusinessType.UPDATE)
    public Result changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        try {
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return Result.error("用户未登录");
            }

            boolean success = userService.changePassword(loginUser.getUserId(), oldPassword, newPassword);
            if (success) {
                return Result.success("修改密码成功");
            } else {
                return Result.error("修改密码失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error("修改密码失败：" + e.getMessage());
        }
    }

    // ==================== 状态管理接口 ====================

    /**
     * 更新用户状态
     *
     * 启用或停用用户账号
     *
     * @param userId 用户ID
     * @param status 状态（0正常 1停用）
     * @return 更新结果
     */
    @PutMapping("/{userId}/status")
    // @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户状态", businessType = BusinessType.UPDATE)
    public Result updateStatus(@PathVariable("userId") Long userId, @RequestParam("status") String status) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限修改该用户状态");
            }

            boolean success = userService.updateUserStatus(userId, status);
            if (success) {
                String statusText = "0".equals(status) ? "启用" : "停用";
                return Result.success(statusText + "用户成功");
            } else {
                return Result.error("更新用户状态失败");
            }
        } catch (Exception e) {
            log.error("更新用户状态失败：用户ID={}, 状态={}", userId, status, e);
            return Result.error("更新用户状态失败：" + e.getMessage());
        }
    }

    // ==================== 角色管理接口 ====================

    /**
     * 分配用户角色
     *
     * 为用户分配指定的角色列表
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 分配结果
     */
    @PutMapping("/{userId}/roles")
    // @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "分配角色", businessType = BusinessType.GRANT)
    public Result assignRoles(@PathVariable("userId") Long userId, @RequestBody List<Long> roleIds) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限修改该用户角色");
            }

            boolean success = userService.assignUserRoles(userId, roleIds);
            if (success) {
                return Result.success("分配角色成功");
            } else {
                return Result.error("分配角色失败");
            }
        } catch (Exception e) {
            log.error("分配用户角色失败：用户ID={}, 角色IDs={}", userId, roleIds, e);
            return Result.error("分配角色失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @GetMapping("/{userId}/roles")
    // @PreAuthorize("@ss.hasPermi('system:user:query')")
    public Result getUserRoles(@PathVariable("userId") Long userId) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限查看该用户角色");
            }

            List<Long> roleIds = userService.getUserRoleIds(userId);
            return Result.success(roleIds);
        } catch (Exception e) {
            log.error("获取用户角色失败：用户ID={}", userId, e);
            return Result.error("获取用户角色失败：" + e.getMessage());
        }
    }

    // ==================== 导入导出接口 ====================

    /**
     * 导入用户数据
     *
     * @param users 用户列表
     * @param updateSupport 是否支持更新已存在的用户
     * @return 导入结果
     */
    @PostMapping("/import")
    // @PreAuthorize("@ss.hasPermi('system:user:import')")
    @Log(title = "用户导入", businessType = BusinessType.IMPORT)
    public Result importUsers(@RequestBody List<SysUser> users, @RequestParam(defaultValue = "false") boolean updateSupport) {
        try {
            java.util.Map<String, Object> result = userService.importUsers(users, updateSupport);
            return Result.success("导入用户成功", result);
        } catch (Exception e) {
            log.error("导入用户失败", e);
            return Result.error("导入用户失败：" + e.getMessage());
        }
    }

    /**
     * 导出用户数据
     *
     * @param user 查询条件对象
     * @return 导出文件
     */
    @PostMapping("/export")
    // @PreAuthorize("@ss.hasPermi('system:user:export')")
    @Log(title = "用户导出", businessType = BusinessType.EXPORT)
    public void exportUsers(SysUser user, HttpServletResponse response) {
        try {
            List<SysUser> list = userService.selectUserList(user);
            byte[] data = userService.exportUsers(list);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode("用户数据", "UTF-8") + ".xlsx";
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

            // 写入响应
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出用户失败", e);
        }
    }

    // ==================== 统计接口 ====================

    /**
     * 获取用户统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    @GetMapping("/{userId}/statistics")
    // @PreAuthorize("@ss.hasPermi('system:user:query')")
    public Result getUserStatistics(@PathVariable("userId") Long userId) {
        try {
            // 检查数据权限
            if (!hasUserDataPermission(userId)) {
                return Result.error("无权限查看该用户统计信息");
            }

            java.util.Map<String, Object> statistics = userService.getUserStatistics(userId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户统计信息失败：用户ID={}", userId, e);
            return Result.error("获取用户统计信息失败：" + e.getMessage());
        }
    }

    // ==================== 在线管理接口 ====================

    /**
     * 强制用户下线
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/{userId}/force-logout")
    // @PreAuthorize("@ss.hasPermi('monitor:online:forceLogout')")
    @Log(title = "强退用户", businessType = BusinessType.FORCE)
    public Result forceLogout(@PathVariable Long userId) {
        try {
            boolean success = userService.forceUserOffline(userId);
            if (success) {
                return Result.success("强制用户下线成功");
            } else {
                return Result.error("强制用户下线失败");
            }
        } catch (Exception e) {
            log.error("强制用户下线失败：用户ID={}", userId, e);
            return Result.error("强制用户下线失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查用户数据权限
     *
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    private boolean hasUserDataPermission(Long targetUserId) {
        return userService.hasUserHierarchyPermission(targetUserId);
    }

    private UserSummaryResponse toUserSummary(SysUser user) {
        if (user == null) {
            return null;
        }
        UserSummaryResponse response = new UserSummaryResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setUserType(user.getUserType());
        response.setDisplayName(user.getDisplayName());
        response.setInvitationCode(user.getInvitationCode());
        response.setParentUserId(user.getParentUserId());
        response.setParentUsername(user.getParentUsername());
        response.setRemark(user.getRemark());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        response.setLoginTime(user.getLoginTime());
        response.setLoginIp(user.getLoginIp());

        Set<String> roles = user.getRoles() != null ? user.getRoles() : Collections.emptySet();
        response.setRoles(new LinkedHashSet<>(roles));
        response.setIdentities(UserRoleUtils.resolveIdentities(roles).stream()
            .map(UserIdentity::getRoleKey)
            .collect(Collectors.toCollection(LinkedHashSet::new)));

        Set<String> parentRoleKeys = Optional.ofNullable(user.getParentRoles())
            .map(set -> set.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .orElseGet(LinkedHashSet::new);
        response.setParentRoles(parentRoleKeys);
        return response;
    }

}
