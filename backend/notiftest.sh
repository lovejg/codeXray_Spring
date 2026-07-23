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
hdrA=(-H "Authorization: Bearer $A"); hdrB=(-H "Authorization: Bearer $B")
json=(-H "Content-Type: application/json")
AID=$($DB -tAc "SELECT id FROM users WHERE email='a@codexray.com';")
BID=$($DB -tAc "SELECT id FROM users WHERE email='b@codexray.com';")

# A에게 알림 5개(jsonb payload), B에게 1개 심기
$DB -c "INSERT INTO notifications (user_id, type, payload, is_read, created_at)
        SELECT $AID, 'COMMENT', jsonb_build_object('postId', g, 'postTitle', '글'||g), false, now() + (g||' seconds')::interval
        FROM generate_series(1,5) g;" >/dev/null 2>&1
$DB -c "INSERT INTO notifications (user_id, type, payload, is_read, created_at)
        VALUES ($BID, 'NEW_REPORT', '{\"reportId\":1}'::jsonb, false, now());" >/dev/null 2>&1
echo "A=$AID(알림5) B=$BID(알림1) 준비 완료"

echo ""
echo "=== 1) A 목록 — payload 가 JSON 객체로 복원되는지(JSONB 매핑) ==="
curl -s $BASE/notifications "${hdrA[@]}" | grep -oE '"payload":\{[^}]*\}' | head -2

echo ""
echo "=== 2) 커서: ?limit=2 → 2건 + nextCursor 존재 ==="
P1=$(curl -s "$BASE/notifications?limit=2" "${hdrA[@]}")
echo -n "1페이지 건수: "; echo "$P1" | grep -o '"id":[0-9]*' | wc -l
CUR=$(echo "$P1" | grep -o '"nextCursor":[0-9]*' | grep -o '[0-9]*')
echo "nextCursor = $CUR"

echo "=== 3) 다음 페이지 ?cursor=$CUR&limit=2 → 그 이전(id 더 작은) 2건 ==="
curl -s "$BASE/notifications?cursor=$CUR&limit=2" "${hdrA[@]}" | grep -oE '"id":[0-9]+|"nextCursor":[0-9]*' | tr '\n' ' '; echo

echo ""
echo "=== 4) 미읽음 카운트 — 기대 5 ==="
curl -s $BASE/notifications/unread-count "${hdrA[@]}"

echo ""
echo "=== 5) 특정 2건 읽음 처리 → 미읽음 3 ==="
IDS=$($DB -tAc "SELECT id FROM notifications WHERE user_id=$AID ORDER BY id DESC LIMIT 2;")
ID1=$(echo "$IDS" | sed -n 1p); ID2=$(echo "$IDS" | sed -n 2p)
curl -s -o /dev/null -w "read HTTP %{http_code}\n" -X PATCH $BASE/notifications/read "${hdrA[@]}" "${json[@]}" -d "{\"ids\":[$ID1,$ID2]}"
echo -n "미읽음: "; curl -s $BASE/notifications/unread-count "${hdrA[@]}"

echo ""
echo "=== 6) ?onlyUnread=true → 3건 ==="
echo -n "미읽음 목록 건수: "; curl -s "$BASE/notifications?onlyUnread=true" "${hdrA[@]}" | grep -o '"id":[0-9]*' | wc -l

echo ""
echo "=== 7) read-all → 미읽음 0 ==="
curl -s -o /dev/null -w "read-all HTTP %{http_code}\n" -X PATCH $BASE/notifications/read-all "${hdrA[@]}"
echo -n "미읽음: "; curl -s $BASE/notifications/unread-count "${hdrA[@]}"

echo ""
echo "=== 8) 삭제: A가 자기 알림 1개 삭제 204 → A 알림 4개 ==="
curl -s -o /dev/null -w "DELETE HTTP %{http_code}\n" -X DELETE $BASE/notifications/$ID1 "${hdrA[@]}"
echo -n "A 알림 수: "; $DB -tAc "SELECT count(*) FROM notifications WHERE user_id=$AID;"

echo ""
echo "=== 9) 소유권: A가 B의 알림 삭제 시도 → 204지만 실제 삭제 0 (B 알림 그대로 1) ==="
BNID=$($DB -tAc "SELECT id FROM notifications WHERE user_id=$BID LIMIT 1;")
curl -s -o /dev/null -w "DELETE HTTP %{http_code}\n" -X DELETE $BASE/notifications/$BNID "${hdrA[@]}"
echo -n "B 알림 수(기대 1): "; $DB -tAc "SELECT count(*) FROM notifications WHERE user_id=$BID;"

echo ""
echo "=== 10) 인증 없이 목록 — 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/notifications

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
