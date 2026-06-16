package com.ticket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka reliability configuration.
 *
 * Producer idempotence is enabled via spring.kafka.producer.properties.enable.idempotence=true
 * in application.yml — this prevents duplicates caused by producer retries
 * (Kafka detects and discards duplicates via Producer ID + sequence number).
 *
 * Consumer: this DefaultErrorHandler retries failed records 3 times with
 * 1-second backoff, then logs and skips the poison record so the consumer
 * is never blocked.
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // 3 retries, 1 second apart, then skip
        var backOff = new FixedBackOff(1000L, 3L);
        var handler = new DefaultErrorHandler((record, exception) -> {
            log.error("Kafka consumer exhausted retries — skipping record: topic={} partition={} offset={} key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), exception);
        }, backOff);
        // Do not retry on deserialization errors (poison pills)
        handler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class);
        return handler;
    }
}
