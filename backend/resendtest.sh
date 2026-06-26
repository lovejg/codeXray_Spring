#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"

$DB -c "TRUNCATE users RESTART IDENTITY CASCADE;" >/dev/null 2>&1
$DB -c "TRUNCATE email_verification_token RESTART IDENTITY CASCADE;" >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -40 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api/auth

echo "=== 가입 (인증 안 한 상태) ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"re@codexray.com","password":"password123","nickname":"리센드"}'
echo "가입 직후 토큰 개수: $($DB -tAc "SELECT count(*) FROM email_verification_token;")"

echo ""
echo "=== 1) resend — 기대: 200, 토큰 1개 더 발급 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X POST $BASE/resend-verification -H "Content-Type: application/json" \
  -d '{"email":"re@codexray.com"}'
echo "resend 후 토큰 개수: $($DB -tAc "SELECT count(*) FROM email_verification_token;")  (기대: 2)"

echo ""
echo "=== 2) 가장 최근(resend된) 토큰으로 인증 — 기대: 200 ==="
NEWTOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X POST $BASE/verify-email -H "Content-Type: application/json" \
  -d "{\"token\":\"$NEWTOKEN\"}"
echo "email_verified: $($DB -tAc "SELECT email_verified FROM users WHERE email='re@codexray.com';")"

echo ""
echo "=== 3) 이미 인증된 계정에 resend — 기대: 409 ALREADY_VERIFIED ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/resend-verification -H "Content-Type: application/json" \
  -d '{"email":"re@codexray.com"}'

echo ""
echo "=== 4) 없는 이메일에 resend — 기대: 404 NOT_FOUND ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/resend-verification -H "Content-Type: application/json" \
  -d '{"email":"ghost@codexray.com"}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
