package com.deepreach.message.controller;

import com.deepreach.common.web.LegacyResponse;
import com.deepreach.message.dto.SmsContactListRequest;
import com.deepreach.message.dto.SmsContactSummary;
import com.deepreach.message.dto.SmsLegacyMessageSendRequest;
import com.deepreach.message.dto.SmsMessageQueryRequest;
import com.deepreach.message.dto.SmsMessageReadRequest;
import com.deepreach.message.dto.SmsMessageRecord;
import com.deepreach.message.dto.SmsMessageSendRequest;
import com.deepreach.message.dto.SmsSendLogResponse;
import com.deepreach.message.dto.SmsSendResult;
import com.deepreach.message.service.SmsConversationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsMessageController {

    private final SmsConversationService smsConversationService;

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@Valid @RequestBody SmsMessageSendRequest request) {
        try {
            SmsSendLogResponse response = smsConversationService.sendMessage(request);
            return LegacyResponse.success(response);
        } catch (Exception ex) {
            return LegacyResponse.error("发送短信失败: " + ex.getMessage());
        }
    }

    @PostMapping("/messages/send")
    public Map<String, Object> sendLegacyMessage(@Valid @RequestBody SmsLegacyMessageSendRequest request) {
        try {
            SmsSendResult response = smsConversationService.sendMessageLegacy(request);
            return LegacyResponse.success(response);
        } catch (Exception ex) {
            return LegacyResponse.error("发送短信失败: " + ex.getMessage());
        }
    }

    @PostMapping("/messages/read")
    public Map<String, Object> setRead(@Valid @RequestBody SmsMessageReadRequest request) {
        smsConversationService.setMessageRead(request.getTaskId(), request.getTargetNumber());
        return LegacyResponse.success(Boolean.TRUE);
    }

    @PostMapping("/tasks/contacts")
    public Map<String, Object> listContacts(@Valid @RequestBody SmsContactListRequest request) {
        List<SmsContactSummary> contacts = smsConversationService.listContactsByTask(request);
        return LegacyResponse.success(contacts);
    }

    @PostMapping("/tasks/contacts/messages")
    public Map<String, Object> listMessages(@Valid @RequestBody SmsMessageQueryRequest request) {
        List<SmsMessageRecord> records = smsConversationService.listMessagesByContact(request);
        return LegacyResponse.success(records);
    }

    @GetMapping("/messages/search")
    public Map<String, Object> searchMessages(@RequestParam("taskId") Long taskId,
                                              @RequestParam(value = "targetNumber", required = false) String targetNumber,
                                              @RequestParam(value = "messageContent", required = false) String messageContent) {
        try {
            List<SmsContactSummary> results = smsConversationService.searchMessages(taskId, targetNumber, messageContent);
            return LegacyResponse.success(results);
        } catch (Exception ex) {
            return LegacyResponse.error("搜索短信消息失败: " + ex.getMessage());
        }
    }
}
