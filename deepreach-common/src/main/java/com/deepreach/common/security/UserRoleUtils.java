package com.deepreach.common.security;

import com.deepreach.common.security.enums.UserIdentity;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 用户角色/身份辅助工具。
 */
public final class UserRoleUtils {

    private UserRoleUtils() {
    }

    /**
     * 将角色标识集合转换为身份集合。
     *
     * @param roleKeys 角色标识集合
     * @return 身份集合（不可变）
     */
    public static Set<UserIdentity> resolveIdentities(Collection<String> roleKeys) {
        if (roleKeys == null || roleKeys.isEmpty()) {
            return Collections.emptySet();
        }
        EnumSet<UserIdentity> identities = EnumSet.noneOf(UserIdentity.class);
        for (String roleKey : roleKeys) {
            UserIdentity.fromRoleKey(roleKey).ifPresent(identities::add);
        }
        return Collections.unmodifiableSet(identities);
    }

    /**
     * 判断是否包含指定身份。
     *
     * @param roleKeys 角色标识集合
     * @param identity 目标身份
     * @return 是否匹配
     */
    public static boolean hasIdentity(Collection<String> roleKeys, UserIdentity identity) {
        if (identity == null || roleKeys == null || roleKeys.isEmpty()) {
            return false;
        }
        String targetKey = identity.getRoleKey();
        for (String roleKey : roleKeys) {
            if (Objects.equals(targetKey, roleKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含任一身份。
     *
     * @param roleKeys 角色标识集合
     * @param identities 身份数组
     * @return 是否匹配任一身份
     */
    public static boolean hasAnyIdentity(Collection<String> roleKeys, UserIdentity... identities) {
        if (identities == null || identities.length == 0 || roleKeys == null || roleKeys.isEmpty()) {
            return false;
        }
        for (UserIdentity identity : identities) {
            if (hasIdentity(roleKeys, identity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含全部身份。
     *
     * @param roleKeys 角色标识集合
     * @param identities 身份数组
     * @return 是否全部匹配
     */
    public static boolean hasAllIdentities(Collection<String> roleKeys, UserIdentity... identities) {
        if (identities == null || identities.length == 0) {
            return false;
        }
        for (UserIdentity identity : identities) {
            if (!hasIdentity(roleKeys, identity)) {
                return false;
            }
        }
        return true;
    }
}
