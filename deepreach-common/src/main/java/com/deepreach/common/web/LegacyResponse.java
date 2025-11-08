package com.deepreach.common.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 构造与 Python 服务相同的返回结构：{"success":true/false,"data":...,"message":""}
 */
public final class LegacyResponse {

    private LegacyResponse() {
    }

    public static Map<String, Object> success() {
        return success(null);
    }

    public static Map<String, Object> success(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        if (data != null) {
            body.put("data", data);
        }
        return body;
    }

    public static Map<String, Object> message(String message) {
        Map<String, Object> body = success();
        body.put("message", message);
        return body;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }

    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> body = error(message);
        body.put("code", code);
        return body;
    }
}
