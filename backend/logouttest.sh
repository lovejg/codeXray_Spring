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
JAR=/tmp/cookies.txt
rm -f $JAR

curl -s -o /dev/null -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"out@codexray.com","password":"password123","nickname":"로그아웃"}'
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST $BASE/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$TOKEN\"}"

echo "=== 1) 로그인 ==="
curl -s -c $JAR -o /dev/null -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"out@codexray.com","password":"password123"}'
RT=$(grep refreshToken $JAR | awk '{print $NF}')
echo "refresh: $RT"
echo "Redis 존재? $($REDIS EXISTS refresh:$RT)  (기대: 1)"

echo ""
echo "=== 2) 로그아웃 (쿠키 첨부) — 기대: 200 + Set-Cookie Max-Age=0 ==="
curl -s -i -b $JAR -X POST $BASE/logout | grep -E "HTTP/|Set-Cookie"

echo ""
echo "=== 3) 로그아웃 후 상태 ==="
echo "Redis 존재? $($REDIS EXISTS refresh:$RT)  (기대: 0, 삭제됨)"

echo ""
echo "=== 4) 로그아웃한 토큰으로 refresh 재시도 — 기대: 401 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/refresh -H "Cookie: refreshToken=$RT"

echo ""
echo "=== 5) 쿠키 없이 로그아웃 — 기대: 200 (에러 없이 통과) ==="
curl -s -w "\nHTTP %{http_code}\n" -o /dev/null -X POST $BASE/logout

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
