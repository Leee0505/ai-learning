#!/bin/bash
# ================================================================
# Stop development services
# ================================================================

echo "Stopping services..."

docker stop kafka 2>/dev/null && docker rm kafka 2>/dev/null && echo "Kafka stopped"
docker stop zookeeper 2>/dev/null && docker rm zookeeper 2>/dev/null && echo "Zookeeper stopped"
docker stop redis 2>/dev/null && docker rm redis 2>/dev/null && echo "Redis stopped"
docker stop mysql 2>/dev/null && docker rm mysql 2>/dev/null && echo "MySQL stopped"

echo "All services stopped."
