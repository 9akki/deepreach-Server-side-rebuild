package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;
import com.deepreach.common.core.service.UserHierarchyService;
import com.deepreach.common.security.enums.UserIdentity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 数据权限计算器（基于用户层级）。
 */
@Service
@RequiredArgsConstructor
public class DataScopeCalculator {

    private static final Logger log = LoggerFactory.getLogger(DataScopeCalculator.class);

    private final UserHierarchyService hierarchyService;

    /**
     * 判断用户是否拥有完整数据访问权限。
     */
    public boolean hasFullAccess(LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }
        return loginUser.isSuperAdmin() || loginUser.isAdminIdentity();
    }

    /**
     * 计算当前用户可以访问的用户ID集合（包含自身及层级下属）。
     */
    public Set<Long> calculateAccessibleUserIds(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            log.warn("无法计算数据权限：登录用户为空");
            return Collections.emptySet();
        }

        Set<Long> scope = new LinkedHashSet<>();
        scope.add(loginUser.getUserId());
        scope.addAll(hierarchyService.findDescendantIds(loginUser.getUserId()));
        return scope;
    }
}
