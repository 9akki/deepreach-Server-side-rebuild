package com.deepreach.common.core.dto;

import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.UserDrBalance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargeResult {
    private UserDrBalance balance;
    private DrBillingRecord billingRecord;
}
