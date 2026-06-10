package com.knowledge.lab.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed binding for all {@code kafka.*} YAML properties.
 *
 * Centralizes every Kafka knob in one place - {@code @Value}
 * annotations across the codebase.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private Topics   topics   = new Topics();
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();

    @Getter @Setter
    public static class Topics {

        /** Main topic carrying all annotation domain events. */
        private String annotationEvents = "annotation-events";

        /** Dead-letter topic for poison messages. */
        private String annotationEventsDlt = "annotation-events.DLT";

        /** Number of partitions — increase for higher throughput. */
        private int partitions = 3;

        /** Replication factor — set to ≥ 2 in production. */
        private short replicationFactor = 1;
    }

    @Getter
    @Setter
    public static class Producer {
        private int    retries           = 3;
        private String acks              = "all";          // strongest durability guarantee
        private int    lingerMs          = 5;              // small batching window
        private int    batchSize         = 16_384;         // 16 KB
        private String compressionType   = "snappy";
    }

    @Getter
    @Setter
    public static class Consumer {
        private String groupId          = "knowledge-lab-annotation-group";
        private int    concurrency      = 3;               // threads = partitions
        private int    maxPollRecords   = 50;
        private long   pollTimeoutMs    = 3_000;
    }
}
