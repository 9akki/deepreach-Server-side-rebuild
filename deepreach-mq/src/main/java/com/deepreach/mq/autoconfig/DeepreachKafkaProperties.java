package com.deepreach.mq.autoconfig;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一Kafka配置，前缀：deepreach.kafka
 */
@Data
@ConfigurationProperties(prefix = "deepreach.kafka")
public class DeepreachKafkaProperties {

    /**
     * 集群地址，支持多个
     */
    private List<String> bootstrapServers = Collections.singletonList("localhost:9092");

    /**
     * 是否启用Kafka自动配置
     */
    private boolean enabled = true;

    /**
     * Producer 端配置
     */
    private final Producer producer = new Producer();

    /**
     * Consumer 端配置
     */
    private final Consumer consumer = new Consumer();

    /**
     * 监听容器配置
     */
    private final Listener listener = new Listener();

    /**
     * 默认KafkaTemplate配置
     */
    private final Template template = new Template();

    @Data
    public static class Producer {

        private String acks = "all";
        private int retries = 3;
        private int batchSize = 16_384;
        private int lingerMs = 10;
        private long bufferMemory = 33_554_432L;
        private String compressionType = "zstd";
        private Class<?> keySerializer = org.apache.kafka.common.serialization.StringSerializer.class;
        private Class<?> valueSerializer = org.springframework.kafka.support.serializer.JsonSerializer.class;
    }

    @Data
    public static class Consumer {

        private String groupId = "deepreach-consumer";
        private String autoOffsetReset = "latest";
        private boolean enableAutoCommit = false;
        private Duration pollTimeout = Duration.ofSeconds(3);
        private Class<?> keyDeserializer = org.apache.kafka.common.serialization.StringDeserializer.class;
        private Class<?> valueDeserializer = org.springframework.kafka.support.serializer.JsonDeserializer.class;
        /**
         * 默认反序列化类型
         */
        private String valueType = "com.deepreach.common.core.mq.event.TranslationChargeEvent";
    }

    @Data
    public static class Listener {

        private int concurrency = 3;
        private String ackMode = "manual";
        private Duration idleEventInterval = Duration.ofMinutes(1);
        private int maxRetries = 3;
        private Duration backoffInterval = Duration.ofSeconds(5);
    }

    @Data
    public static class Template {

        private Duration requestTimeout = Duration.ofSeconds(30);
        private Duration transactionTimeout = Duration.ofSeconds(60);
        private boolean transactional = false;
        private String transactionIdPrefix = "deepreach-tx-";
        private String defaultTopic;
    }
}
