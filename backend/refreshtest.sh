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
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -30 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api/auth

# 가입 + 이메일 인증
curl -s -o /dev/null -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"refresh@codexray.com","password":"password123","nickname":"리프레시"}'
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST $BASE/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$TOKEN\"}"

echo "=== 로그인 (응답 헤더 포함, -i) ==="
echo "기대: 200 / 본문에 accessToken / Set-Cookie: refreshToken=...; HttpOnly"
curl -s -i -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"refresh@codexray.com","password":"password123"}'

echo ""
echo ""
echo "=== Redis 확인 (refresh:* 키, 값=userId, TTL) ==="
KEY=$($REDIS --scan --pattern 'refresh:*' | head -1)
echo "key: $KEY"
echo "value(userId): $($REDIS GET "$KEY")"
echo "TTL(seconds): $($REDIS TTL "$KEY")"

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
