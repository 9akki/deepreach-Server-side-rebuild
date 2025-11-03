package com.deepreach.auth.dto;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.domain.model.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * 登录响应DTO
 *
 * 封装登录成功后返回给客户端的数据结构
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Schema(description = "登录响应")
public class LoginResponseDTO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "令牌过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Schema(description = "用户权限列表")
    private Set<String> permissions;

    @Schema(description = "用户角色列表")
    private Set<String> roles;

    // 构造函数
    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String accessToken, String refreshToken, String tokenType, 
                          Long expiresIn, LoginUser loginUser) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = loginUser != null ? loginUser.toUserInfo() : null;
        this.permissions = loginUser != null ? loginUser.getPermissions() : null;
        this.roles = loginUser != null ? loginUser.getRoles() : null;
    }

    // Getter和Setter方法
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "LoginResponseDTO{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", user=" + user +
                ", permissions=" + permissions +
                ", roles=" + roles +
                '}';
    }
}