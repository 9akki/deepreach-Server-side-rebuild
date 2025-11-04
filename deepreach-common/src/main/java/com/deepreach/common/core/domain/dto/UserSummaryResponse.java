package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 精简的用户列表响应 DTO。
 *
 * 仅保留前端列表展示与过滤所需字段，避免把部门相关信息重新暴露。
 */
@Data
public class UserSummaryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String status;
    private Integer userType;
    private String displayName;
    private String invitationCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime loginTime;
    private String loginIp;
    private Set<String> roles = new LinkedHashSet<>();
    private Set<String> identities = new LinkedHashSet<>();
    private Long parentUserId;
    private String parentUsername;
    private Set<String> parentRoles = new LinkedHashSet<>();
    private String remark;
}
