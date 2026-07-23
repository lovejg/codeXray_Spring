#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"
REDIS="docker exec codexray-redis redis-cli"

# Flyway가 V1~V4 + 시드를 새로 깔도록 스키마 초기화
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

reg_and_login() {
  local email=$1 nick=$2
  curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\",\"nickname\":\"$nick\"}"
  local tok=$($DB -tAc "SELECT token FROM email_verification_token WHERE user_id=(SELECT id FROM users WHERE email='$email') ORDER BY id DESC LIMIT 1;")
  curl -s -o /dev/null -X POST $BASE/auth/verify-email -H "Content-Type: application/json" -d "{\"token\":\"$tok\"}"
  curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"password123\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}

# 일반 유저 A
A=$(reg_and_login "a@codexray.com" "유저에이")
# 어드민: 가입/인증 후 role 승격 → 승격 "후" 로그인해야 JWT에 ROLE_ADMIN 실림
reg_and_login "admin@codexray.com" "관리자" >/dev/null
$DB -c "UPDATE users SET role='ADMIN' WHERE email='admin@codexray.com';" >/dev/null 2>&1
ADMIN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@codexray.com","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo "A: ${A:0:16}...  ADMIN: ${ADMIN:0:16}..."

hdrA=(-H "Authorization: Bearer $A"); hdrAdmin=(-H "Authorization: Bearer $ADMIN")
json=(-H "Content-Type: application/json")

echo ""
echo "=== 0) 재계산 전 problem 1 상태 (시드값: tier/adjustedLevel 보통 null) ==="
curl -s $BASE/problems/1 | grep -oE '"level":[0-9]+|"acceptanceRate":[0-9.]+|"adjustedLevel":[^,]*|"tier":[^,]*'

echo ""
echo "=== 1) A: 난이도 5(가장 어려움) 피드백 POST /ratings/feedback/1 — 기대 200 level=5 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/ratings/feedback/1 "${hdrA[@]}" "${json[@]}" -d '{"level":5}'

echo ""
echo "=== 2) problem 1 재계산 반영 — adjustedLevel 상승, tier 채워짐(DIAMOND/PLATINUM 쪽) ==="
curl -s $BASE/problems/1 | grep -oE '"adjustedLevel":[^,]*|"tier":[^,]*'

echo ""
echo "=== 3) A: 내 피드백 조회 GET /ratings/feedback/1 — 기대 level=5 ==="
curl -s $BASE/ratings/feedback/1 "${hdrA[@]}" | grep -oE '"problemId":[0-9]+|"level":[0-9]+'

echo ""
echo "=== 4) A: 같은 문제 재제출(upsert) level=0(가장 쉬움) — 기대 여전히 1행, tier 하락 ==="
curl -s -o /dev/null -X POST $BASE/ratings/feedback/1 "${hdrA[@]}" "${json[@]}" -d '{"level":0}'
echo -n "level_feedback 행 수: "; $DB -tAc "SELECT count(*) FROM level_feedback;"
echo -n "재계산 후: "; curl -s $BASE/problems/1 | grep -oE '"adjustedLevel":[^,]*|"tier":[^,]*'

echo ""
echo "=== 5) 범위 밖 level=9 — 기대 400 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ratings/feedback/1 "${hdrA[@]}" "${json[@]}" -d '{"level":9}'

echo ""
echo "=== 6) 없는 문제 999999 피드백 — 기대 404 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ratings/feedback/999999 "${hdrA[@]}" "${json[@]}" -d '{"level":3}'

echo ""
echo "=== 7) 인증 없이 피드백 — 기대 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ratings/feedback/1 "${json[@]}" -d '{"level":3}'

echo ""
echo "=== 8) recompute-all: 일반 유저 A — 기대 403 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/ratings/recompute-all "${hdrA[@]}"

echo ""
echo "=== 9) recompute-all: 어드민 — 기대 200 + count>0 ==="
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/ratings/recompute-all "${hdrAdmin[@]}"

echo ""
echo "=== 10) 피드백 없는 문제도 재계산됨 — problem 2 tier non-null 확인 ==="
curl -s $BASE/problems/2 | grep -oE '"level":[0-9]+|"adjustedLevel":[^,]*|"tier":[^,]*'

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
