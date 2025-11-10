package com.deepreach.web.controller;

import com.deepreach.common.web.BaseController;
import com.deepreach.common.utils.PageUtils;
import com.deepreach.common.web.Result;
import com.deepreach.common.web.page.TableDataInfo;
import com.deepreach.web.dto.AgentCommissionAccountDTO;
import com.deepreach.web.dto.AgentCommissionAdjustRequest;
import com.deepreach.web.dto.AgentCommissionRecordDTO;
import com.deepreach.web.dto.AgentCommissionRecordQuery;
import com.deepreach.web.dto.AgentCommissionOverviewRequest;
import com.deepreach.web.dto.AgentCommissionOverviewResponse;
import com.deepreach.web.dto.CommissionSettlementApplyRequest;
import com.deepreach.web.dto.CommissionSettlementApproveRequest;
import com.deepreach.web.dto.CommissionSettlementRejectRequest;
import com.deepreach.web.dto.AdminSettlementQueryRequest;
import com.deepreach.web.dto.AgentCommissionSummaryResponse;
import com.deepreach.web.entity.AgentCommissionSettlement;
import com.deepreach.web.service.AgentCommissionService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Validated
@RestController
@RequestMapping("/agent/commission")
@RequiredArgsConstructor
public class AgentCommissionController extends BaseController {

    private final AgentCommissionService agentCommissionService;

    /**
     * 代理申请佣金结算
     */
    @PostMapping("/settlement/apply")
    public Result<AgentCommissionSettlement> apply(@Validated @RequestBody CommissionSettlementApplyRequest request) {
        Long currentUserId = getCurrentUserId();
        try {
            AgentCommissionSettlement settlement = agentCommissionService.applySettlement(
                currentUserId,
                request.getAmount(),
                currentUserId,
                request.getRemark(),
                request.getNetwork(),
                request.getAddress()
            );
            return success(settlement);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error(400, ex.getMessage());
        }
    }

    /**
     * 审批通过
     */
    @PostMapping("/settlement/{id}/approve")
    public Result<AgentCommissionSettlement> approve(@PathVariable("id") @NotNull Long settlementId,
                                                     @Validated @RequestBody CommissionSettlementApproveRequest request) {
        Long operatorId = getCurrentUserId();
        AgentCommissionSettlement settlement = agentCommissionService.approveSettlement(
            settlementId,
            request.getApprovedAmount(),
            operatorId,
            request.getRemark()
        );
        return success(settlement);
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/settlement/{id}/reject")
    public Result<AgentCommissionSettlement> reject(@PathVariable("id") @NotNull Long settlementId,
                                                    @Validated @RequestBody CommissionSettlementRejectRequest request) {
        Long operatorId = getCurrentUserId();
        AgentCommissionSettlement settlement = agentCommissionService.rejectSettlement(
            settlementId,
            operatorId,
            request.getRemark()
        );
        return success(settlement);
    }

    /**
     * 管理员获取进行中的结算请求
     */
    @GetMapping("/settlement/admin/in-progress")
    public Result<List<AgentCommissionSettlement>> listInProgressSettlements() {
        List<AgentCommissionSettlement> settlements = agentCommissionService.listSettlementsByStatuses(
            Collections.singletonList(AgentCommissionSettlement.STATUS_PENDING)
        );
        return success(settlements);
    }

    /**
     * 管理员获取已处理完成的结算记录
     */
    @GetMapping("/settlement/admin/completed")
    public Result<List<AgentCommissionSettlement>> listCompletedSettlements() {
        List<AgentCommissionSettlement> settlements = agentCommissionService.listSettlementsByStatuses(
            Arrays.asList(
                AgentCommissionSettlement.STATUS_APPROVED,
                AgentCommissionSettlement.STATUS_REJECTED,
                AgentCommissionSettlement.STATUS_CANCELLED
            )
        );
        return success(settlements);
    }

    /**
     * 管理员分页查询结算申请
     */
    @PostMapping("/settlement/admin/list")
    public TableDataInfo<AgentCommissionSettlement> listSettlementsForAdmin(
        @RequestBody(required = false) AdminSettlementQueryRequest request) {
        return buildAdminSettlementPage(request, null);
    }

    @GetMapping("/settlement/admin/list")
    public TableDataInfo<AgentCommissionSettlement> listSettlementsForAdminGet(
        AdminSettlementQueryRequest request,
        @RequestParam(value = "userId", required = false) Long queryUserId,
        @RequestParam(value = "pageNum", required = false) Integer pageNum,
        @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (request == null) {
            request = new AdminSettlementQueryRequest();
        }
        if (pageNum != null) {
            request.setPageNum(pageNum);
        }
        if (pageSize != null) {
            request.setPageSize(pageSize);
        }
        return buildAdminSettlementPage(request, queryUserId);
    }

    private TableDataInfo<AgentCommissionSettlement> buildAdminSettlementPage(
        AdminSettlementQueryRequest request,
        Long queryUserId) {
        AdminSettlementQueryRequest effective = request != null ? request : new AdminSettlementQueryRequest();
        if (queryUserId != null && effective.getUserId() == null) {
            effective.setUserId(queryUserId);
        }
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return TableDataInfo.error("用户未登录");
        }
        com.deepreach.common.core.domain.model.LoginUser loginUser = com.deepreach.common.security.SecurityUtils.getCurrentLoginUser();
        boolean isAdmin = loginUser != null && loginUser.isAdminIdentity();
        if (!isAdmin) {
            effective.setUserId(currentUserId);
        }
        int pageNum = effective.getPageNum() != null && effective.getPageNum() > 0 ? effective.getPageNum() : 1;
        int pageSize = effective.getPageSize() != null && effective.getPageSize() > 0 ? effective.getPageSize() : 10;

        List<AgentCommissionSettlement> all = agentCommissionService.searchAdminSettlements(effective);
        List<AgentCommissionSettlement> rows = PageUtils.manualPage(all, pageNum, pageSize);
        PageUtils.PageState state = PageUtils.getCurrentPageState();
        long total = state != null ? state.getTotal() : all.size();
        int respPageNum = state != null ? state.getPageNum() : pageNum;
        int respPageSize = state != null ? state.getPageSize() : pageSize;
        PageUtils.clearManualPage();
        return TableDataInfo.success(rows, total, respPageNum, respPageSize);
    }

    /**
     * 获取代理佣金账户
     */
    @GetMapping("/account/{agentUserId}")
    public Result<AgentCommissionAccountDTO> getAccount(@PathVariable Long agentUserId) {
        AgentCommissionAccountDTO account = agentCommissionService.getCommissionAccount(agentUserId);
        return success(account);
    }

    /**
     * 获取代理佣金汇总（总佣金/已结算/可用）
     */
    @GetMapping("/account/{agentUserId}/summary")
    public Result<AgentCommissionSummaryResponse> getAccountSummary(@PathVariable Long agentUserId) {
        AgentCommissionSummaryResponse summary = agentCommissionService.getAgentCommissionSummary(agentUserId);
        return success(summary);
    }

    /**
     * 获取代理佣金明细
     */
    @PostMapping("/{agentUserId}/records")
    public Result<?> getRecords(@PathVariable Long agentUserId,
                                @RequestBody(required = false) AgentCommissionRecordQuery query) {
        java.util.List<AgentCommissionRecordDTO> records = agentCommissionService.getCommissionRecords(agentUserId, query);
        return success(records);
    }

    /**
     * 获取伞下代理佣金概览
     */
    @PostMapping("/overview")
    public Result<AgentCommissionOverviewResponse> getOverview(@RequestBody(required = false) AgentCommissionOverviewRequest request) {
        Long currentUserId = getCurrentUserId();
        AgentCommissionOverviewResponse response = agentCommissionService.getAgentCommissionOverview(currentUserId, request);
        return success(response);
    }

    /**
     * 手动调整代理佣金
     */
    @PostMapping("/manual-adjust")
    public Result<AgentCommissionAccountDTO> manualAdjust(@Validated @RequestBody AgentCommissionAdjustRequest request) {
        Long operatorId = getCurrentUserId();
        AgentCommissionAccountDTO dto = agentCommissionService.manualAdjustCommission(
            request.getAgentUserId(),
            request.getAmount(),
            operatorId,
            request.getRemark()
        );
        return success("调整成功", dto);
    }

    /**
     * 获取所有代理已结算佣金总额
     */
    @GetMapping("/settlement/total")
    public Result<BigDecimal> getTotalSettledCommission() {
        BigDecimal total = agentCommissionService.sumSettledCommission();
        return success(total);
    }
}
