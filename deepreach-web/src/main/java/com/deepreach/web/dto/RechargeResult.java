package com.deepreach.web.dto;

import com.deepreach.web.entity.DrBillingRecord;
import com.deepreach.web.entity.UserDrBalance;
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
