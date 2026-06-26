#!/usr/bin/env bash
cd ~/spring/codeXray/backend

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -40 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080

echo "=== 1) 토큰 없이 보호 경로 접근 — 기대: 401 + ErrorResponse JSON ==="
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/users/me

echo ""
echo "=== 2) 엉터리 토큰으로 접근 — 기대: 401 ==="
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/users/me -H "Authorization: Bearer not.a.valid.token"

echo ""
echo "=== 3) auth 경로는 토큰 없이도 열림 (permitAll) — 가입 400(검증) 정도 기대, 401 아님 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/auth/register -H "Content-Type: application/json" -d '{}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
