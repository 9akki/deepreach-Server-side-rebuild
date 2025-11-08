package com.deepreach.message.mapper;

import com.deepreach.message.entity.SmsTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsTaskMapper {

    int insertTask(SmsTask task);

    SmsTask selectById(@Param("id") Long id);

    List<SmsTask> listByUserId(@Param("userId") Long userId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int incrementSentCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementReplyCount(@Param("id") Long id, @Param("delta") int delta);

    int incrementTotalCount(@Param("id") Long id, @Param("delta") int delta);
}
