package com.deepreach.common.core.support;

import com.deepreach.common.core.domain.entity.UserDrBalance;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.exception.ServiceException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 在提供服务前校验余额是否足够。
 */
@Component
@RequiredArgsConstructor
public class ConsumptionBalanceGuard {

    private final ChargeAccountResolver chargeAccountResolver;
    private final UserDrBalanceService userDrBalanceService;

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
        UserDrBalance balance = userDrBalanceService.getByUserId(account.getChargeUserId());
        if (balance == null) {
            throw new ServiceException("用户余额账户不存在");
        }
        if (!balance.isNormal()) {
            throw new ServiceException("用户余额账户状态异常，无法使用服务");
        }
        BigDecimal available = balance.getDrBalance() != null ? balance.getDrBalance() : BigDecimal.ZERO;
        if (available.compareTo(minimumAmount) < 0) {
            throw new ServiceException("账户余额不足，请先充值");
        }
        return account;
    }
}
