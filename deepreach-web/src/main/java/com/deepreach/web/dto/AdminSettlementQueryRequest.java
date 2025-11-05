package com.deepreach.web.dto;

import lombok.Data;

/**
 * 管理员结算申请查询请求
 */
@Data
public class AdminSettlementQueryRequest {
    private Long userId;
    private String username;
    private Long settlementId;
    private String beginTime;
    private String endTime;
    private Integer pageNum;
    private Integer pageSize;
}

