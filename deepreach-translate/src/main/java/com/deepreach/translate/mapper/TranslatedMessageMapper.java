package com.deepreach.translate.mapper;

import com.deepreach.translate.entity.TranslatedMessage;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TranslatedMessageMapper {

    int insert(TranslatedMessage entity);

    Optional<TranslatedMessage> selectLatestBySentText(@Param("userId") Long userId,
                                                       @Param("selfLanguageCode") String selfLanguageCode,
                                                       @Param("sentText") String sentText);
}
