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
count_posts() { grep -o '"type":"' | wc -l; }  # 게시글 개수(author/problem엔 type 없음)

A=$(reg_and_login "a@codexray.com" "유저에이")
B=$(reg_and_login "b@codexray.com" "유저비")
reg_and_login "admin@codexray.com" "관리자" >/dev/null
$DB -c "UPDATE users SET role='ADMIN' WHERE email='admin@codexray.com';" >/dev/null 2>&1
ADMIN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@codexray.com","password":"password123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo "A/B/ADMIN 준비 완료"

hdrA=(-H "Authorization: Bearer $A"); hdrB=(-H "Authorization: Bearer $B"); hdrAdmin=(-H "Authorization: Bearer $ADMIN")
json=(-H "Content-Type: application/json")

echo ""
echo "=== 1) A: 공개 QUESTION(problemId=1) 등록 — 기대 201 + author/problem 포함 ==="
RES=$(curl -s -w "\nHTTP %{http_code}" -X POST $BASE/community/posts "${hdrA[@]}" "${json[@]}" \
  -d '{"type":"QUESTION","title":"공개질문","content":"BFS 어떻게 접근?","problemId":1}')
echo "$RES" | grep -oE '"author":\{[^}]*\}|"problem":\{[^}]*\}|HTTP [0-9]+'
PUB=$(echo "$RES" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo ">> 공개글 id = $PUB"

echo ""
echo "=== 2) A: 비공개글 등록 — 기대 201 ==="
RES=$(curl -s -X POST $BASE/community/posts "${hdrA[@]}" "${json[@]}" \
  -d '{"type":"QUESTION","title":"비밀글","content":"나만 볼래","isPrivate":true}')
PRIV=$(echo "$RES" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo ">> 비공개글 id = $PRIV"

echo ""
echo "=== 3) 비로그인 목록 — 기대 공개글 1건, '비밀글' 없음 ==="
BODY=$(curl -s $BASE/community/posts)
echo -n "게시글 수: "; echo "$BODY" | count_posts
echo -n "비밀글 노출?: "; echo "$BODY" | grep -q "비밀글" && echo "노출됨(버그)" || echo "숨겨짐(정상)"

echo ""
echo "=== 4) A 로그인 목록 — 기대 2건(비공개 포함) ==="
echo -n "게시글 수: "; curl -s $BASE/community/posts "${hdrA[@]}" | count_posts

echo ""
echo "=== 5) B 로그인 목록 — 기대 1건(A 비공개 제외) ==="
echo -n "게시글 수: "; curl -s $BASE/community/posts "${hdrB[@]}" | count_posts

echo ""
echo "=== 6) 비공개글 상세: 비로그인 403 / A 200 / B 403 ==="
echo -n "anon: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PRIV
echo -n "A:    "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PRIV "${hdrA[@]}"
echo -n "B:    "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PRIV "${hdrB[@]}"

echo ""
echo "=== 7) A/B 가 공개글에 댓글 — 기대 각 201, 상세에 2개, 목록 commentCount=2 ==="
curl -s -o /dev/null -X POST $BASE/community/posts/$PUB/comments "${hdrA[@]}" "${json[@]}" -d '{"content":"A의 댓글"}'
curl -s -o /dev/null -w "댓글 등록 HTTP %{http_code}\n" -X POST $BASE/community/posts/$PUB/comments "${hdrB[@]}" "${json[@]}" -d '{"content":"B의 댓글"}'
echo -n "상세 댓글 수: "; curl -s $BASE/community/posts/$PUB | grep -o '"content":"[^"]*"' | wc -l
echo -n "목록 commentCount: "; curl -s $BASE/community/posts | grep -o '"commentCount":[0-9]*'

echo ""
echo "=== 8) 비공개글에 B 댓글 — 기대 403 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts/$PRIV/comments "${hdrB[@]}" "${json[@]}" -d '{"content":"엿보기"}'

echo ""
echo "=== 9) 수정: B(비작성자) 403 / A 200 title 변경 ==="
echo -n "B PUT: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X PUT $BASE/community/posts/$PUB "${hdrB[@]}" "${json[@]}" -d '{"title":"탈취","content":"x"}'
echo -n "A PUT: "; curl -s -X PUT $BASE/community/posts/$PUB "${hdrA[@]}" "${json[@]}" -d '{"title":"공개질문(수정)","content":"BFS vs DFS"}' | grep -oE '"title":"[^"]*"'

echo ""
echo "=== 10) 익명화: DB에서 유저 A 삭제 → 공개글은 보존, 작성자 '탈퇴한 사용자' ==="
$DB -c "DELETE FROM users WHERE email='a@codexray.com';" >/dev/null 2>&1
echo -n "글 존재?: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PUB
curl -s $BASE/community/posts/$PUB | grep -oE '"author":\{[^}]*\}'
echo -n "A 댓글 익명화 포함?: "; curl -s $BASE/community/posts/$PUB | grep -q "탈퇴한 사용자" && echo "OK" || echo "실패"

echo ""
echo "=== 11) 숨김(hidden) 가시성: DB로 hidden=true → 비로그인 상세 404, 목록서 제외 ==="
$DB -c "UPDATE community_posts SET hidden=true WHERE id=$PUB;" >/dev/null 2>&1
echo -n "anon 상세: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PUB
echo -n "anon 목록 수(기대 0): "; curl -s $BASE/community/posts | count_posts

echo ""
echo "=== 12) 관리자 삭제: A가 남긴 공개글을 ADMIN 이 삭제 — 기대 204 → 재조회 404 ==="
echo -n "ADMIN DELETE: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE $BASE/community/posts/$PUB "${hdrAdmin[@]}"
echo -n "재조회: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/$PUB "${hdrAdmin[@]}"

echo ""
echo "=== 13) 인증 없이 글쓰기 401 / 없는 글 상세 404 ==="
echo -n "unauth POST: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/community/posts "${json[@]}" -d '{"type":"QUESTION","title":"t","content":"c"}'
echo -n "없는 글:     "; curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/community/posts/999999

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
