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

# 유저 두 명 만들고 인증까지
curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"a@codexray.com","password":"password123","nickname":"알파"}'
curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"b@codexray.com","password":"password123","nickname":"베타"}'
$DB -c "UPDATE users SET email_verified = true;" >/dev/null 2>&1

TOKEN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@codexray.com","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "=== 1) 정상 변경 알파→감마 — 기대: 200 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X PATCH $BASE/users/me/nickname \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nickname":"감마"}'
echo "DB 닉네임: $($DB -tAc "SELECT nickname FROM users WHERE email='a@codexray.com';")  (기대: 감마)"

echo ""
echo "=== 2) 남이 쓰는 닉네임(베타)으로 변경 — 기대: 409 NICKNAME_DUPLICATED ==="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/users/me/nickname \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nickname":"베타"}'

echo ""
echo "=== 3) 너무 짧은 닉네임(1자) — 기대: 400 검증 실패 ==="
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/users/me/nickname \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nickname":"x"}'

echo ""
echo "=== 4) 토큰 없이 — 기대: 401 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X PATCH $BASE/users/me/nickname \
  -H "Content-Type: application/json" -d '{"nickname":"델타"}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
