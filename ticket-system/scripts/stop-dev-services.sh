#!/bin/bash
# ================================================================
# Stop development services
# Usage: bash stop-dev-services.sh
# ================================================================

echo "Stopping services..."

for svc in kafka redis mysql; do
  if docker ps -a --format '{{.Names}}' | grep -q "^${svc}$"; then
    docker stop ${svc} && docker rm ${svc} && echo "${svc} stopped"
  else
    echo "${svc} not running — skipped"
  fi
done

# Optionally remove network
docker network rm ticket-dev 2>/dev/null && echo "network removed" || true

echo "Done."
