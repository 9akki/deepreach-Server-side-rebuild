package com.deepreach.common.core.support;

import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.exception.ServiceException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 在提供服务前校验余额是否足够。
 */
@Component
@RequiredArgsConstructor
public class ConsumptionBalanceGuard {

    private static final long BALANCE_CACHE_TTL_MILLIS = 5_000;

    private final ChargeAccountResolver chargeAccountResolver;
    private final UserDrBalanceService userDrBalanceService;
    private final Map<Long, CachedBalanceEntry> balanceCache = new ConcurrentHashMap<>();

    public ChargeAccountResolver.ChargeAccount ensureSufficientBalance(Long requestUserId,
                                                                       BigDecimal minimumAmount,
                                                                       String scene) {
        if (requestUserId == null || requestUserId <= 0) {
            throw new ServiceException("用户ID不能为空");
        }
        if (minimumAmount == null || minimumAmount.compareTo(BigDecimal.ZERO) <= 0) {
            minimumAmount = BigDecimal.ZERO;
        }
        ChargeAccountResolver.ChargeAccount account = chargeAccountResolver.resolve(requestUserId);
        UserBalanceSnapshot snapshot = getBalanceSnapshot(account.getChargeUserId(), false);
        if (!snapshot.normal) {
            throw new ServiceException("用户余额账户状态异常，无法使用服务");
        }
        if (snapshot.available.compareTo(minimumAmount) < 0) {
            snapshot = getBalanceSnapshot(account.getChargeUserId(), true);
            if (snapshot.available.compareTo(minimumAmount) < 0) {
                throw new ServiceException("账户余额不足，请先充值");
            }
        }
        return account;
    }

    public void evictBalance(Long userId) {
        if (userId != null) {
            balanceCache.remove(userId);
        }
    }

    private UserBalanceSnapshot getBalanceSnapshot(Long chargeUserId, boolean forceReload) {
        if (!forceReload) {
            CachedBalanceEntry cached = balanceCache.get(chargeUserId);
            if (cached != null && !cached.isExpired()) {
                return cached.snapshot;
            }
        }
        return loadSnapshot(chargeUserId);
    }

    private UserBalanceSnapshot loadSnapshot(Long chargeUserId) {
        UserDrBalance balance = userDrBalanceService.getByUserId(chargeUserId);
        if (balance == null) {
            throw new ServiceException("用户余额账户不存在");
        }
        UserBalanceSnapshot snapshot = new UserBalanceSnapshot(
            balance.getAvailableBalance(),
            balance.isNormal(),
            balance.getVersion() != null ? balance.getVersion() : 0
        );
        balanceCache.put(chargeUserId, new CachedBalanceEntry(snapshot));
        return snapshot;
    }

    private static final class CachedBalanceEntry {
        private final UserBalanceSnapshot snapshot;
        private final long expiresAt;

        private CachedBalanceEntry(UserBalanceSnapshot snapshot) {
            this.snapshot = snapshot;
            this.expiresAt = System.currentTimeMillis() + BALANCE_CACHE_TTL_MILLIS;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static final class UserBalanceSnapshot {
        private final BigDecimal available;
        private final boolean normal;
        private final int version;

        private UserBalanceSnapshot(BigDecimal available, boolean normal, int version) {
            this.available = available != null ? available : BigDecimal.ZERO;
            this.normal = normal;
            this.version = version;
        }
    }
}
