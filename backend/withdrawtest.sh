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

TOKEN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@codexray.com","password":"password123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "탈퇴 전 유저 수: $($DB -tAc "SELECT count(*) FROM users;")  (기대: 1)"

echo ""
echo "=== 1) 토큰 없이 탈퇴 — 기대: 401 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X DELETE $BASE/users/me

echo ""
echo "=== 2) 정상 탈퇴 — 기대: 204 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X DELETE $BASE/users/me \
  -H "Authorization: Bearer $TOKEN"
echo "탈퇴 후 유저 수: $($DB -tAc "SELECT count(*) FROM users;")  (기대: 0)"

echo ""
echo "=== 3) 탈퇴한 계정으로 /me 조회 — 기대: 404 (유저 없음) ==="
curl -s -w "\nHTTP %{http_code}\n" -X GET $BASE/users/me \
  -H "Authorization: Bearer $TOKEN"

echo ""
echo "=== 4) 탈퇴한 계정으로 로그인 — 기대: 401 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@codexray.com","password":"password123"}'

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
