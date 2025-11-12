package com.deepreach.common.core.support;

import com.deepreach.common.core.domain.entity.SysUser;
import com.deepreach.common.core.service.SysUserService;
import com.deepreach.common.exception.ServiceException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 统一解析扣费账号：支持商家总账号 & 子账户（扣其父账号）。
 */
@Component
@RequiredArgsConstructor
public class ChargeAccountResolver {

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000;

    private final SysUserService sysUserService;
    private final Map<Long, CachedChargeAccount> cache = new ConcurrentHashMap<>();

    public ChargeAccount resolve(Long requestUserId) {
        if (requestUserId == null || requestUserId <= 0) {
            throw new ServiceException("用户ID不能为空");
        }
        ChargeAccount cached = getCachedAccount(requestUserId);
        if (cached != null) {
            return cached;
        }
        ChargeAccount computed = resolveInternal(requestUserId);
        cache.put(requestUserId, new CachedChargeAccount(computed));
        return computed;
    }

    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private ChargeAccount getCachedAccount(Long userId) {
        CachedChargeAccount cached = cache.get(userId);
        if (cached == null || cached.isExpired()) {
            cache.remove(userId, cached);
            return null;
        }
        return cached.account;
    }

    private ChargeAccount resolveInternal(Long requestUserId) {
        SysUser user = sysUserService.selectUserWithDept(requestUserId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (user.isBuyerMainIdentity()) {
            return new ChargeAccount(user.getUserId(), user.getUserId(), true);
        }
        if (user.isBuyerSubIdentity()) {
            Long parentUserId = user.getParentUserId();
            if (parentUserId == null) {
                throw new ServiceException("子账户未绑定商家总账号");
            }
            SysUser parent = sysUserService.selectUserWithDept(parentUserId);
            if (parent == null || !parent.isBuyerMainIdentity()) {
                throw new ServiceException("关联的父用户不是有效的商家总账号");
            }
            return new ChargeAccount(parent.getUserId(), user.getUserId(), false);
        }
        throw new ServiceException("仅支持商家总账号或员工子账号进行扣费");
    }

    private static final class CachedChargeAccount {
        private final ChargeAccount account;
        private final long expiresAt;

        private CachedChargeAccount(ChargeAccount account) {
            this.account = account;
            this.expiresAt = System.currentTimeMillis() + CACHE_TTL_MILLIS;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    @Getter
    public static class ChargeAccount {
        private final Long chargeUserId;
        private final Long operatorUserId;
        private final boolean mainAccount;

        public ChargeAccount(Long chargeUserId, Long operatorUserId, boolean mainAccount) {
            this.chargeUserId = chargeUserId;
            this.operatorUserId = operatorUserId;
            this.mainAccount = mainAccount;
        }
    }
}
