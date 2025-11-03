package com.deepreach.web.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 条件查询用户账单/交易记录的筛选参数
 */
@Data
public class DrTransactionQuery {
    private String startTime;
    private String endTime;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer billType;
    private String businessType;
}

