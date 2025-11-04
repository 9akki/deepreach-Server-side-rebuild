package com.deepreach.common.security.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 系统内置的用户身份枚举，与角色标识(role_key)一一对应。
 */
public enum UserIdentity {

    ADMIN("admin"),
    AGENT_LEVEL_1("agent_level_1"),
    AGENT_LEVEL_2("agent_level_2"),
    AGENT_LEVEL_3("agent_level_3"),
    BUYER_MAIN("buyer_main"),
    BUYER_SUB("buyer_sub");

    private static final Map<String, UserIdentity> ROLE_KEY_INDEX;

    static {
        Map<String, UserIdentity> index = new HashMap<>();
        for (UserIdentity value : UserIdentity.values()) {
            index.put(value.roleKey, value);
        }
        ROLE_KEY_INDEX = Collections.unmodifiableMap(index);
    }

    private final String roleKey;

    UserIdentity(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getRoleKey() {
        return roleKey;
    }

    /**
     * 根据角色标识解析身份枚举。
     *
     * @param roleKey 角色标识
     * @return 身份枚举
     */
    public static Optional<UserIdentity> fromRoleKey(String roleKey) {
        if (roleKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ROLE_KEY_INDEX.get(roleKey));
    }
}
