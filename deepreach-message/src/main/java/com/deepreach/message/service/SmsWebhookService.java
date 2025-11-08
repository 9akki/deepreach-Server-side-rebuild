package com.deepreach.message.service;

import com.deepreach.message.dto.SmsWebhookPayload;

public interface SmsWebhookService {

    SmsWebhookPayload normalizePayload(SmsWebhookPayload payload);

    SmsWebhookPayload handleInbound(SmsWebhookPayload payload);
}
