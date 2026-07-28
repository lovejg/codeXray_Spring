#!/usr/bin/env bash
# Keeps this shell alive (foreground vite) so the WSL session/distro stays up.
docker start codexray-db codexray-redis codexray-kafka >/dev/null 2>&1
cd ~/spring/codeXray/backend
./gradlew bootRun --console=plain > /tmp/run_back.log 2>&1 &
BACK=$!
echo "backend pid=$BACK, waiting for start..."
for i in $(seq 1 120); do
  grep -q "Started BackendApplication" /tmp/run_back.log 2>/dev/null && { echo "BACKEND_UP"; break; }
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/run_back.log 2>/dev/null && { echo "BACKEND_FAILED"; tail -25 /tmp/run_back.log; exit 1; }
  sleep 1
done
cd ~/spring/codeXray/frontend
echo "starting vite (foreground)..."
exec npm run dev -- --host
