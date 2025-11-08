package com.deepreach.message.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SmsTaskCreateRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long instanceId;

    @NotEmpty
    private List<String> receiverNumbers;

    @NotEmpty
    private List<String> messageContents;

    private Integer totalCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public List<String> getReceiverNumbers() {
        return receiverNumbers;
    }

    public void setReceiverNumbers(List<String> receiverNumbers) {
        this.receiverNumbers = receiverNumbers;
    }

    public List<String> getMessageContents() {
        return messageContents;
    }

    public void setMessageContents(List<String> messageContents) {
        this.messageContents = messageContents;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
