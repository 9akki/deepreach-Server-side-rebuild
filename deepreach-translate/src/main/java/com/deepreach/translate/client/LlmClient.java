package com.deepreach.translate.client;

import java.util.List;

public interface LlmClient {

    LlmResult chat(List<Message> messages, String model);
}
