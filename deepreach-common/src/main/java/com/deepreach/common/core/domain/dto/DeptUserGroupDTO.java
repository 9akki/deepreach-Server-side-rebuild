package com.deepreach.common.core.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO describing a direct department and the users under it.
 */
@Data
public class DeptUserGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deptId;
    private String deptName;
    private String deptType;
    private Integer level;
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
        private Integer userType;
    }
}
