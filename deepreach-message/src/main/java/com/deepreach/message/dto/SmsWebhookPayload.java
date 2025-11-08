package com.deepreach.message.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SmsWebhookPayload {

    @JsonAlias({"messagesid"})
    private String messageSid;

    @JsonAlias({"from"})
    private String messageFrom;

    @JsonAlias({"to"})
    private String messageTo;

    @JsonAlias({"source"})
    private String source;

    private String targetNumber;

    @JsonAlias({"message", "body"})
    private String messageContent;

    @JsonAlias({"mediaurls"})
    private String mediaUrls;

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

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getReceivedDatetime() {
        return receivedDatetime;
    }

    public void setReceivedDatetime(String receivedDatetime) {
        this.receivedDatetime = receivedDatetime;
    }
}
