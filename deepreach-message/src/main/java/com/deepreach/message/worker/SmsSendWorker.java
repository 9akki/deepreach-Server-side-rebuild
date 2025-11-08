package com.deepreach.message.worker;

import com.alibaba.fastjson2.JSON;
import com.deepreach.common.core.domain.entity.DrBillingRecord;
import com.deepreach.common.core.domain.entity.DrPriceConfig;
import com.deepreach.common.core.dto.DeductResponse;
import com.deepreach.common.core.service.DrPriceConfigService;
import com.deepreach.common.core.service.UserDrBalanceService;
import com.deepreach.common.core.support.ChargeAccountResolver;
import com.deepreach.common.core.support.ConsumptionBalanceGuard;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.message.config.MessageSmsProperties;
import com.deepreach.message.entity.SmsHistory;
import com.deepreach.message.entity.SmsTask;
import com.deepreach.message.mapper.SmsHistoryMapper;
import com.deepreach.message.mapper.SmsTaskMapper;
import com.deepreach.message.service.SmsGatewayClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class SmsSendWorker {

    private static final BigDecimal DEFAULT_SMS_UNIT_PRICE = new BigDecimal("0.05");

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sms-send-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final SmsGatewayClient smsGatewayClient;
    private final SmsTaskMapper smsTaskMapper;
    private final SmsHistoryMapper smsHistoryMapper;
    private final UserDrBalanceService userDrBalanceService;
    private final DrPriceConfigService drPriceConfigService;
    private final MessageSmsProperties messageSmsProperties;
    private final ConsumptionBalanceGuard balanceGuard;

    public SmsSendWorker(SmsGatewayClient smsGatewayClient,
                         SmsTaskMapper smsTaskMapper,
                         SmsHistoryMapper smsHistoryMapper,
                         UserDrBalanceService userDrBalanceService,
                         DrPriceConfigService drPriceConfigService,
                         MessageSmsProperties messageSmsProperties,
                         ConsumptionBalanceGuard balanceGuard) {
        this.smsGatewayClient = smsGatewayClient;
        this.smsTaskMapper = smsTaskMapper;
        this.smsHistoryMapper = smsHistoryMapper;
        this.userDrBalanceService = userDrBalanceService;
        this.drPriceConfigService = drPriceConfigService;
        this.messageSmsProperties = messageSmsProperties;
        this.balanceGuard = balanceGuard;
    }

    @PostConstruct
    public void start() {
        executor.submit(this::runLoop);
        log.info("SmsSendWorker started");
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    public void enqueue(Long taskId) {
        if (taskId != null) {
            queue.offer(taskId);
        }
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Long taskId = queue.poll(1, TimeUnit.SECONDS);
                if (taskId == null) {
                    continue;
                }
                processTask(taskId);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("SmsSendWorker encountered error", ex);
            }
        }
    }

    private void processTask(Long taskId) {
        SmsTask task = smsTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        List<String> numbers = JSON.parseArray(task.getReceiverNumbers(), String.class);
        List<String> contents = JSON.parseArray(task.getMessageContents(), String.class);
        if (CollectionUtils.isEmpty(numbers) || CollectionUtils.isEmpty(contents)) {
            log.warn("Sms task {} has empty receivers or contents", taskId);
            return;
        }
        BigDecimal unitPrice = resolveSmsUnitPrice();
        ChargeAccountResolver.ChargeAccount account =
            balanceGuard.ensureSufficientBalance(task.getUserId(), unitPrice, "短信任务发送");
        int numberSize = numbers.size();
        int contentSize = contents.size();
        for (int i = 0; i < numberSize; i++) {
            String number = normalizeNumber(numbers.get(i));
            String content = contents.get(i % contentSize);
            SmsGatewayClient.SendCommand command = new SmsGatewayClient.SendCommand();
            command.setTo(number);
            command.setSource(StringUtils.hasText(messageSmsProperties.getSenderNumber())
                ? messageSmsProperties.getSenderNumber()
                : (task.getInstanceId() != null ? String.valueOf(task.getInstanceId()) : ""));
            command.setBody(content);
            SmsGatewayClient.SendResult result = smsGatewayClient.send(command);
            String recordedSource = StringUtils.hasText(result.getSourceId())
                ? normalizeNumber(result.getSourceId())
                : command.getSource();

            SmsHistory history = new SmsHistory();
            history.setTaskId(taskId);
            history.setTargetNumber(number);
            history.setMessageContent(content);
            history.setMessageTo(number);
            history.setMessageFrom(recordedSource);
            history.setSentAt(LocalDateTime.now());
            history.setStatus(result.isSuccess() ? 0 : 1);
            history.setRead(1);
            smsHistoryMapper.insertHistory(history);

            smsTaskMapper.incrementTotalCount(taskId, 1);
            if (result.isSuccess()) {
                smsTaskMapper.incrementSentCount(taskId, 1);
                deductBalance(account, unitPrice, "短信任务扣费");
            }
            sleepSafely(1000);
        }
        smsTaskMapper.updateStatus(taskId, 1);
    }

    private String normalizeNumber(String number) {
        if (number == null) {
            return null;
        }
        String normalized = number.trim();
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("\\s+", "");
    }

    private void deductBalance(ChargeAccountResolver.ChargeAccount account, BigDecimal unitPrice, String description) {
        if (account == null || unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        DrBillingRecord record = buildSmsBillingRecord(unitPrice, account, description);
        DeductResponse response = userDrBalanceService.deductWithDetails(record, account.getOperatorUserId());
        if (response == null || !response.isSuccess()) {
            String message = response != null ? response.getMessage() : "扣费响应为空";
            throw new ServiceException("短信扣费失败: " + message);
        }
    }

    private BigDecimal resolveSmsUnitPrice() {
        DrPriceConfig config = drPriceConfigService.selectDrPriceConfigByBusinessType(DrPriceConfig.BUSINESS_TYPE_SMS);
        if (config == null || config.getDrPrice() == null) {
            log.warn("短信计价配置缺失，使用默认单价 {}", DEFAULT_SMS_UNIT_PRICE);
            return DEFAULT_SMS_UNIT_PRICE;
        }
        return config.getDrPrice();
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private DrBillingRecord buildSmsBillingRecord(BigDecimal amount,
                                                  ChargeAccountResolver.ChargeAccount account,
                                                  String description) {
        DrBillingRecord record = new DrBillingRecord();
        record.setUserId(account.getChargeUserId());
        record.setOperatorId(account.getOperatorUserId());
        record.setBillType(2);
        record.setBillingType(1);
        record.setBusinessType(DrBillingRecord.BUSINESS_TYPE_SMS);
        record.setDrAmount(amount);
        record.setDescription(description);
        record.setRemark("SmsTaskWorker");
        return record;
    }
}
