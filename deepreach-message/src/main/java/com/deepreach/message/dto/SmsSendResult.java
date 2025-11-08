package com.deepreach.message.dto;

public class SmsSendResult {

    private Long messageId;
    private Integer status;
    private Integer read;

    public SmsSendResult() {
    }

    public SmsSendResult(Long messageId, Integer status, Integer read) {
        this.messageId = messageId;
        this.status = status;
        this.read = read;
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
}
