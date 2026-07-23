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

A=$(reg_and_login "a@codexray.com" "유저에이")
B=$(reg_and_login "b@codexray.com" "유저비")
echo "A: ${A:0:16}...  B: ${B:0:16}..."

hdrA=(-H "Authorization: Bearer $A"); hdrB=(-H "Authorization: Bearer $B")
json=(-H "Content-Type: application/json")

echo ""
echo "=== 1) A: 노트 등록 POST /notes (CODE, tags 2개) — 기대 201 ==="
RES=$(curl -s -w "\nHTTP %{http_code}" -X POST $BASE/notes "${hdrA[@]}" "${json[@]}" \
  -d '{"type":"CODE","title":"BFS 템플릿","body":"from collections import deque","language":"python","tags":["BFS","파이썬"]}')
echo "$RES"
NID=$(echo "$RES" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo ">> note id = $NID"

echo ""
echo "=== 2) A: 두 번째 노트 (PATTERN, body에 Dijkstra) ==="
curl -s -o /dev/null -X POST $BASE/notes "${hdrA[@]}" "${json[@]}" \
  -d '{"type":"PATTERN","title":"최단경로 판단","body":"가중치 있으면 Dijkstra","tags":["그래프"]}'

echo ""
echo "=== 3) A: 목록 GET /notes — 기대 2건 ==="
echo -n "노트 개수: "; curl -s $BASE/notes "${hdrA[@]}" | grep -o '"id":[0-9]*' | wc -l

echo ""
echo "=== 4) type=CODE 필터 — 기대 1건 / type=MISTAKE — 기대 0건 ==="
echo -n "CODE: ";    curl -s "$BASE/notes?type=CODE"    "${hdrA[@]}" | grep -o '"id":[0-9]*' | wc -l
echo -n "MISTAKE: "; curl -s "$BASE/notes?type=MISTAKE" "${hdrA[@]}" | grep -o '"id":[0-9]*' | wc -l

echo ""
echo "=== 5) search=dijkstra (본문, 대소문자 무시) — 기대 1건(최단경로 판단) ==="
curl -s "$BASE/notes?search=dijkstra" "${hdrA[@]}" | grep -oE '"title":"[^"]*"'

echo ""
echo "=== 6) A: 단건 조회 GET /notes/$NID — tags 포함 확인 ==="
curl -s $BASE/notes/$NID "${hdrA[@]}" | grep -oE '"title":"[^"]*"|"tags":\[[^]]*\]'

echo ""
echo "=== 7) A: 수정 PUT /notes/$NID — title/body/tags 교체 ==="
curl -s -o /dev/null -X PUT $BASE/notes/$NID "${hdrA[@]}" "${json[@]}" \
  -d '{"type":"CODE","title":"BFS 템플릿 v2","body":"deque + visited","language":"python","tags":["BFS","큐"]}'
curl -s $BASE/notes/$NID "${hdrA[@]}" | grep -oE '"title":"[^"]*"|"tags":\[[^]]*\]'
echo -n "note_tags 행(교체 반영, 기대 2 = BFS,큐): "; $DB -tAc "SELECT count(*) FROM note_tags WHERE note_id=$NID;"

echo ""
echo "=== 8) 소유권: B가 A노트 조회 403 / 없는 노트 404 ==="
echo -n "B GET A노트: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/notes/$NID "${hdrB[@]}"
echo -n "없는 노트:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/notes/999999 "${hdrA[@]}"

echo ""
echo "=== 9) B가 A노트 수정/삭제 — 기대 403 ==="
echo -n "B PUT:    "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PUT $BASE/notes/$NID "${hdrB[@]}" "${json[@]}" -d '{"type":"CODE","title":"해킹","body":"x"}'
echo -n "B DELETE: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE $BASE/notes/$NID "${hdrB[@]}"

echo ""
echo "=== 10) 검증: 빈 title 400 / 태그 21개 400 ==="
echo -n "빈 title: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/notes "${hdrA[@]}" "${json[@]}" -d '{"type":"CODE","title":"","body":"x"}'
TAGS21=$(python3 -c "print(','.join('\"t%d\"'%i for i in range(21)))")
echo -n "태그 21개: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/notes "${hdrA[@]}" "${json[@]}" -d "{\"type\":\"CODE\",\"title\":\"t\",\"body\":\"x\",\"tags\":[$TAGS21]}"

echo ""
echo "=== 11) A 삭제 DELETE /notes/$NID — 204 → 재조회 404 → note_tags CASCADE 0 ==="
echo -n "DELETE: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE $BASE/notes/$NID "${hdrA[@]}"
echo -n "재조회: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/notes/$NID "${hdrA[@]}"
echo -n "note_tags(기대 0): "; $DB -tAc "SELECT count(*) FROM note_tags WHERE note_id=$NID;"

echo ""
echo "=== 12) 인증 없이 목록 — 기대 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/notes

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
