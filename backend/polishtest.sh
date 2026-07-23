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
ntypes() { curl -s $BASE/notifications -H "Authorization: Bearer $1" | grep -oE '"type":"[A-Z_]+"' | tr '\n' ' '; echo; }
uc() { curl -s $BASE/notifications/unread-count -H "Authorization: Bearer $1" | grep -o '[0-9]*'; }

A=$(reg_and_login "a@codexray.com" "유저에이")
reg_and_login "admin@codexray.com" "관리자" >/dev/null
$DB -c "UPDATE users SET role='ADMIN' WHERE email='admin@codexray.com';" >/dev/null 2>&1
ADMIN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" -d '{"email":"admin@codexray.com","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
hA=(-H "Authorization: Bearer $A"); hAd=(-H "Authorization: Bearer $ADMIN"); json=(-H "Content-Type: application/json")

# 문제 1,2 = GOLD 패밀리, 3 = DIAMOND 패밀리, 4 = 티어 없음
$DB -c "UPDATE problems SET tier='GOLD_II'     WHERE id=1;" >/dev/null 2>&1
$DB -c "UPDATE problems SET tier='GOLD_I'      WHERE id=2;" >/dev/null 2>&1
$DB -c "UPDATE problems SET tier='DIAMOND_III' WHERE id=3;" >/dev/null 2>&1
echo "준비 완료 (problems 1,2=GOLD / 3=DIAMOND / 4=티어없음)"

sol() { curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/solutions "${hA[@]}" "${json[@]}" -d "{\"problemId\":$1,\"code\":\"print($1)\"}"; }

echo ""
echo "=== 1) A가 문제1(GOLD) 풀이 → TIER_UP 알림 (미읽음 1) ==="
echo -n "HTTP $(sol 1)  A 미읽음: $(uc "$A")  타입: "; ntypes "$A"

echo "=== 2) A가 문제2(GOLD, 같은 패밀리) → 새 TIER_UP 없음 (미읽음 그대로 1) ==="
echo -n "HTTP $(sol 2)  A 미읽음: $(uc "$A")"; echo

echo "=== 3) A가 문제3(DIAMOND, 다른 패밀리) → TIER_UP (미읽음 2) ==="
echo -n "HTTP $(sol 3)  A 미읽음: $(uc "$A")  타입: "; ntypes "$A"

echo "=== 4) A가 문제4(티어 없음) → TIER_UP 없음 (미읽음 그대로 2) ==="
echo -n "HTTP $(sol 4)  A 미읽음: $(uc "$A")  payload(첫 알림 family): "
curl -s $BASE/notifications "${hA[@]}" | grep -oE '"family":"[A-Z]+"' | tr '\n' ' '; echo

echo ""
echo "=== 5) 건의사항 다이제스트: 오래된 FEEDBACK 2개 + 최근 1개 → 미처리 stale=2 ==="
curl -s -o /dev/null -X POST $BASE/community/posts "${hA[@]}" "${json[@]}" -d '{"type":"FEEDBACK","title":"오래된건의1","content":"c"}'
curl -s -o /dev/null -X POST $BASE/community/posts "${hA[@]}" "${json[@]}" -d '{"type":"FEEDBACK","title":"오래된건의2","content":"c"}'
$DB -c "UPDATE community_posts SET created_at = now() - interval '10 days' WHERE type='FEEDBACK';" >/dev/null 2>&1
curl -s -o /dev/null -X POST $BASE/community/posts "${hA[@]}" "${json[@]}" -d '{"type":"FEEDBACK","title":"최근건의","content":"c"}'
echo -n "digest 결과: "; curl -s -X POST $BASE/admin/jobs/stale-suggestion-digest "${hAd[@]}" | grep -oE '"ok":(true|false)|"count":[0-9]+'
echo -n "ADMIN 타입: "; ntypes "$ADMIN"

echo ""
echo "=== 6) 어드민 보호: 일반 유저 A가 배치 트리거 → 403 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/admin/jobs/tier-recompute "${hA[@]}"

echo ""
echo "=== 7) cleanup-tokens: 토큰 만료일 40일 전으로 조작 후 정리 → deleted>0 ==="
$DB -c "UPDATE email_verification_token SET expires_at = now() - interval '40 days';" >/dev/null 2>&1
curl -s -X POST $BASE/admin/jobs/cleanup-tokens "${hAd[@]}" | grep -oE '"ok":true|"deleted":[0-9]+'

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
