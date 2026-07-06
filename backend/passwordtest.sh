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

BASE=http://localhost:8080/api

curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"a@codexray.com","password":"password123","nickname":"알파"}'
$DB -c "UPDATE users SET email_verified = true;" >/dev/null 2>&1

login() {
  curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
    -d "{\"email\":\"a@codexray.com\",\"password\":\"$1\"}"
}

TOKEN=$(login password123 | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "=== 1) 현재 비번 틀림 — 기대: 400 PASSWORD_MISMATCH ==="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/users/me/password \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"currentPassword":"wrongpass","newPassword":"newpass123"}'

echo ""
echo "=== 2) 정상 변경 — 기대: 200 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X PATCH $BASE/users/me/password \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"currentPassword":"password123","newPassword":"newpass123"}'

echo ""
echo "=== 3) 옛 비번으로 로그인 — 기대: 401 (이미 바뀜) ==="
echo "$(login password123 | grep -o '"errorCode":"[^"]*"')  로그인응답"

echo ""
echo "=== 4) 새 비번으로 로그인 — 기대: 200 + accessToken ==="
NEWTOKEN=$(login newpass123 | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
[ -n "$NEWTOKEN" ] && echo "새 비번 로그인 성공 (토큰 발급됨)" || echo "실패!"

echo ""
echo "=== 5) newPassword 너무 짧음(7자) — 기대: 400 검증 실패 ==="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/users/me/password \
  -H "Authorization: Bearer $NEWTOKEN" -H "Content-Type: application/json" \
  -d '{"currentPassword":"newpass123","newPassword":"short12"}'

echo ""
echo "=== 6) 토큰 없이 — 기대: 401 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X PATCH $BASE/users/me/password \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"newpass123","newPassword":"whatever123"}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
