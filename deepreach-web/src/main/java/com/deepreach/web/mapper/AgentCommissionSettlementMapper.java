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
}
