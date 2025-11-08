package com.deepreach.message.service;

import com.deepreach.message.dto.SmsContactListRequest;
import com.deepreach.message.dto.SmsContactSummary;
import com.deepreach.message.dto.SmsLegacyMessageSendRequest;
import com.deepreach.message.dto.SmsMessageQueryRequest;
import com.deepreach.message.dto.SmsMessageRecord;
import com.deepreach.message.dto.SmsMessageSendRequest;
import com.deepreach.message.dto.SmsSendLogResponse;
import com.deepreach.message.dto.SmsSendResult;
import java.util.List;

public interface SmsConversationService {

    SmsSendLogResponse sendMessage(SmsMessageSendRequest request);

    SmsSendResult sendMessageLegacy(SmsLegacyMessageSendRequest request);

    List<SmsContactSummary> listContactsByTask(SmsContactListRequest request);

    List<SmsMessageRecord> listMessagesByContact(SmsMessageQueryRequest request);

    void setMessageRead(Long taskId, String targetNumber);
}
