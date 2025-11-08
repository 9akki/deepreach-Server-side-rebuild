package com.deepreach.message.dto;

import com.deepreach.message.service.SmsGatewayClient;

public class SmsSendLogResponse {

    private Long messageId;
    private Integer status;
    private Integer read;
    private Object smsResult;

    public SmsSendLogResponse() {
    }

    public SmsSendLogResponse(Long messageId, Integer status, Integer read,
                              Object smsResult) {
        this.messageId = messageId;
        this.status = status;
        this.read = read;
        this.smsResult = smsResult;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRead() {
        return read;
    }

    public void setRead(Integer read) {
        this.read = read;
    }

    public Object getSmsResult() {
        return smsResult;
    }

    public void setSmsResult(Object smsResult) {
        this.smsResult = smsResult;
    }
}
