package com.deepreach.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionSettlementApproveRequest {

    @NotNull(message = "审批金额不能为空")
    @DecimalMin(value = "0.0001", message = "审批金额必须大于0")
    private BigDecimal approvedAmount;

    private String remark;
}
