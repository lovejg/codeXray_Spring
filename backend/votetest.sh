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
sm() { grep -oE '"upvotes":[0-9]+|"downvotes":[0-9]+|"score":-?[0-9]+|"myVote":-?[0-9]+' | tr '\n' ' '; echo; }

A=$(reg_and_login "a@codexray.com" "유저에이"); B=$(reg_and_login "b@codexray.com" "유저비"); C=$(reg_and_login "c@codexray.com" "유저씨")
hdrA=(-H "Authorization: Bearer $A"); hdrB=(-H "Authorization: Bearer $B"); hdrC=(-H "Authorization: Bearer $C")
json=(-H "Content-Type: application/json")
echo "A/B/C 준비 완료"

newpost() { curl -s -X POST $BASE/community/posts "${hdrA[@]}" "${json[@]}" -d "$1" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'; }
PUB=$(newpost '{"type":"QUESTION","title":"추천글","content":"c"}')
PUB2=$(newpost '{"type":"QUESTION","title":"인기글","content":"c"}')
FB=$(newpost '{"type":"FEEDBACK","title":"건의글","content":"c"}')
echo "PUB=$PUB PUB2=$PUB2 FB=$FB"

echo ""
echo "=== 1) B 추천(PUB) — 기대 up1 down0 score1 myVote1 ==="
curl -s -X POST $BASE/community/posts/$PUB/vote "${hdrB[@]}" "${json[@]}" -d '{"value":1}' | sm

echo "=== 2) C 비추천(PUB) — 기대 up1 down1 score0 myVote-1 ==="
curl -s -X POST $BASE/community/posts/$PUB/vote "${hdrC[@]}" "${json[@]}" -d '{"value":-1}' | sm

echo "=== 3) B 를 비추천으로 변경(upsert) — 기대 up0 down2 score-2 myVote-1 ==="
curl -s -X POST $BASE/community/posts/$PUB/vote "${hdrB[@]}" "${json[@]}" -d '{"value":-1}' | sm

echo "=== 4) B 투표 철회 — 기대 up0 down1 score-1 myVote0 ==="
curl -s -X DELETE $BASE/community/posts/$PUB/vote "${hdrB[@]}" | sm

echo ""
echo "=== 5) 상세(PUB) as C — votes 임베드 + myVote-1 ==="
curl -s $BASE/community/posts/$PUB "${hdrC[@]}" | grep -oE '"votes":\{[^}]*\}'

echo ""
echo "=== 6) 규칙: 본인글 투표 400 / 비투표글(FEEDBACK) 400 / value=0 400 / 미인증 401 ==="
echo -n "A 본인글:     "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/vote "${hdrA[@]}" "${json[@]}" -d '{"value":1}'
echo -n "FEEDBACK글:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$FB/vote "${hdrB[@]}" "${json[@]}" -d '{"value":1}'
echo -n "value=0:      "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/vote "${hdrB[@]}" "${json[@]}" -d '{"value":0}'
echo -n "미인증:       "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/vote "${json[@]}" -d '{"value":1}'

echo ""
echo "=== 7) 정렬: PUB2 에 B,C 추천(score2) → ?sort=votes 는 인기글 먼저, 추천글 마지막 ==="
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB2/vote "${hdrB[@]}" "${json[@]}" -d '{"value":1}'
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB2/vote "${hdrC[@]}" "${json[@]}" -d '{"value":1}'
echo -n "정렬 순서: "; curl -s "$BASE/community/posts?sort=votes" | grep -o '"title":"[^"]*"' | tr '\n' ' '; echo

echo ""
echo "=== 8) 목록 myVote: C 시점 — 추천글 myVote-1, 인기글 myVote1 ==="
curl -s "$BASE/community/posts" "${hdrC[@]}" | grep -oE '"title":"[^"]*"|"myVote":-?[0-9]+' | tr '\n' ' '; echo

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
