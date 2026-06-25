#!/usr/bin/env bash
cd ~/spring/codeXray/backend

# 깨끗하게 시작: 기존 데이터 비우기(테스트 반복 대비)
docker exec codexray-db psql -U postgres -d codexray -c "TRUNCATE users RESTART IDENTITY CASCADE;" >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!

# 부팅 대기
for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -30 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api/auth/register

echo "=== 1) 정상 가입 (기대: 201) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE -H "Content-Type: application/json" \
  -d '{"email":"test@codexray.com","password":"password123","nickname":"테스터"}'

echo ""
echo "=== 2) 같은 이메일 재가입 (기대: 409) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE -H "Content-Type: application/json" \
  -d '{"email":"test@codexray.com","password":"password123","nickname":"다른닉"}'

echo ""
echo "=== 3) 검증 실패 (기대: 400 + fieldErrors) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE -H "Content-Type: application/json" \
  -d '{"email":"bad@x.com","password":"123","nickname":"x"}'

echo ""
echo "=== DB 확인 ==="
docker exec codexray-db psql -U postgres -d codexray \
  -c "SELECT id, email, nickname, role, provider, email_verified, left(password,12) AS pw_prefix FROM users;"

kill $APP_PID 2>/dev/null
echo "=== app stopped ==="
