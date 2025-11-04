package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户列表查询请求
 *
 * 提供分页与过滤条件，用于替换原基于部门的查询参数。
 */
@Data
public class UserListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码（默认 1） */
    private Integer pageNum;

    /** 每页大小（默认 10） */
    private Integer pageSize;

    /** 根用户ID（基于用户树的范围过滤） */
    private Long rootUserId;

    /** 身份标识（admin、agent_level_1、buyer_main 等） */
    private String identity;

    /** 精确匹配 - 用户ID */
    private Long userId;

    /** 仅查询直属下级 */
    private Boolean parentOnly;

    /** 精确匹配 - 用户名 */
    private String username;

    /** 模糊匹配 - 昵称 */
    private String nickname;

    /** 精确匹配 - 手机号 */
    private String phone;

    /** 精确匹配 - 邮箱 */
    private String email;

    /** 状态过滤（0 正常，1 停用） */
    private String status;

    /** 用户类型（1 后台用户，2 客户端用户等） */
    private Integer userType;

    /** 指定角色标识列表（intersection） */
    private List<String> roleKeys;

    /** 注册时间起始（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss） */
    private String beginTime;

    /** 注册时间结束 */
    private String endTime;

    /** 关键字（在用户名/昵称/真实姓名内模糊匹配） */
    private String keyword;
}
