package com.deepreach.translate.client;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.deepreach.common.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 基于 Azure OpenAI SDK 的 LLM 客户端实现。
 */
public class AzureOpenAiChatClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiChatClient.class);

    private final OpenAIClient client;
    private final Map<String, String> deploymentMappings;

    public AzureOpenAiChatClient(OpenAIClient client, Map<String, String> deploymentMappings) {
        this.client = client;
        this.deploymentMappings = deploymentMappings;
    }

    @Override
    public LlmResult chat(List<Message> messages, String model) {
        String deploymentName = resolveDeployment(model);
        List<ChatRequestMessage> azureMessages = convertMessages(messages);
        ChatCompletionsOptions options = new ChatCompletionsOptions(azureMessages)
            .setModel(deploymentName);

        ChatCompletions completions = client.getChatCompletions(deploymentName, options);
        String content = Optional.ofNullable(completions.getChoices())
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0).getMessage())
            .map(ChatResponseMessage::getContent)
            .filter(StringUtils::hasText)
            .orElseThrow(() -> new ServiceException("Azure OpenAI响应为空"));
        long totalTokens = completions.getUsage() != null
            ? completions.getUsage().getTotalTokens()
            : 0L;
        log.debug("Azure deployment {} consumed {} tokens", deploymentName, totalTokens);
        return new LlmResult(content, totalTokens);
    }

    private String resolveDeployment(String model) {
        if (!StringUtils.hasText(model)) {
            throw new ServiceException("Azure OpenAI调用缺少模型/渠道标识");
        }
        String normalized = model.trim().toLowerCase(Locale.ROOT);
        String deployment = null;
        if (deploymentMappings != null) {
            deployment = deploymentMappings.get(normalized);
            if (deployment == null) {
                deployment = deploymentMappings.get(model.trim());
            }
        }
        if (!StringUtils.hasText(deployment)) {
            throw new ServiceException("未配置渠道 " + model + " 对应的 Azure 部署名称");
        }
        return deployment;
    }

    private List<ChatRequestMessage> convertMessages(List<Message> messages) {
        List<ChatRequestMessage> converted = new ArrayList<>();
        if (messages == null) {
            return converted;
        }
        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            String content = message.getContent();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            converted.add(buildRequestMessage(message.getRole(), content));
        }
        if (converted.isEmpty()) {
            throw new ServiceException("Azure OpenAI请求消息为空");
        }
        return converted;
    }

    private ChatRequestMessage buildRequestMessage(String role, String content) {
        if (!StringUtils.hasText(role)) {
            return new ChatRequestUserMessage(content);
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "system":
                return new ChatRequestSystemMessage(content);
            case "assistant":
                return new ChatRequestAssistantMessage(content);
            case "user":
            default:
                return new ChatRequestUserMessage(content);
        }
    }
}
