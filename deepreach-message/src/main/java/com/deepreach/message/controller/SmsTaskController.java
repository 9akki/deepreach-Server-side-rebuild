package com.deepreach.message.controller;

import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.web.LegacyResponse;
import com.deepreach.message.dto.SmsTaskCreateRequest;
import com.deepreach.message.dto.SmsTaskDetailResponse;
import com.deepreach.message.dto.SmsTaskDetailsResponse;
import com.deepreach.message.dto.SmsTaskSummaryResponse;
import com.deepreach.message.service.SmsTaskService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/sms/tasks")
@RequiredArgsConstructor
public class SmsTaskController {

    private final SmsTaskService smsTaskService;

    @PostMapping
    public Map<String, Object> createTask(@Valid @RequestBody SmsTaskCreateRequest request) {
        try {
            Long taskId = smsTaskService.createTask(request);
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            return LegacyResponse.success(data);
        } catch (ServiceException ex) {
            return LegacyResponse.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        } catch (Exception ex) {
            log.error("Create sms task failed", ex);
            return LegacyResponse.error("创建短信任务失败:" + ex.getMessage());
        }
    }

    @GetMapping
    public Map<String, Object> listTasks(@RequestParam("userId") Long userId,
                               @RequestParam(value = "summary", defaultValue = "false") boolean summary,
                               @RequestParam(value = "details", defaultValue = "false") boolean details) {
        if (summary) {
            List<SmsTaskSummaryResponse> summaries = smsTaskService.listTaskSummaries(userId);
            return LegacyResponse.success(summaries);
        }
        if (details) {
            List<SmsTaskDetailsResponse> detailList = smsTaskService.listTaskDetails(userId);
            return LegacyResponse.success(detailList);
        }
        return LegacyResponse.error(400, "请求参数无效，summary/details 至少选择其一");
    }

    @GetMapping("/{taskId}")
    public Map<String, Object> getTask(@PathVariable("taskId") Long taskId) {
        try {
            return LegacyResponse.success(smsTaskService.getTaskDetail(taskId));
        } catch (ServiceException ex) {
            return LegacyResponse.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        }
    }
}
