package com.deepreach.message.service;

import com.alibaba.fastjson2.JSONObject;

public interface SmsGatewayClient {

    SendResult send(SendCommand command);

    class SendCommand {
        private String to;
        private String source;
        private String body;
        private String imageUrl;

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    class SendResult {
        private boolean success;
        private String message;
        private String sourceId;
        private JSONObject rawPayload;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public JSONObject getRawPayload() {
            return rawPayload;
        }

        public void setRawPayload(JSONObject rawPayload) {
            this.rawPayload = rawPayload;
        }
    }
}
