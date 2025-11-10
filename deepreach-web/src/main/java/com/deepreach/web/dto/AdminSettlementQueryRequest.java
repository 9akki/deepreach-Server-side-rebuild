package com.deepreach.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 管理员结算申请查询请求
 */
@Data
public class AdminSettlementQueryRequest {
    @JsonAlias("agentUserId")
    private Long userId;
    private String username;
    private Long settlementId;
    private String beginTime;
    private String endTime;
    private Integer pageNum;
    private Integer pageSize;
}
