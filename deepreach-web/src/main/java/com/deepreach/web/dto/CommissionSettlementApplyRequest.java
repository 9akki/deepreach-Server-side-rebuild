package com.deepreach.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionSettlementApplyRequest {

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "0.0001", message = "申请金额必须大于0")
    private BigDecimal amount;

    private String remark;

    @NotBlank(message = "network不能为空")
    private String network;

    @NotBlank(message = "address不能为空")
    private String address;
}
