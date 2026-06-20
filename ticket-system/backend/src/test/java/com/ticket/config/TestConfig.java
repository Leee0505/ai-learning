package com.ticket.config;

import com.ticket.event.EventPublisher;
import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = Mockito.mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> mockBucket = (RBucket<String>) Mockito.mock(RBucket.class);
        when(mockBucket.isExists()).thenReturn(false);
        doReturn(mockBucket).when(mockClient).getBucket(anyString());

        // Mock rate limit counter — always returns 1 (below limit)
        RAtomicLong mockCounter = Mockito.mock(RAtomicLong.class);
        when(mockCounter.incrementAndGet()).thenReturn(1L);
        doReturn(mockCounter).when(mockClient).getAtomicLong(anyString());

        return mockClient;
    }

    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return Mockito.mock(EventPublisher.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return (KafkaTemplate<String, Object>) Mockito.mock(KafkaTemplate.class);
    }
}
