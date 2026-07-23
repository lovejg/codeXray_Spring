#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"
REDIS="docker exec codexray-redis redis-cli"

$DB -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" >/dev/null 2>&1
$REDIS FLUSHALL >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 120); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -60 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api

reg_and_login() {
  local email=$1 nick=$2
  curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\",\"nickname\":\"$nick\"}"
  local tok=$($DB -tAc "SELECT token FROM email_verification_token WHERE user_id=(SELECT id FROM users WHERE email='$email') ORDER BY id DESC LIMIT 1;")
  curl -s -o /dev/null -X POST $BASE/auth/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$tok\"}"
  curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}

A=$(reg_and_login "a@codexray.com" "유저에이"); B=$(reg_and_login "b@codexray.com" "유저비")
hA=(-H "Authorization: Bearer $A"); hB=(-H "Authorization: Bearer $B")
json=(-H "Content-Type: application/json")
BODY='{"task":"OPTIMIZE","code":"for i in range(n):\n  print(i)","language":"python","problemTitle":"반복문"}'
echo "A/B 준비 완료 (일일 한도 기본 2, ANTHROPIC 키 없음 → 분석은 503 예상)"

echo ""
echo "=== 1) 인증 없이 → 기대 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${json[@]}" -d "$BODY"

echo ""
echo "=== 2) 검증: task 누락 400 / 빈 code 400 (이건 한도 소비 안 함) ==="
echo -n "task 누락: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hA[@]}" "${json[@]}" -d '{"code":"x"}'
echo -n "빈 code:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hA[@]}" "${json[@]}" -d '{"task":"EXPLAIN","code":""}'

echo ""
echo "=== 3) A 분석 1회차 → 한도 통과, 키 없음이라 503 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hA[@]}" "${json[@]}" -d "$BODY"
echo "=== 4) A 분석 2회차 → 503 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hA[@]}" "${json[@]}" -d "$BODY"
echo "=== 5) A 분석 3회차 → 일일 한도 초과 429 (레이트리밋 동작) ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hA[@]}" "${json[@]}" -d "$BODY" | grep -oE '"errorCode":"[A-Z_]+"|HTTP [0-9]+'

echo ""
echo "=== 6) 사용자별 격리: B 1회차 → 429 아님(503) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ai/analyze "${hB[@]}" "${json[@]}" -d "$BODY"

echo ""
echo "=== 7) Redis 쿼터 키 확인 (A=3 시도, B=1 시도) ==="
AID=$($DB -tAc "SELECT id FROM users WHERE email='a@codexray.com';")
BID=$($DB -tAc "SELECT id FROM users WHERE email='b@codexray.com';")
TODAY=$(date +%F)
echo -n "A quota: "; $REDIS GET "ai:quota:$AID:$TODAY"
echo -n "B quota: "; $REDIS GET "ai:quota:$BID:$TODAY"
echo -n "A quota TTL(초, 24h≈86400): "; $REDIS TTL "ai:quota:$AID:$TODAY"

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
