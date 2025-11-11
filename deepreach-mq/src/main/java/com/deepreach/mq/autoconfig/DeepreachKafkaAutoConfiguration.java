package com.deepreach.mq.autoconfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka自动配置：统一Producer/Consumer基础设施
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeepreachKafkaProperties.class)
@ConditionalOnProperty(prefix = "deepreach.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeepreachKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> kafkaProducerFactory(DeepreachKafkaProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, join(properties.getBootstrapServers()));
        config.put(ProducerConfig.ACKS_CONFIG, properties.getProducer().getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, properties.getProducer().getRetries());
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getProducer().getBatchSize());
        config.put(ProducerConfig.LINGER_MS_CONFIG, properties.getProducer().getLingerMs());
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, properties.getProducer().getBufferMemory());
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, properties.getProducer().getCompressionType());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();
        jsonSerializer.setAddTypeInfo(true);
        factory.setValueSerializer(jsonSerializer);
        if (properties.getTemplate().isTransactional()) {
            factory.setTransactionIdPrefix(properties.getTemplate().getTransactionIdPrefix());
        }
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory,
                                                       DeepreachKafkaProperties properties) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        if (StringUtils.hasText(properties.getTemplate().getDefaultTopic())) {
            template.setDefaultTopic(properties.getTemplate().getDefaultTopic());
        }
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "deepreach.kafka.template", name = "transactional", havingValue = "true")
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> kafkaConsumerFactory(DeepreachKafkaProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, join(properties.getBootstrapServers()));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumer().getGroupId());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getConsumer().getAutoOffsetReset());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getConsumer().isEnableAutoCommit());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, properties.getConsumer().getValueType());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>();
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
    }

    @Bean(name = "deepreachKafkaListenerFactory")
    @ConditionalOnMissingBean(name = "deepreachKafkaListenerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> deepreachKafkaListenerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        DefaultErrorHandler kafkaErrorHandler,
        DeepreachKafkaProperties properties) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.getListener().getConcurrency());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setPollTimeout(properties.getConsumer().getPollTimeout().toMillis());
        factory.getContainerProperties().setIdleEventInterval(properties.getListener().getIdleEventInterval().toMillis());
        ContainerProperties.AckMode ackMode = ContainerProperties.AckMode
            .valueOf(properties.getListener().getAckMode().toUpperCase());
        factory.getContainerProperties().setAckMode(ackMode);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultErrorHandler kafkaErrorHandler(DeepreachKafkaProperties properties) {
        long interval = properties.getListener().getBackoffInterval().toMillis();
        return new DefaultErrorHandler(new FixedBackOff(interval, properties.getListener().getMaxRetries()));
    }

    private String join(List<String> servers) {
        return String.join(",", servers);
    }
}
