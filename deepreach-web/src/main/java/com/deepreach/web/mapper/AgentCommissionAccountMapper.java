package com.deepreach.web.mapper;

import com.deepreach.web.entity.AgentCommissionAccount;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AgentCommissionAccountMapper {

    AgentCommissionAccount selectByAgentUserId(@Param("agentUserId") Long agentUserId);

    int insert(AgentCommissionAccount account);

    int incrementCommission(@Param("agentUserId") Long agentUserId,
                            @Param("amount") BigDecimal amount);

    int adjustAvailableCommission(@Param("agentUserId") Long agentUserId,
                                  @Param("availableDelta") BigDecimal availableDelta,
                                  @Param("frozenDelta") BigDecimal frozenDelta,
                                  @Param("pendingDelta") BigDecimal pendingDelta,
                                  @Param("settledDelta") BigDecimal settledDelta);

    List<Map<String, Object>> selectAccountsByAgentUserIds(@Param("agentUserIds") Set<Long> agentUserIds);

    BigDecimal sumSettledCommission();
}
