#!/usr/bin/env bash
set +e
DB="docker exec codexray-db psql -U postgres -d codexray"
REDIS="docker exec codexray-redis redis-cli"
docker start codexray-db codexray-redis >/dev/null 2>&1

# 백엔드 기동 (스키마/시드 유지 — DROP 안 함)
cd ~/spring/codeXray/backend
./gradlew bootRun --console=plain > /tmp/e2e_back.log 2>&1 &
BACK_PID=$!
for i in $(seq 1 120); do
  grep -q "Started BackendApplication" /tmp/e2e_back.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/e2e_back.log 2>/dev/null && { echo "BACKEND FAILED"; tail -30 /tmp/e2e_back.log; kill $BACK_PID; exit 1; }
  sleep 1
done
echo "backend up"

# vite dev 기동
cd ~/spring/codeXray/frontend
npm run dev > /tmp/e2e_vite.log 2>&1 &
VITE_PID=$!
for i in $(seq 1 40); do
  grep -qi "ready in" /tmp/e2e_vite.log 2>/dev/null && break
  sleep 0.5
done
sleep 1
echo "vite up"

B=http://localhost:5173/api
J='-H Content-Type:application/json'

echo ""
echo "=== 1) 프록시→백엔드 공개 문제목록 ==="
curl -s "$B/problems?size=3" | grep -oE '"total":[0-9]+' | head -1

echo "=== 2) 미인증 /users/me → 401 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$B/users/me"

echo "=== 3) 로그인 플로우: 가입→인증→로그인→me (쿠키 refresh) ==="
EMAIL="fe_$(date +%s)@codexray.com"
curl -s -o /dev/null -X POST "$B/auth/register" $J -d "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"nickname\":\"프론트유저\"}"
TOK=$($DB -tAc "SELECT token FROM email_verification_token WHERE user_id=(SELECT id FROM users WHERE email='$EMAIL') ORDER BY id DESC LIMIT 1;")
curl -s -o /dev/null -X POST "$B/auth/verify-email" $J -d "{\"token\":\"$TOK\"}"
# 로그인: refresh 쿠키를 쿠키자에 저장
ACCESS=$(curl -s -c /tmp/e2e_cookie.txt -X POST "$B/auth/login" $J -d "{\"email\":\"$EMAIL\",\"password\":\"password123\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo -n "access token: ${ACCESS:0:14}...  "
echo -n "refresh 쿠키 저장됨? "; grep -q refreshToken /tmp/e2e_cookie.txt && echo YES || echo NO
echo -n "me(with token): "; curl -s -H "Authorization: Bearer $ACCESS" "$B/users/me" | grep -oE '"nickname":"[^"]*"'

echo "=== 4) 쿠키로 refresh (body 없이) → 새 accessToken ==="
NEW=$(curl -s -b /tmp/e2e_cookie.txt -X POST "$B/auth/refresh" $J | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo -n "새 토큰 발급? "; [ -n "$NEW" ] && [ "$NEW" != "$ACCESS" ] && echo YES || echo "NO($NEW)"

# 정리
kill $VITE_PID 2>/dev/null; pkill -f vite 2>/dev/null
kill $BACK_PID 2>/dev/null
echo ""
echo "=== done (servers stopped) ==="
