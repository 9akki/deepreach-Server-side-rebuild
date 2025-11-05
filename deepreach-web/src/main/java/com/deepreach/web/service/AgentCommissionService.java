package com.deepreach.web.service;

import com.deepreach.web.dto.AgentCommissionAccountDTO;
import com.deepreach.web.dto.AgentCommissionOverviewRequest;
import com.deepreach.web.dto.AgentCommissionOverviewResponse;
import com.deepreach.web.dto.AgentCommissionRecordDTO;
import com.deepreach.web.dto.AgentCommissionRecordQuery;
import com.deepreach.web.entity.AgentCommissionAccount;
import com.deepreach.web.entity.AgentCommissionSettlement;

import java.math.BigDecimal;
import java.util.List;

public interface AgentCommissionService {

    /**
     * 处理充值佣金分发
     *
     * @param buyerUserId 买家总账户用户ID
     * @param buyerDeptId 买家总账户部门ID
     * @param rechargeAmount 充值金额
     * @param operatorId 操作人
     * @param billingId 充值账单ID
     */
    void distributeRechargeCommission(Long buyerUserId,
                                      Long buyerDeptId,
                                      BigDecimal rechargeAmount,
                                      Long operatorId,
                                      Long billingId);

    /**
     * 获取或创建代理佣金账户
     *
     * @param agentUserId 代理用户ID
     * @return 佣金账户
     */
    AgentCommissionAccount getOrCreateAccount(Long agentUserId);

    /**
     * 创建结算申请
     *
     * @param settlement 申请信息
     * @return 结算申请
     */
    AgentCommissionSettlement createSettlement(AgentCommissionSettlement settlement);

    /**
     * 代理提交结算申请
     *
     * @param agentUserId 代理用户ID
     * @param amount 申请金额
     * @param operatorId 操作人
     * @param remark 备注
     * @return 结算申请
     */
    AgentCommissionSettlement applySettlement(Long agentUserId,
                                              BigDecimal amount,
                                              Long operatorId,
                                              String remark);

    /**
     * 审批结算申请 - 通过
     *
     * @param settlementId 申请ID
     * @param approvedAmount 审批金额
     * @param operatorId 审批人ID
     * @param remark 审批备注
     * @return 更新后的申请
     */
    AgentCommissionSettlement approveSettlement(Long settlementId,
                                                BigDecimal approvedAmount,
                                                Long operatorId,
                                                String remark);

    /**
     * 审批结算申请 - 拒绝
     *
     * @param settlementId 申请ID
     * @param operatorId 审批人ID
     * @param remark 拒绝原因
     * @return 更新后的申请
     */
    AgentCommissionSettlement rejectSettlement(Long settlementId,
                                               Long operatorId,
                                               String remark);

    AgentCommissionAccountDTO getCommissionAccount(Long agentUserId);
    com.deepreach.web.dto.AgentCommissionSummaryResponse getAgentCommissionSummary(Long agentUserId);

    List<AgentCommissionRecordDTO> getCommissionRecords(Long agentUserId, AgentCommissionRecordQuery query);

    AgentCommissionOverviewResponse getAgentCommissionOverview(Long currentUserId, AgentCommissionOverviewRequest request);

    /**
     * 根据状态集合查询结算申请
     *
     * @param statuses 状态列表
     * @return 符合条件的结算申请
     */
    List<AgentCommissionSettlement> listSettlementsByStatuses(List<String> statuses);

    List<AgentCommissionSettlement> searchAdminSettlements(com.deepreach.web.dto.AdminSettlementQueryRequest query);

    /**
     * 汇总所有代理已结算佣金总额
     *
     * @return 已结算佣金总额
     */
    BigDecimal sumSettledCommission();

    /**
     * 手动调整代理佣金
     *
     * @param agentUserId 代理用户ID
     * @param amount 调整金额（正数扣减，负数增加）
     * @param operatorId 操作人
     * @param remark 备注
     * @return 调整后的佣金账户信息
     */
    AgentCommissionAccountDTO manualAdjustCommission(Long agentUserId,
                                                     BigDecimal amount,
                                                     Long operatorId,
                                                     String remark);
}
