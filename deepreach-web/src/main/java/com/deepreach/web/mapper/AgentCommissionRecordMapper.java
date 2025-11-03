package com.deepreach.web.mapper;

import com.deepreach.web.entity.AgentCommissionRecord;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AgentCommissionRecordMapper {

    int insert(AgentCommissionRecord record);

    List<Map<String, Object>> selectRecordsByAgent(@Param("agentUserId") Long agentUserId,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime,
                                                   @Param("minAmount") BigDecimal minAmount,
                                                   @Param("maxAmount") BigDecimal maxAmount);

    List<Map<String, Object>> sumCommissionByAgents(@Param("agentUserIds") List<Long> agentUserIds,
                                                    @Param("startTime") String startTime,
                                                    @Param("endTime") String endTime,
                                                    @Param("minAmount") BigDecimal minAmount,
                                                    @Param("maxAmount") BigDecimal maxAmount);
}
