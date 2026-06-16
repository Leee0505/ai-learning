#!/bin/bash
# ================================================================
# Start development services on 192.168.50.208 (Debian)
# Data directory: /home/dev/mydata/
# ================================================================

set -e

DATA_ROOT=/home/dev/mydata

echo "=== Starting MySQL ==="
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -v ${DATA_ROOT}/mysql/log:/var/log/mysql \
  -v ${DATA_ROOT}/mysql/conf:/etc/mysql/conf.d \
  -v ${DATA_ROOT}/mysql/data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=jw-0505 \
  mysql:8.0

echo "=== Starting Redis ==="
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v ${DATA_ROOT}/redis/data:/data \
  redis:latest

echo "=== Starting Zookeeper ==="
docker run -d \
  --name zookeeper \
  -p 2181:2181 \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  -v ${DATA_ROOT}/zookeeper/data:/var/lib/zookeeper/data \
  -v ${DATA_ROOT}/zookeeper/log:/var/lib/zookeeper/log \
  confluentinc/cp-zookeeper:7.5.0

echo "=== Starting Kafka ==="
docker run -d \
  --name kafka \
  -p 9092:9092 \
  --link zookeeper:zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.50.208:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -v ${DATA_ROOT}/kafka/data:/var/lib/kafka/data \
  confluentinc/cp-kafka:7.5.0

echo ""
echo "=== All services started ==="
echo "MySQL:    192.168.50.208:3306"
echo "Redis:    192.168.50.208:6379"
echo "Kafka:    192.168.50.208:9092"
echo "ZK:       192.168.50.208:2181"
echo ""
echo "Check status:  docker ps"
echo "Kafka test:    docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
