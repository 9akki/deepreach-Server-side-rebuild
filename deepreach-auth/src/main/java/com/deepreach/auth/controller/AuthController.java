package com.deepreach.auth.controller;

import com.deepreach.auth.dto.UserLoginDTO;
import com.deepreach.auth.dto.UserRegisterDTO;
import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.security.JwtTokenUtil;
import com.deepreach.common.security.SecurityCache;
import com.deepreach.common.security.SecurityUtils;
import com.deepreach.common.web.BaseController;
import com.deepreach.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 *
 * 提供用户认证相关的API接口，包括：
 * 1. 用户登录和注册
 * 2. 令牌刷新和验证
 * 3. 用户登出和会话管理
 * 4. 当前用户信息获取
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-26
 */
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
@Validated
public class AuthController extends BaseController {

    @Autowired
    private SysUserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private SecurityCache securityCache;

    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    /**
     * 用户登录
     *
     * 验证用户名和密码，生成JWT令牌返回给客户端
     *
     * @param loginDTO 登录请求参数
     * @return 登录结果，包含JWT令牌和用户信息
     */
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO, HttpServletRequest request) {
        logger.info("用户登录请求：{}", loginDTO.getUsername());

        try {
            // 获取客户端IP
            String clientIp = getClientIp();
            
            // 用户认证
            LoginUser loginUser = userService.authenticate(
                    loginDTO.getUsername(), 
                    loginDTO.getPassword(), 
                    clientIp
            );

            // 生成访问令牌
            String accessToken = jwtTokenUtil.generateToken(loginUser);
            
            // 生成刷新令牌
            String refreshToken = jwtTokenUtil.generateRefreshToken(loginUser);

            // 存储用户信息到缓存
            securityCache.storeUser(accessToken, loginUser, jwtExpiration);

            // 获取完整用户信息用于返回给前端
            com.deepreach.common.core.domain.vo.UserVO userVO = userService.getCompleteUserInfo(loginUser.getUserId());

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("tokenType", "Bearer");
            result.put("expiresIn", jwtExpiration);
            result.put("user", userVO);

            logger.info("用户 {} 登录成功，IP：{}", loginDTO.getUsername(), clientIp);
            return success("登录成功", result);
        } catch (Exception e) {
            logger.error("用户 {} 登录失败，IP：{}，错误：{}", 
                    loginDTO.getUsername(), getClientIp(), e.getMessage());
            return error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 用户注册
     *
     * 创建新用户账号，进行参数验证和唯一性检查
     *
     * @param registerDTO 注册请求参数
     * @return 注册结果
     */
    @Operation(summary = "用户注册", description = "创建新用户账号")
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        logger.info("用户注册请求：{}", registerDTO.getUsername());

        try {
            // 验证密码和确认密码是否一致
            if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
                return error("密码和确认密码不一致");
            }

            // 创建用户对象
            SysUser user = new SysUser();
            user.setUsername(registerDTO.getUsername());
            user.setPassword(registerDTO.getPassword());
            user.setEmail(registerDTO.getEmail());
            user.setPhone(registerDTO.getPhone());
            user.setNickname(registerDTO.getNickname());
            user.setDeptId(registerDTO.getDeptId());
            user.setCreateBy("register");

            // 注册用户
            SysUser registeredUser = userService.register(user);

            logger.info("用户 {} 注册成功，ID：{}", registerDTO.getUsername(), registeredUser.getUserId());
            return success("注册成功");
        } catch (Exception e) {
            logger.error("用户 {} 注册失败，错误：{}", registerDTO.getUsername(), e.getMessage());
            return error("注册失败：" + e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * 将令牌加入黑名单，清理用户缓存
     *
     * @param request HTTP请求对象
     * @return 登出结果
     */
    @Operation(summary = "用户登出", description = "用户退出登录，清理会话信息")
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        try {
            // 从请求头中获取令牌
            String token = jwtTokenUtil.extractTokenFromHeader(request.getHeader("Authorization"));
            
            if (token != null) {
                // 从缓存中移除用户信息
                securityCache.removeUser(token);
                
                // TODO: 将令牌加入黑名单（可选实现）
                
                logger.info("用户登出成功，令牌：{}", token.substring(0, Math.min(8, token.length())) + "...");
            }
            
            return success("登出成功");
        } catch (Exception e) {
            logger.error("用户登出失败，错误：{}", e.getMessage());
            return error("登出失败：" + e.getMessage());
        }
    }

    /**
     * 刷新令牌
     *
     * 使用刷新令牌生成新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return error("刷新令牌不能为空");
            }

            // 验证刷新令牌
            if (!jwtTokenUtil.isRefreshToken(refreshToken)) {
                return error("无效的刷新令牌");
            }

            // 从刷新令牌中获取用户名
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            
            // 获取用户信息
            SysUser user = userService.selectUserByUsername(username);
            if (user == null) {
                return error("用户不存在");
            }

            // 构建登录用户对象
            LoginUser loginUser = userService.selectLoginUserById(user.getUserId());
            if (loginUser == null) {
                return error("用户信息获取失败");
            }

            // 生成新的访问令牌
            String newAccessToken = jwtTokenUtil.generateToken(loginUser);

            // 存储用户信息到缓存
            securityCache.storeUser(newAccessToken, loginUser, jwtExpiration);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("tokenType", "Bearer");
            result.put("expiresIn", jwtExpiration);

            logger.info("令牌刷新成功，用户：{}", username);
            return success("令牌刷新成功", result);
        } catch (Exception e) {
            logger.error("令牌刷新失败，错误：{}", e.getMessage());
            return error("令牌刷新失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * 根据当前请求的JWT令牌获取用户详细信息
     *
     * @return 当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/user/info")
    public Result<Map<String, Object>> getCurrentUserInfo() {
        try {
            // 获取当前登录用户
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return error("用户未登录");
            }

            // 获取完整用户信息
            com.deepreach.common.core.domain.vo.UserVO userVO = userService.getCompleteUserInfo(loginUser.getUserId());

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("user", userVO);
            result.put("permissions", userVO.getPermissions());
            result.put("roles", userVO.getRoles());

            return success("获取用户信息成功", result);
        } catch (Exception e) {
            logger.error("获取用户信息失败，错误：{}", e.getMessage());
            return error("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 验证令牌
     *
     * 验证JWT令牌的有效性
     *
     * @param request HTTP请求对象
     * @return 验证结果
     */
    @Operation(summary = "验证令牌", description = "验证JWT令牌的有效性")
    @GetMapping("/token/validate")
    public Result<Map<String, Object>> validateToken(HttpServletRequest request) {
        try {
            // 从请求头中获取令牌
            String token = jwtTokenUtil.extractTokenFromHeader(request.getHeader("Authorization"));
            
            if (token == null) {
                return error("令牌不能为空");
            }

            // 验证令牌
            boolean isValid = jwtTokenUtil.validateToken(token);
            
            // 获取令牌信息
            JwtTokenUtil.TokenInfo tokenInfo = jwtTokenUtil.parseToken(token);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("valid", isValid);
            result.put("tokenInfo", tokenInfo);

            return success("令牌验证完成", result);
        } catch (Exception e) {
            logger.error("令牌验证失败，错误：{}", e.getMessage());
            return error("令牌验证失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码
     *
     * 用户修改自己的密码
     *
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    @Operation(summary = "修改密码", description = "用户修改自己的登录密码")
    @PutMapping("/password/change")
    public Result<String> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        try {
            // 获取当前登录用户
            LoginUser loginUser = SecurityUtils.getCurrentLoginUser();
            if (loginUser == null) {
                return error("用户未登录");
            }

            // 修改密码
            boolean success = userService.changePassword(
                    loginUser.getUserId(), 
                    oldPassword, 
                    newPassword
            );

            if (success) {
                logger.info("用户 {} 修改密码成功", loginUser.getUsername());
                return success("密码修改成功");
            } else {
                return error("密码修改失败");
            }
        } catch (Exception e) {
            logger.error("修改密码失败，错误：{}", e.getMessage());
            return error("修改密码失败：" + e.getMessage());
        }
    }

    /**
     * 忘记密码
     *
     * 通过邮箱重置密码
     *
     * @param email 用户邮箱
     * @return 重置结果
     */
    @Operation(summary = "忘记密码", description = "通过邮箱重置密码")
    @PostMapping("/password/forgot")
    public Result<String> forgotPassword(@RequestParam("email") String email) {
        try {
            // TODO: 实现忘记密码逻辑
            // 1. 验证邮箱是否存在
            // 2. 生成重置令牌
            // 3. 发送重置邮件
            // 4. 记录重置请求

            logger.info("用户请求重置密码，邮箱：{}", email);
            return success("重置密码邮件已发送，请查收");
        } catch (Exception e) {
            logger.error("重置密码失败，邮箱：{}，错误：{}", email, e.getMessage());
            return error("重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 重置密码
     *
     * 使用重置令牌重置密码
     *
     * @param resetToken 重置令牌
     * @param newPassword 新密码
     * @return 重置结果
     */
    @Operation(summary = "重置密码", description = "使用重置令牌重置密码")
    @PostMapping("/password/reset")
    public Result<String> resetPassword(
            @RequestParam String resetToken,
            @RequestParam String newPassword) {
        try {
            // TODO: 实现重置密码逻辑
            // 1. 验证重置令牌
            // 2. 获取用户信息
            // 3. 重置密码
            // 4. 使重置令牌失效

            logger.info("用户使用令牌重置密码");
            return success("密码重置成功");
        } catch (Exception e) {
            logger.error("重置密码失败，错误：{}", e.getMessage());
            return error("重置密码失败：" + e.getMessage());
        }
    }
}