package com.deepreach.web.mapper;

import com.deepreach.web.entity.CommunicateHistory;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunicateHistoryMapper {

    CommunicateHistory selectByPk(@Param("userId") Long userId,
                                  @Param("contactUsername") String contactUsername,
                                  @Param("platformId") Integer platformId);

    int upsertHistory(@Param("userId") Long userId,
                      @Param("contactUsername") String contactUsername,
                      @Param("platformId") Integer platformId,
                      @Param("historySplice") String historySplice,
                      @Param("spliceUpdateTime") LocalDateTime spliceUpdateTime);

    List<CommunicateHistory> selectNeedPortraitUpdate(@Param("limit") int limit);

    int updatePortrait(@Param("userId") Long userId,
                       @Param("contactUsername") String contactUsername,
                       @Param("platformId") Integer platformId,
                       @Param("chatPortrait") String chatPortrait,
                       @Param("portraitUpdateTime") LocalDateTime portraitUpdateTime);
}
