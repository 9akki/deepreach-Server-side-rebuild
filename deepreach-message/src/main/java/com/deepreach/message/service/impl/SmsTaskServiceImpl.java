package com.deepreach.message.service.impl;

import com.alibaba.fastjson2.JSON;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.message.dto.SmsTaskCreateRequest;
import com.deepreach.message.dto.SmsTaskDetailResponse;
import com.deepreach.message.dto.SmsTaskDetailsResponse;
import com.deepreach.message.dto.SmsTaskSummaryResponse;
import com.deepreach.message.entity.SmsTask;
import com.deepreach.message.mapper.SmsHistoryMapper;
import com.deepreach.message.mapper.SmsTaskMapper;
import com.deepreach.message.service.SmsTaskService;
import com.deepreach.message.worker.SmsSendWorker;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsTaskServiceImpl implements SmsTaskService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SmsTaskMapper smsTaskMapper;
    private final SmsHistoryMapper smsHistoryMapper;
    private final SmsSendWorker smsSendWorker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(SmsTaskCreateRequest request) {
        validateCreateRequest(request);
        SmsTask task = new SmsTask();
        task.setUserId(request.getUserId());
        task.setInstanceId(request.getInstanceId());
        task.setReceiverNumbers(JSON.toJSONString(request.getReceiverNumbers()));
        task.setMessageContents(JSON.toJSONString(request.getMessageContents()));
        task.setSentCount(0);
        task.setReplyCount(0);
        task.setTotalCount(request.getTotalCount() != null ? request.getTotalCount() : request.getReceiverNumbers().size());
        task.setStatus(0);
        task.setCreatedAt(LocalDateTime.now());
        smsTaskMapper.insertTask(task);
        log.info("Created sms task id={} userId={}", task.getId(), task.getUserId());
        smsSendWorker.enqueue(task.getId());
        return task.getId();
    }

    @Override
    public List<SmsTaskSummaryResponse> listTaskSummaries(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<SmsTask> tasks = smsTaskMapper.listByUserId(userId);
        return tasks.stream()
            .map(task -> {
                SmsTaskSummaryResponse resp = new SmsTaskSummaryResponse();
                resp.setTaskId(task.getId());
                resp.setCreatedAt(formatDate(task.getCreatedAt()));
                resp.setUnreadCount(safeCountUnread(task.getId()));
                return resp;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<SmsTaskDetailsResponse> listTaskDetails(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<SmsTask> tasks = smsTaskMapper.listByUserId(userId);
        return tasks.stream()
            .map(task -> {
                SmsTaskDetailsResponse resp = new SmsTaskDetailsResponse();
                resp.setTaskId(task.getId());
                resp.setTotalCount(task.getTotalCount());
                resp.setSentCount(task.getSentCount());
                resp.setReplyCount(task.getReplyCount());
                resp.setDeliveryRate(calculateDeliveryRate(task.getSentCount(), task.getTotalCount()));
                resp.setStatus(task.getStatus());
                resp.setCreatedAt(formatDate(task.getCreatedAt()));
                return resp;
            })
            .collect(Collectors.toList());
    }

    @Override
    public SmsTaskDetailResponse getTaskDetail(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("任务ID不能为空");
        }
        SmsTask task = smsTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        SmsTaskDetailResponse response = new SmsTaskDetailResponse();
        response.setTaskId(task.getId());
        response.setTotalCount(task.getTotalCount());
        response.setSentCount(task.getSentCount());
        response.setReplyCount(task.getReplyCount());
        response.setDeliveryRate(calculateDeliveryRate(task.getSentCount(), task.getTotalCount()));
        response.setUnreadCount(safeCountUnread(task.getId()));
        response.setStatus(task.getStatus());
        return response;
    }

    private void validateCreateRequest(SmsTaskCreateRequest request) {
        if (request.getUserId() == null || request.getInstanceId() == null) {
            throw new ServiceException("用户或实例信息缺失");
        }
        if (CollectionUtils.isEmpty(request.getReceiverNumbers())) {
            throw new ServiceException("接收号码不能为空");
        }
        if (CollectionUtils.isEmpty(request.getMessageContents())) {
            throw new ServiceException("短信内容不能为空");
        }
    }

    private double calculateDeliveryRate(Integer sentCount, Integer totalCount) {
        if (sentCount == null || totalCount == null || totalCount == 0) {
            return 0D;
        }
        BigDecimal sent = new BigDecimal(sentCount);
        BigDecimal total = new BigDecimal(totalCount);
        return sent.divide(total, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private int safeCountUnread(Long taskId) {
        Integer count = smsHistoryMapper.countUnreadByTask(taskId);
        return count != null ? count : 0;
    }

    private String formatDate(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.toLocalDate().format(DATE_FORMAT);
    }
}
