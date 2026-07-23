#!/usr/bin/env bash
cd ~/spring/codeXray/backend
docker start codexray-db codexray-redis >/dev/null 2>&1
./gradlew bootRun --console=plain > /tmp/feboot.log 2>&1 &
for i in $(seq 1 90); do
  grep -q "Started BackendApplication" /tmp/feboot.log 2>/dev/null && { echo BACKEND_UP; exit 0; }
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/feboot.log 2>/dev/null && { echo BACKEND_FAILED; tail -30 /tmp/feboot.log; exit 1; }
  sleep 1
done
echo TIMEOUT; tail -20 /tmp/feboot.log
