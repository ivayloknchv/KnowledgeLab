package com.knowledge.lab.api.config;

import com.knowledge.lab.api.messaging.event.AnnotationEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka infrastructure configuration.
 *
 * Key decisions:
 *  - Polymorphic JSON serialization using Jackson — same ObjectMapper as the web layer.
 *  - MANUAL_IMMEDIATE acknowledgment — consumer commits after all handlers complete.
 *  - Dead-letter topic (DLT) with 3-attempt fixed backoff before routing poison pills.
 *  - Topic auto-creation at startup via AdminClient — idempotent, safe to run repeatedly.
 *  - Producer uses {@code acks=all} + idempotence for exactly-once produce semantics.
 */
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper    objectMapper;

    @Bean
    public NewTopic annotationEventsTopic() {
        return TopicBuilder.name(kafkaProperties.getTopics().getAnnotationEvents())
                .partitions(kafkaProperties.getTopics().getPartitions())
                .replicas(kafkaProperties.getTopics().getReplicationFactor())
                // No .compact() — this is an event stream, not a key-value state store.
                // Log compaction would keep only the LATEST event per annotationId, silently
                // dropping all prior events (CREATED, UPDATED) whenever a newer one arrives.
                // Use time-based retention (KAFKA_LOG_RETENTION_HOURS in docker-compose) instead.
                .build();
    }

    @Bean
    public NewTopic annotationEventsDltTopic() {
        return TopicBuilder.name(kafkaProperties.getTopics().getAnnotationEventsDlt())
                .partitions(1)
                .replicas(kafkaProperties.getTopics().getReplicationFactor())
                .build();
    }

    @Bean
    public ProducerFactory<String, AnnotationEvent> annotationEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,    StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,  JacksonJsonSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG,                 kafkaProperties.getProducer().getRetries());
        props.put(ProducerConfig.ACKS_CONFIG,                    kafkaProperties.getProducer().getAcks());
        props.put(ProducerConfig.LINGER_MS_CONFIG,               kafkaProperties.getProducer().getLingerMs());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,              kafkaProperties.getProducer().getBatchSize());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,        kafkaProperties.getProducer().getCompressionType());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,      true);  // exactly-once produce
        // Reduce max.block.ms from the 60 s default so a broker hiccup surfaces quickly
        // rather than holding @Async threads for a full minute.
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,            10_000);

        var factory = new DefaultKafkaProducerFactory<String, AnnotationEvent>(props);
        // Reuse the app's ObjectMapper (has JSR-310, polymorphic type info, etc.)
        factory.setValueSerializer(new JacksonJsonSerializer<>((JsonMapper) objectMapper));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, AnnotationEvent> kafkaTemplate() {
        return new KafkaTemplate<>(annotationEventProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, AnnotationEvent> annotationEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                   kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   JacksonJsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,           kafkaProperties.getConsumer().getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,         false);     // manual ack
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest");
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,         "com.knowledge.lab.api.messaging.event");
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS,    false);     // use @JsonTypeInfo in payload

        var deserializer = new JacksonJsonDeserializer<>(AnnotationEvent.class, (JsonMapper) objectMapper, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnnotationEvent>
    annotationEventListenerContainerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, AnnotationEvent>();
        factory.setConsumerFactory(annotationEventConsumerFactory());
        factory.setConcurrency(kafkaProperties.getConsumer().getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * After 3 attempts with a 1-second fixed backoff, route the message
     * to the dead-letter topic rather than blocking the partition forever.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate(),
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        kafkaProperties.getTopics().getAnnotationEventsDlt(), 0
                )
        );

        var backoff = new FixedBackOff(1_000L, 3L);  // 1 s × 3 attempts
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);  // don't retry malformed msgs
        return handler;
    }
}
