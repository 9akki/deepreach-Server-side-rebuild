package com.deepreach.message.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

public class SmsWebhookPayload {

    @JsonAlias({"messagesid"})
    private String messageSid;

    @JsonAlias({"from"})
    private String messageFrom;

    @JsonAlias({"to"})
    private String messageTo;

    @JsonAlias({"source"})
    private String source;

    @JsonAlias({"accountid"})
    private String accountId;

    private String targetNumber;

    @JsonAlias({"message", "body"})
    private String messageContent;

    private List<String> mediaUrls = new ArrayList<>();

    @JsonAlias({"receiveddatetime"})
    private String receivedDatetime;

    public String getMessageSid() {
        return messageSid;
    }

    public void setMessageSid(String messageSid) {
        this.messageSid = messageSid;
    }

    public String getMessageFrom() {
        return messageFrom;
    }

    public void setMessageFrom(String messageFrom) {
        this.messageFrom = messageFrom;
    }

    public String getMessageTo() {
        return messageTo;
    }

    public void setMessageTo(String messageTo) {
        this.messageTo = messageTo;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getTargetNumber() {
        return targetNumber;
    }

    public void setTargetNumber(String targetNumber) {
        this.targetNumber = targetNumber;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
    }

    @JsonAlias({"mediaurls"})
    public void setMediaUrlsRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.mediaUrls = new ArrayList<>();
            return;
        }
        String[] parts = raw.split(";");
        List<String> parsed = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        this.mediaUrls = parsed;
    }

    public String getReceivedDatetime() {
        return receivedDatetime;
    }

    public void setReceivedDatetime(String receivedDatetime) {
        this.receivedDatetime = receivedDatetime;
    }
}
