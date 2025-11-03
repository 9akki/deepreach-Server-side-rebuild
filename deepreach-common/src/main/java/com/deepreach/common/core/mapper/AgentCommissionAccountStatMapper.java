package com.deepreach.common.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface AgentCommissionAccountStatMapper {

    List<Map<String, Object>> selectCommissionByUserIds(@Param("userIds") Set<Long> userIds);
}
