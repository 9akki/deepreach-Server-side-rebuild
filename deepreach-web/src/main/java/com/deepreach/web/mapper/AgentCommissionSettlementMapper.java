package com.deepreach.web.mapper;

import com.deepreach.web.entity.AgentCommissionSettlement;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AgentCommissionSettlementMapper {

    int insert(AgentCommissionSettlement settlement);

    int update(AgentCommissionSettlement settlement);

    AgentCommissionSettlement selectById(Long settlementId);

    List<AgentCommissionSettlement> selectByAgentUserId(Long agentUserId);

    List<AgentCommissionSettlement> selectByStatuses(@Param("statuses") List<String> statuses);

    List<AgentCommissionSettlement> searchAdminSettlements(@Param("settlementId") Long settlementId,
                                                           @Param("agentUserId") Long agentUserId,
                                                           @Param("username") String username,
                                                           @Param("beginTime") java.time.LocalDateTime beginTime,
                                                           @Param("endTime") java.time.LocalDateTime endTime);
}
