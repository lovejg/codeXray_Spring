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

# 가입 + 인증
curl -s -o /dev/null -X POST $BASE/register -H "Content-Type: application/json" \
  -d '{"email":"rot@codexray.com","password":"password123","nickname":"로테이션"}'
TOKEN=$($DB -tAc "SELECT token FROM email_verification_token ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST $BASE/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$TOKEN\"}"

echo "=== 1) 로그인 (쿠키를 $JAR 에 저장) ==="
curl -s -c $JAR -o /dev/null -X POST $BASE/login -H "Content-Type: application/json" \
  -d '{"email":"rot@codexray.com","password":"password123"}'
OLD_RT=$(grep refreshToken $JAR | awk '{print $NF}')
echo "발급된 refresh(옛): $OLD_RT"
echo "Redis 키 개수: $($REDIS --scan --pattern 'refresh:*' | wc -l)  (기대: 1)"

echo ""
echo "=== 2) refresh 호출 (옛 쿠키 -b, 새 쿠키 -c 로 갱신) ==="
curl -s -i -b $JAR -c $JAR -X POST $BASE/refresh | grep -E "HTTP/|Set-Cookie|accessToken"
NEW_RT=$(grep refreshToken $JAR | awk '{print $NF}')
echo "발급된 refresh(새): $NEW_RT"

echo ""
echo "=== 3) rotation 확인 ==="
echo "옛 != 새 ?  $([ "$OLD_RT" != "$NEW_RT" ] && echo YES || echo NO)"
echo "Redis 키 개수: $($REDIS --scan --pattern 'refresh:*' | wc -l)  (기대: 1, 옛것 삭제됨)"
echo "옛 토큰 Redis 존재? $($REDIS EXISTS refresh:$OLD_RT)  (기대: 0)"
echo "새 토큰 Redis 존재? $($REDIS EXISTS refresh:$NEW_RT)  (기대: 1)"

echo ""
echo "=== 4) 옛 토큰으로 refresh 재시도 (기대: 401) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/refresh \
  -H "Cookie: refreshToken=$OLD_RT"

kill $APP_PID 2>/dev/null
echo ""
echo "=== app stopped ==="
