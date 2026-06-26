#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"
REDIS="docker exec codexray-redis redis-cli"

$DB -c "TRUNCATE users RESTART IDENTITY CASCADE;" >/dev/null 2>&1
$DB -c "TRUNCATE email_verification_token RESTART IDENTITY CASCADE;" >/dev/null 2>&1
$REDIS FLUSHALL >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -40 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api

curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"me@codexray.com","password":"password123","nickname":"미미"}'
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST $BASE/auth/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$TOKEN\"}"

# 로그인해서 access token 추출 (jq 없이 grep/sed로)
ACCESS=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"me@codexray.com","password":"password123"}' \
  | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo "access token(앞 30자): ${ACCESS:0:30}..."

echo ""
echo "=== 1) 유효한 토큰으로 /me — 기대: 200 + 내 정보(비번 없음) ==="
curl -s -w "\nHTTP %{http_code}\n" $BASE/users/me -H "Authorization: Bearer $ACCESS"

echo ""
echo "=== 2) 토큰 없이 /me — 기대: 401 ==="
curl -s -w "\nHTTP %{http_code}\n" $BASE/users/me

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
