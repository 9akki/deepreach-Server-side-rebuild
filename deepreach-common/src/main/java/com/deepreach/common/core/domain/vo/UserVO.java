package com.deepreach.common.core.domain.vo;

import com.deepreach.common.security.UserRoleUtils;
import com.deepreach.common.security.enums.UserIdentity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

/**
 * 极简用户信息视图对象。
 *
 * <p>仅保留前端展示所需的基础信息和角色集合，摒弃部门、层级等耦合字段。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "精简用户视图对象")
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "账号状态")
    private String status;

    @Schema(description = "用户类型")
    private Integer userType;

    @Schema(description = "父用户ID（子账号使用）")
    private Long parentUserId;

    @Schema(description = "用户角色列表")
    private Set<String> roles = Collections.emptySet();

    @Schema(description = "用户权限列表")
    private Set<String> permissions = Collections.emptySet();

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== 便捷方法 ====================

    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        if (realName != null && !realName.trim().isEmpty()) {
            return realName;
        }
        return username != null ? username : "未知用户";
    }

    public boolean isActive() {
        return "0".equals(status);
    }

    public boolean hasIdentity(UserIdentity identity) {
        return UserRoleUtils.hasIdentity(this.roles, identity);
    }

    public boolean isAdminIdentity() {
        return hasIdentity(UserIdentity.ADMIN);
    }

    public boolean isAgentIdentity() {
        return UserRoleUtils.hasAnyIdentity(this.roles,
                UserIdentity.AGENT_LEVEL_1,
                UserIdentity.AGENT_LEVEL_2,
                UserIdentity.AGENT_LEVEL_3);
    }

    public boolean isBuyerMainIdentity() {
        return hasIdentity(UserIdentity.BUYER_MAIN);
    }

    public boolean isBuyerSubIdentity() {
        return hasIdentity(UserIdentity.BUYER_SUB);
    }

    public boolean hasParentUser() {
        return parentUserId != null && parentUserId > 0;
    }
}
