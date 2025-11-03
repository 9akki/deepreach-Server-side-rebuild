package com.deepreach.web.mapper;

import com.deepreach.web.entity.AgentCommissionSettlementRecord;

import java.util.List;

public interface AgentCommissionSettlementRecordMapper {

    int insert(AgentCommissionSettlementRecord record);

    List<AgentCommissionSettlementRecord> selectBySettlementId(Long settlementId);
}
