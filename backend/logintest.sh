#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"

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

echo "=== 1) 가입 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"login@codexray.com","password":"password123","nickname":"로그인맨"}'

echo ""
echo "=== 2) 인증 전 로그인 시도 (기대: 403 EMAIL_NOT_VERIFIED) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"login@codexray.com","password":"password123"}'

# 이메일 인증 처리
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST $BASE/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$TOKEN\"}"

echo ""
echo "=== 3) 틀린 비밀번호 (기대: 401 LOGIN_FAILED) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"login@codexray.com","password":"wrongpass"}'

echo ""
echo "=== 4) 없는 이메일 (기대: 401 LOGIN_FAILED) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"nobody@codexray.com","password":"password123"}'

echo ""
echo "=== 5) 정상 로그인 (기대: 200 + accessToken) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"login@codexray.com","password":"password123"}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
