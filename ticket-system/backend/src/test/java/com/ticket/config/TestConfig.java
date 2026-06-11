package com.ticket.config;

import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
        return mockClient;
    }
}
