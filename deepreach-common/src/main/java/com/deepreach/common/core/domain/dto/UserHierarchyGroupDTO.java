package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于用户层级和身份的分组结果，用于替代原部门分组展示。
 */
@Data
public class UserHierarchyGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 身份分组的唯一标识（例如 admin、agent_level_1 等）。
     */
    private String identityKey;

    /**
     * 身份分组的显示名称（中文描述）。
     */
    private String identityLabel;

    /**
     * 分组下的用户摘要信息。
     */
    private List<UserSummary> users = new ArrayList<>();

    @Data
    public static class UserSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;
        private String username;
        private String nickname;
        private String realName;
        private String phone;
        private String email;
        private String status;
        private Long parentUserId;
        private Set<String> roleKeys = new LinkedHashSet<>();
        private String primaryIdentity;
    }
}
