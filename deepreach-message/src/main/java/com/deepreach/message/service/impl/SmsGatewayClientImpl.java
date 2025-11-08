package com.deepreach.message.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.deepreach.common.exception.ServiceException;
import com.deepreach.message.config.MessageSmsProperties;
import com.deepreach.message.service.SmsGatewayClient;
import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmsGatewayClientImpl implements SmsGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayClientImpl.class);

    private final OkHttpClient okHttpClient;
    private final MessageSmsProperties properties;
    private volatile long lastSentTime;

    public SmsGatewayClientImpl(OkHttpClient messageOkHttpClient, MessageSmsProperties properties) {
        this.okHttpClient = messageOkHttpClient;
        this.properties = properties;
        this.lastSentTime = 0L;
    }

    @Override
    public SendResult send(SendCommand command) {
        if (!StringUtils.hasText(command.getTo())) {
            throw new ServiceException("短信目标号码不能为空");
        }
        if (!StringUtils.hasText(command.getBody())) {
            throw new ServiceException("短信内容不能为空");
        }

        throttle();
        String url = properties.getBaseUrl() + properties.getSendPath();
        FormBody.Builder builder = new FormBody.Builder()
            .add("accountid", properties.getAccountId())
            .add("apikey", properties.getApiKey())
            .add("to", command.getTo())
            .add("body", command.getBody())
            .add("source", StringUtils.hasText(command.getSource()) ? command.getSource() : properties.getSenderNumber());
        if (StringUtils.hasText(command.getImageUrl())) {
            builder.add("imageurl", command.getImageUrl());
        }
        FormBody formBody = builder.build();
        Request request = new Request.Builder()
            .url(url)
            .header(HttpHeaders.ACCEPT, "application/json")
            .post(formBody)
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Sms gateway error httpCode={} body={}", response.code(), responseBody);
                throw new ServiceException("短信网关请求失败，HTTP " + response.code());
            }
            JSONObject json = JSONObject.parseObject(responseBody);
            SendResult result = new SendResult();
            int code = json.getIntValue("code", 500);
            result.setSuccess(code == 200);
            result.setMessage(json.getString("message"));
            JSONObject data = json.getJSONObject("data");
            if (data != null) {
                result.setSourceId(data.getString("source"));
            }
            result.setRawPayload(json);
            return result;
        } catch (IOException ex) {
            throw new ServiceException("短信网关调用异常: " + ex.getMessage(), ex);
        }
    }

    private synchronized void throttle() {
        if (properties.getRateLimitMs() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = Math.max(1, properties.getRateLimitMs());
        long wait = interval - (now - lastSentTime);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastSentTime = System.currentTimeMillis();
    }
}
