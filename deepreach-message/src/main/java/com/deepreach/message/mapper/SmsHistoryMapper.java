package com.deepreach.message.mapper;

import com.deepreach.message.entity.SmsHistory;
import com.deepreach.message.dto.SmsContactSummary;
import com.deepreach.message.dto.SmsMessageRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsHistoryMapper {

    int insertHistory(SmsHistory history);

    List<SmsHistory> listByTask(@Param("taskId") Long taskId);

    List<SmsHistory> listMessagesByContact(@Param("taskId") Long taskId, @Param("targetNumber") String targetNumber);

    int updateReadFlag(@Param("taskId") Long taskId, @Param("targetNumber") String targetNumber, @Param("read") Integer read);

    Long findTaskIdByFromAndTo(@Param("messageFrom") String messageFrom, @Param("messageTo") String messageTo);

    Integer countUnreadByTask(@Param("taskId") Long taskId);

    List<SmsContactSummary> listContactsWithLatestMessagePaged(@Param("taskId") Long taskId,
                                                               @Param("limit") int limit,
                                                               @Param("offset") int offset);

    List<SmsMessageRecord> listMessagesByContactPaged(@Param("taskId") Long taskId,
                                                      @Param("targetNumber") String targetNumber,
                                                      @Param("limit") int limit,
                                                      @Param("offset") int offset);

    List<SmsContactSummary> searchMessagesByTask(@Param("taskId") Long taskId,
                                                 @Param("targetNumber") String targetNumber,
                                                 @Param("messageContent") String messageContent);
}
