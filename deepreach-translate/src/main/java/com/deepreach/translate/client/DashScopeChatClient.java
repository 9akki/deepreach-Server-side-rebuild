package com.deepreach.translate.client;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

public class DashScopeChatClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeChatClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public DashScopeChatClient(OkHttpClient httpClient, String baseUrl, String apiKey) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public LlmResult chat(List<Message> messages, String model) {
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        JSONArray jsonMessages = new JSONArray();
        for (Message message : messages) {
            JSONObject obj = new JSONObject();
            obj.put("role", message.getRole());
            obj.put("content", message.getContent());
            jsonMessages.add(obj);
        }
        payload.put("messages", jsonMessages);

        RequestBody body = RequestBody.create(payload.toJSONString(), JSON);
        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .post(body)
            .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("DashScope 调用失败，HTTP " + response.code());
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("DashScope 响应缺少 choices 字段");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message != null ? message.getString("content") : "";
            long tokens = 0L;
            JSONObject usage = json.getJSONObject("usage");
            if (usage != null) {
                tokens = usage.getLongValue("total_tokens");
            }
            log.debug("DashScope model {} consumed {} tokens", model, tokens);
            return new LlmResult(content, tokens);
        } catch (IOException ex) {
            throw new IllegalStateException("DashScope 调用异常: " + ex.getMessage(), ex);
        }
    }
}
