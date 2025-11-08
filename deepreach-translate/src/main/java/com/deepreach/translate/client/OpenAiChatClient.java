package com.deepreach.translate.client;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAiChatClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);
    private final OpenAiService openAiService;

    public OpenAiChatClient(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public LlmResult chat(List<Message> messages, String model) {
        List<ChatMessage> chatMessages = messages.stream()
            .map(message -> new ChatMessage(message.getRole(), message.getContent()))
            .collect(Collectors.toList());
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(chatMessages)
            .build();
        ChatCompletionResult result = openAiService.createChatCompletion(request);
        String content = result.getChoices()
            .stream()
            .findFirst()
            .map(choice -> choice.getMessage().getContent())
            .orElse("");
        long tokens = result.getUsage() != null ? result.getUsage().getTotalTokens() : 0L;
        log.debug("OpenAI model {} consumed {} tokens", model, tokens);
        return new LlmResult(content, tokens);
    }
}
