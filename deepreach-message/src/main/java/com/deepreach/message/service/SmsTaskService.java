package com.deepreach.message.service;

import com.deepreach.message.dto.SmsTaskCreateRequest;
import com.deepreach.message.dto.SmsTaskDetailResponse;
import com.deepreach.message.dto.SmsTaskDetailsResponse;
import com.deepreach.message.dto.SmsTaskSummaryResponse;
import java.util.List;

public interface SmsTaskService {

    Long createTask(SmsTaskCreateRequest request);

    List<SmsTaskSummaryResponse> listTaskSummaries(Long userId);

    List<SmsTaskDetailsResponse> listTaskDetails(Long userId);

    SmsTaskDetailResponse getTaskDetail(Long taskId);
}
