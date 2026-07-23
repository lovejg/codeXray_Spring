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
uc() { curl -s $BASE/notifications/unread-count -H "Authorization: Bearer $1" | grep -o '"count":[0-9]*' | grep -o '[0-9]*'; }
ntypes() { curl -s $BASE/notifications -H "Authorization: Bearer $1" | grep -oE '"type":"[A-Z_]+"' | tr '\n' ' '; echo; }

A=$(reg_and_login "a@codexray.com" "유저에이"); B=$(reg_and_login "b@codexray.com" "유저비")
reg_and_login "admin@codexray.com" "관리자" >/dev/null
$DB -c "UPDATE users SET role='ADMIN' WHERE email='admin@codexray.com';" >/dev/null 2>&1
ADMIN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" -d '{"email":"admin@codexray.com","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
hA=(-H "Authorization: Bearer $A"); hB=(-H "Authorization: Bearer $B"); hAd=(-H "Authorization: Bearer $ADMIN")
json=(-H "Content-Type: application/json")

newpost() { curl -s -X POST $BASE/community/posts "${hA[@]}" "${json[@]}" -d "$1" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'; }
PUB=$(newpost '{"type":"QUESTION","title":"질문글","content":"c"}')
PUB2=$(newpost '{"type":"QUESTION","title":"질문글2","content":"c"}')
SUG=$(newpost '{"type":"FEEDBACK","title":"건의글","content":"레벨 이상해요"}')
echo "PUB=$PUB PUB2=$PUB2 SUG=$SUG (A=글쓴이)"

echo ""
echo "=== 1) 댓글 알림: B가 PUB에 댓글 → A에게 COMMENT 알림 (A 미읽음 1) ==="
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB/comments "${hB[@]}" "${json[@]}" -d '{"content":"저도 궁금"}'
echo -n "A 미읽음: $(uc "$A")  A 타입: "; ntypes "$A"

echo "=== 2) 자기 글 자기 댓글 → 자기 알림 없음 (A 미읽음 그대로 1) ==="
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB/comments "${hA[@]}" "${json[@]}" -d '{"content":"제가 씁니다"}'
echo -n "A 미읽음: $(uc "$A")"; echo

echo ""
echo "=== 3) 신고: B가 PUB 신고 → 201, 관리자에게 NEW_REPORT (ADMIN 미읽음 1) ==="
curl -s -o /dev/null -w "report HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/report "${hB[@]}" "${json[@]}" -d '{"reason":"스팸입니다"}'
echo -n "ADMIN 미읽음: $(uc "$ADMIN")  타입: "; ntypes "$ADMIN"

echo "=== 4) 신고 규칙: 본인글 신고 400 / 중복 신고 400 ==="
echo -n "A 본인글 신고: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/report "${hA[@]}" "${json[@]}" -d '{"reason":"자기신고"}'
echo -n "B 중복 신고:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/report "${hB[@]}" "${json[@]}" -d '{"reason":"또신고"}'

echo ""
echo "=== 5) 관리자 신고목록: ADMIN 1건(OPEN) / 일반유저 B 403 ==="
echo -n "ADMIN 목록 상태: "; curl -s "$BASE/community/admin/reports" "${hAd[@]}" | grep -oE '"status":"[A-Z]+"|"reason":"[^"]*"' | tr '\n' ' '; echo
echo -n "B 접근: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/community/admin/reports" "${hB[@]}"

echo ""
echo "=== 6) 숨김: ADMIN이 PUB 숨김 → OPEN신고 자동 HANDLED + A에 POST_HIDDEN + B에 REPORT_RESOLVED ==="
curl -s -o /dev/null -w "hide HTTP %{http_code}\n" -X PATCH $BASE/community/admin/posts/$PUB/hide "${hAd[@]}" "${json[@]}" -d '{"hidden":true}'
echo -n "OPEN 신고 수(기대0): "; curl -s "$BASE/community/admin/reports?status=OPEN" "${hAd[@]}" | grep -o '"id":[0-9]*' | wc -l
echo -n "HANDLED 신고 수(기대1): "; curl -s "$BASE/community/admin/reports?status=HANDLED" "${hAd[@]}" | grep -o '"id":[0-9]*' | wc -l
echo -n "A 타입: "; ntypes "$A"
echo -n "B 타입: "; ntypes "$B"

echo ""
echo "=== 7) 수동 신고처리: B가 PUB2 신고 → ADMIN DISMISSED → B에 REPORT_RESOLVED ==="
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB2/report "${hB[@]}" "${json[@]}" -d '{"reason":"광고글"}'
RID=$($DB -tAc "SELECT id FROM post_reports WHERE post_id=$PUB2 LIMIT 1;")
curl -s -o /dev/null -w "updateReport HTTP %{http_code}\n" -X PATCH $BASE/community/admin/reports/$RID "${hAd[@]}" "${json[@]}" -d '{"status":"DISMISSED","adminNote":"근거 없음"}'
echo -n "B 미읽음: $(uc "$B")  타입: "; ntypes "$B"

echo ""
echo "=== 8) 건의사항 상태/답변: ADMIN → A에 STATUS_CHANGE, ADMIN_REPLY ==="
curl -s -o /dev/null -w "status HTTP %{http_code}\n" -X PATCH $BASE/community/posts/$SUG/status "${hAd[@]}" "${json[@]}" -d '{"status":"IN_PROGRESS"}'
echo -n "답변 후 adminReply: "; curl -s -X PATCH $BASE/community/posts/$SUG/admin-reply "${hAd[@]}" "${json[@]}" -d '{"adminReply":"확인했습니다"}' | grep -oE '"adminReply":"[^"]*"'
echo -n "A 타입: "; ntypes "$A"

echo ""
echo "=== 9) 비건의글에 상태/답변 400 (PUB2=QUESTION) ==="
echo -n "status:      "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PATCH $BASE/community/posts/$PUB2/status "${hAd[@]}" "${json[@]}" -d '{"status":"RESOLVED"}'
echo -n "admin-reply: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PATCH $BASE/community/posts/$PUB2/admin-reply "${hAd[@]}" "${json[@]}" -d '{"adminReply":"x"}'

echo ""
echo "=== 10) 일반유저 관리자 액션 차단: B status/hide 403 ==="
echo -n "B status: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PATCH $BASE/community/posts/$SUG/status "${hB[@]}" "${json[@]}" -d '{"status":"RESOLVED"}'
echo -n "B hide:   "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PATCH $BASE/community/admin/posts/$SUG/hide "${hB[@]}" "${json[@]}" -d '{"hidden":true}'

echo ""
echo "=== 11) 최종 미읽음: A=4(COMMENT,POST_HIDDEN,STATUS_CHANGE,ADMIN_REPLY) B=2(REPORT_RESOLVED×2) ADMIN=1(NEW_REPORT) ==="
echo "A=$(uc "$A")  B=$(uc "$B")  ADMIN=$(uc "$ADMIN")"

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
