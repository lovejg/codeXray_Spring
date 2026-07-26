#!/usr/bin/env bash
set -e
docker start codexray-db codexray-redis >/dev/null 2>&1 || true
sleep 2
echo "=== tables ==="
docker exec codexray-db psql -U postgres -d codexray -c "\dt"
echo "=== users count ==="
docker exec codexray-db psql -U postgres -d codexray -c "SELECT count(*) FROM users;"
