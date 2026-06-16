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

echo "=== Starting Kafka (KRaft — no Zookeeper) ==="
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://192.168.50.208:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  -e CLUSTER_ID=ticket-dev-cluster-001 \
  -v ${DATA_ROOT}/kafka/data:/var/lib/kafka/data \
  confluentinc/cp-kafka:7.5.0

echo ""
echo "=== All services started ==="
echo "MySQL:    192.168.50.208:3306"
echo "Redis:    192.168.50.208:6379"
echo "Kafka:    192.168.50.208:9092 (KRaft — no Zookeeper)"
echo ""
echo "Check status:  docker ps"
echo "Kafka test:    docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
