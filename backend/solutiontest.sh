#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"
REDIS="docker exec codexray-redis redis-cli"

# Flyway가 V1~V3 + 시드를 새로 깔도록 스키마 초기화
$DB -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" >/dev/null 2>&1
$REDIS FLUSHALL >/dev/null 2>&1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 120); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -50 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api

# ── 유저 2명 가입/인증/로그인 ──
reg_and_login() {
  local email=$1 nick=$2
  curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\",\"nickname\":\"$nick\"}"
  local tok=$($DB -tAc "SELECT token FROM email_verification_token WHERE user_id=(SELECT id FROM users WHERE email='$email') ORDER BY id DESC LIMIT 1;")
  curl -s -o /dev/null -X POST $BASE/auth/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$tok\"}"
  curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}

A=$(reg_and_login "a@codexray.com" "유저에이")
B=$(reg_and_login "b@codexray.com" "유저비")
echo "A token: ${A:0:20}...  B token: ${B:0:20}..."

hdrA=(-H "Authorization: Bearer $A"); hdrB=(-H "Authorization: Bearer $B")
json=(-H "Content-Type: application/json")

echo ""
echo "=== 1) A: 풀이 등록 POST /solutions (problemId=1) — 기대 201 ==="
RES=$(curl -s -w "\nHTTP %{http_code}" -X POST $BASE/solutions "${hdrA[@]}" "${json[@]}" \
  -d '{"problemId":1,"code":"print(1)","language":"python"}')
echo "$RES"
SID=$(echo "$RES" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo ">> solution id = $SID"

echo ""
echo "=== 2) A: 내 풀이 목록 GET /solutions — 기대 1건, starred=false ==="
curl -s $BASE/solutions "${hdrA[@]}" | grep -oE '"id":[0-9]+|"starred":(true|false)|"code":"[^"]*"'

echo ""
echo "=== 3) A: 같은 문제 재등록(upsert) code 변경 — 기대 여전히 1건, code=print(2) ==="
curl -s -o /dev/null -X POST $BASE/solutions "${hdrA[@]}" "${json[@]}" \
  -d '{"problemId":1,"code":"print(2)"}'
echo -n "목록 개수: "; curl -s $BASE/solutions "${hdrA[@]}" | grep -o '"code"' | wc -l
echo -n "현재 code: "; curl -s $BASE/solutions/$SID "${hdrA[@]}" | grep -o '"code":"[^"]*"'

echo ""
echo "=== 4) A: 없는 문제로 등록 — 기대 404 PROBLEM_NOT_FOUND ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/solutions "${hdrA[@]}" "${json[@]}" \
  -d '{"problemId":999999,"code":"x"}'

echo ""
echo "=== 5) A: star 토글 PATCH /solutions/$SID/star — 기대 starred=true ==="
curl -s $BASE/solutions/$SID/star -X PATCH "${hdrA[@]}" | grep -o '"starred":\(true\|false\)'

echo ""
echo "=== 6) A: starred 필터 — ?starred=true 기대 1, ?starred=false 기대 0 ==="
echo -n "starred=true 개수: ";  curl -s "$BASE/solutions?starred=true"  "${hdrA[@]}" | grep -o '"id"' | wc -l
echo -n "starred=false 개수: "; curl -s "$BASE/solutions?starred=false" "${hdrA[@]}" | grep -o '"id"' | wc -l

echo ""
echo "=== 7) A: 메모 upsert PUT /solutions/$SID/memo, 그리고 상세에 메모 포함 확인 ==="
curl -s $BASE/solutions/$SID/memo -X PUT "${hdrA[@]}" "${json[@]}" \
  -d '{"wrongReason":"인덱스 실수","logic":"BFS","keyFunctions":"deque","freeNote":"복습필요"}' \
  | grep -oE '"wrongReason":"[^"]*"|"logic":"[^"]*"'
echo -n "상세의 memo: "; curl -s $BASE/solutions/$SID "${hdrA[@]}" | grep -o '"memo":{[^}]*}'

echo ""
echo "=== 8) 소유권: B가 A의 풀이 조회 — 기대 403 / 없는 풀이 — 기대 404 ==="
echo -n "B가 A풀이 GET: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/solutions/$SID "${hdrB[@]}"
echo -n "없는 풀이 GET:  "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/solutions/999999 "${hdrA[@]}"

echo ""
echo "=== 9) 삭제: B가 삭제 시도 403 / A가 삭제 204 / 재조회 404 ==="
echo -n "B DELETE: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE $BASE/solutions/$SID "${hdrB[@]}"
echo -n "A DELETE: "; curl -s -w " HTTP %{http_code}\n" -X DELETE $BASE/solutions/$SID "${hdrA[@]}"
echo -n "재조회:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/solutions/$SID "${hdrA[@]}"

echo ""
echo "=== 10) 메모 CASCADE: 풀이 삭제 후 memos 개수 — 기대 0 ==="
$DB -tAc "SELECT 'memos='||count(*) FROM memos;"

echo ""
echo "=== 11) 인증 없이 접근 — 기대 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/solutions

echo ""
echo "=== DELETE 관련 예외 로그 ==="
grep -iE "Exception|Caused by|ERROR|constraint|delete from solutions" /tmp/boot.log | tail -20

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
