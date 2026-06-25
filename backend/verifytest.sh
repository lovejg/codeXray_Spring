#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"

# 깨끗하게 시작
$DB -c "TRUNCATE users RESTART IDENTITY CASCADE;" >/dev/null 2>&1
$DB -c "TRUNCATE email_verification_token RESTART IDENTITY CASCADE;" >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!

for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -30 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api/auth

echo "=== 1) 가입 (기대: 201) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"verify@codexray.com","password":"password123","nickname":"인증맨"}'

# DB에서 방금 발급된 토큰 꺼내기
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
echo ""
echo "발급된 토큰: $TOKEN"

echo ""
echo "=== 2) 잘못된 토큰 (기대: 400 INVALID_TOKEN) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/verify-email -H "Content-Type: application/json" \
  -d '{"token":"not-a-real-token"}'

echo ""
echo "=== 3) 정상 인증 (기대: 200) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/verify-email -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}"

echo ""
echo "=== 4) 같은 토큰 재사용 (기대: 400 INVALID_TOKEN, 이미 사용됨) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/verify-email -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}"

echo ""
echo "=== DB 확인 (email_verified=t, used_at 채워짐 기대) ==="
$DB -c "SELECT id, email, email_verified FROM users;"
$DB -c "SELECT id, left(token,12) AS tok, used_at FROM email_verification_token;"

kill $APP_PID 2>/dev/null
echo "=== app stopped ==="
