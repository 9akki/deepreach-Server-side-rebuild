package com.deepreach.message.controller;

import com.deepreach.common.exception.ServiceException;
import com.deepreach.common.web.LegacyResponse;
import com.deepreach.message.dto.SmsWebhookPayload;
import com.deepreach.message.service.SmsWebhookService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsWebhookController {

    private final SmsWebhookService smsWebhookService;

    @PostMapping({"", "/webhook"})
    public Map<String, Object> receive(@Valid @RequestBody SmsWebhookPayload payload) {
        try {
            SmsWebhookPayload normalized = smsWebhookService.handleInbound(payload);
            Map<String, Object> body = LegacyResponse.success(normalized);
            body.put("message", "Webhook已写入历史");
            return body;
        } catch (ServiceException ex) {
            return LegacyResponse.error(ex.getCode() != null ? ex.getCode() : 500, ex.getMessage());
        } catch (Exception ex) {
            log.error("Webhook处理失败", ex);
            return LegacyResponse.error("Webhook处理失败: " + ex.getMessage());
        }
    }
}
