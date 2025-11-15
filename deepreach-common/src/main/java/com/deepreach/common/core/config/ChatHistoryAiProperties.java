package com.deepreach.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "chat.history.ai")
public class ChatHistoryAiProperties {

    /**
     * 聊天排序兜底接口地址，例如 http://206.82.1.18:7809/chat_object/user_profile
     */
    private String sortEndpoint = "http://206.82.1.18:7809/chat_object/user_profile";

    /**
     * 请求超时时间（毫秒）
     */
    private long timeoutMs = 5000L;

    /**
     * 调用排序接口时使用的指令
     */
    private String sortInstruction = "history 中的记录顺序可能错乱，请输出按正常时间顺序排列的 JSON 数组，"
        + "保持每条字段为 {\"role\",\"contact\",\"content\",\"timestamp\"}，必要时可生成合理的 timestamp。";

    /**
     * 排序场景的人设描述
     */
    private String sortCharacter = "你是一个擅长整理聊天记录的系统助手。";

    /**
     * 用户画像生成接口地址，例如 http://206.82.1.18:7809/chat_object/user_profile
     */
    private String profileEndpoint = "http://206.82.1.18:7809/chat_object/user_profile";

    /**
     * 用户画像生成提示
     */
    private String profileInstruction = "请根据 history 输出联系人画像 JSON，字段包括 summary/intent/tone/keywords/preferences/historyCoverage/lastUpdated。";

    /**
     * 画像场景的人设
     */
    private String profileCharacter = "你是一个擅长总结联系人画像的分析师。";

    /**
     * 是否启用画像刷新
     */
    private boolean profileEnabled = true;

    /**
     * 画像刷新间隔（毫秒）
     */
    private long profileRefreshIntervalMs = 600_000L;

    /**
     * 每批处理的画像数量
     */
    private int profileBatchSize = 50;
}
