package com.deepreach.web.mapper;

import com.deepreach.web.entity.CommunicateHistory;
import java.time.LocalDateTime;
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

    int insertPortrait(@Param("userId") Long userId,
                       @Param("contactUsername") String contactUsername,
                       @Param("platformId") Integer platformId,
                       @Param("chatPortrait") String chatPortrait,
                       @Param("portraitUpdateTime") LocalDateTime portraitUpdateTime);

    int updatePortrait(@Param("userId") Long userId,
                       @Param("contactUsername") String contactUsername,
                       @Param("platformId") Integer platformId,
                       @Param("chatPortrait") String chatPortrait,
                       @Param("portraitUpdateTime") LocalDateTime portraitUpdateTime);
}
