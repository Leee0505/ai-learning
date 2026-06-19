package com.ticket.service.impl;

import com.ticket.config.ApiMetricsInterceptor;
import com.ticket.dto.response.*;
import com.ticket.service.MonitorService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class MonitorServiceImpl implements MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorServiceImpl.class);

    private final RedissonClient redissonClient;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ApiMetricsInterceptor apiMetrics;
    private final String kafkaBootstrapServers;

    public MonitorServiceImpl(RedissonClient redissonClient,
                              RedisConnectionFactory redisConnectionFactory,
                              ApiMetricsInterceptor apiMetrics,
                              @Value("${spring.kafka.bootstrap-servers}") String kafkaBootstrapServers) {
        this.redissonClient = redissonClient;
        this.redisConnectionFactory = redisConnectionFactory;
        this.apiMetrics = apiMetrics;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    @Override
    public MonitorOverviewResponse getOverview() {
        KafkaMetricsResponse kafka = getKafkaMetrics();
        RedisMetricsResponse redis = getRedisMetrics();
        ApiMetricsResponse api = getApiMetrics();

        return MonitorOverviewResponse.builder()
                .kafkaStatus(kafka.isConnected() ? "HEALTHY" : "CRITICAL")
                .redisStatus(redis.isConnected() ?
                        (redis.getHitRate() > 0.7 ? "HEALTHY" : redis.getHitRate() > 0.5 ? "WARNING" : "CRITICAL") :
                        "CRITICAL")
                .apiStatus(api.getP95Ms() < 200 ? "HEALTHY" : api.getP95Ms() < 500 ? "WARNING" : "CRITICAL")
                .build();
    }

    @Override
    public KafkaMetricsResponse getKafkaMetrics() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        try (AdminClient admin = AdminClient.create(props)) {
            var groups = admin.listConsumerGroups().all().get(3, TimeUnit.SECONDS);
            var cluster = admin.describeCluster().nodes().get(3, TimeUnit.SECONDS);
            return KafkaMetricsResponse.builder()
                    .connected(true)
                    .activeConsumers(groups.size())
                    .totalPartitions(0) // placeholder: would need per-topic describe
                    .build();
        } catch (Exception e) {
            log.warn("Kafka metrics unavailable: {}", e.getMessage());
            return KafkaMetricsResponse.builder().connected(false).build();
        }
    }

    @Override
    public RedisMetricsResponse getRedisMetrics() {
        try {
            // Check connectivity via a simple Redisson ping
            redissonClient.getAtomicLong("monitor:ping").get();
            boolean connected = true;

            // Use RedisConnectionFactory for INFO stats (bypasses Redisson abstraction)
            Properties info = redisConnectionFactory.getConnection().serverCommands().info("stats");
            long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
            long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total : 1.0;

            Properties keyspace = redisConnectionFactory.getConnection().serverCommands().info("keyspace");
            long keys = 0;
            if (keyspace != null) {
                for (String key : keyspace.stringPropertyNames()) {
                    if (key.startsWith("db")) {
                        String[] parts = keyspace.getProperty(key).split(",");
                        for (String part : parts) {
                            if (part.startsWith("keys=")) {
                                keys += Long.parseLong(part.substring(5));
                            }
                        }
                    }
                }
            }

            Properties memory = redisConnectionFactory.getConnection().serverCommands().info("memory");
            long usedMemory = Long.parseLong(memory.getProperty("used_memory", "0"));

            return RedisMetricsResponse.builder()
                    .connected(connected)
                    .hitRate(hitRate)
                    .totalKeys(keys)
                    .usedMemoryBytes(usedMemory)
                    .build();
        } catch (Exception e) {
            log.warn("Redis metrics unavailable: {}", e.getMessage());
            return RedisMetricsResponse.builder().connected(false).build();
        }
    }

    @Override
    public ApiMetricsResponse getApiMetrics() {
        return ApiMetricsResponse.builder()
                .requestCount(apiMetrics.getRequestCount())
                .errorCount(apiMetrics.getErrorCount())
                .p50Ms(apiMetrics.getP50Ms())
                .p95Ms(apiMetrics.getP95Ms())
                .p99Ms(apiMetrics.getP99Ms())
                .build();
    }
}
