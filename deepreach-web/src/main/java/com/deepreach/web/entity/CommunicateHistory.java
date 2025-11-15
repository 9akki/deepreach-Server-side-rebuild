package com.deepreach.web.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * communicate_history 实体.
 */
@Data
public class CommunicateHistory {

    private Long userId;
    private String contactUsername;
    private Integer platformId;
    private String historySplice;
    private String chatPortrait;
    private LocalDateTime spliceUpdateTime;
    private LocalDateTime portraitUpdateTime;
    private Long version;
}
