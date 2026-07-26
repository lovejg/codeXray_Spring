#!/usr/bin/env bash
set -e
echo "=== BEFORE ==="
docker exec codexray-db psql -U postgres -d codexray -c "SELECT count(*) users FROM users;"
docker exec codexray-db psql -U postgres -d codexray -c "TRUNCATE users, email_verification_token RESTART IDENTITY CASCADE;"
echo "=== AFTER ==="
docker exec codexray-db psql -U postgres -d codexray -c "SELECT count(*) users FROM users;"
docker exec codexray-db psql -U postgres -d codexray -c "SELECT count(*) tokens FROM email_verification_token;"
echo "=== problems catalog kept ==="
docker exec codexray-db psql -U postgres -d codexray -c "SELECT count(*) problems FROM problems;"
echo "=== flush redis refresh tokens ==="
docker exec codexray-redis redis-cli FLUSHALL
