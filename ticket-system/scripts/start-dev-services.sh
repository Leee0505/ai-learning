#!/bin/bash
# ================================================================
# Start development services on 192.168.50.208 (Debian)
# Data directory: /home/dev/mydata/  (data survives container removal)
# Usage: bash start-dev-services.sh
# ================================================================

set -e

DATA_ROOT=/home/dev/mydata
NETWORK=ticket-dev

# ── Ensure network exists ──
docker network inspect ${NETWORK} >/dev/null 2>&1 || \
  docker network create ${NETWORK}

# ── Ensure data directories exist with correct permissions ──
mkdir -p ${DATA_ROOT}/mysql/log ${DATA_ROOT}/mysql/conf ${DATA_ROOT}/mysql/data
mkdir -p ${DATA_ROOT}/redis/data
mkdir -p ${DATA_ROOT}/kafka/data
# Kafka container runs as appuser (uid=1000)
chown -R 1000:1000 ${DATA_ROOT}/kafka/data 2>/dev/null || true

# ── Helpers ──
replace_container() {
  local name=$1
  local image=$2
  shift 2
  if docker ps -a --format '{{.Names}}' | grep -q "^${name}$"; then
    echo "--- ${name} exists, stopping & removing old container ---"
    docker stop ${name} 2>/dev/null || true
    docker rm ${name} 2>/dev/null || true
  fi
  echo "--- Starting ${name} (image: ${image}) ---"
  docker run -d \
    --name ${name} \
    --network ${NETWORK} \
    --restart unless-stopped \
    "$@" \
    ${image}
}

# ── MySQL ──
replace_container mysql mysql:8.0 \
  -p 3306:3306 \
  -v ${DATA_ROOT}/mysql/log:/var/log/mysql \
  -v ${DATA_ROOT}/mysql/conf:/etc/mysql/conf.d \
  -v ${DATA_ROOT}/mysql/data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=jw-0505

# ── Redis ──
replace_container redis redis:7-alpine \
  -p 6379:6379 \
  -v ${DATA_ROOT}/redis/data:/data

# ── Kafka (KRaft — no Zookeeper) ──
replace_container kafka confluentinc/cp-kafka:7.5.0 \
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
  -v ${DATA_ROOT}/kafka/data:/var/lib/kafka/data

echo ""
echo "=== All services running ==="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' --filter "name=mysql|redis|kafka"
echo ""
echo "Kafka test:  docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
