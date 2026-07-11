#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"

# ── 앱 부팅 (ddl-auto=update 로 problems/algorithm_tags/problem_tags 테이블 생성) ──
./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 90); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -40 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

# ── 샘플 데이터 삽입 (docker exec 는 -c 인자 방식으로; stdin heredoc 은 -i 필요) ──
$DB -c "TRUNCATE problem_tags, problems, algorithm_tags RESTART IDENTITY CASCADE;"
$DB -c "INSERT INTO algorithm_tags (name) VALUES ('DP'), ('BFS'), ('Greedy');"
$DB -c "INSERT INTO problems (title, source, level, tier, link, created_at, updated_at) VALUES ('두 수의 합','PRACTICE',1,'BRONZE_I','http://p/1',now(),now()), ('카카오 택배','KAKAO_BLIND',3,'GOLD_II','http://p/2',now(),now()), ('DP 배낭문제','MONTHLY_CHALLENGE',4,'PLATINUM_III','http://p/3',now(),now());"
$DB -c "INSERT INTO problem_tags (problem_id, tag_id) VALUES (1,1),(1,2),(2,3),(3,1);"

BASE=http://localhost:8080/api

echo "=== 1) 전체 목록 (기대: total=3) ==="
curl -s "$BASE/problems" | grep -o '"total":[0-9]*'

echo ""
echo "=== 2) 제목 검색 keyword=DP (기대: 'DP 배낭문제' 1건) ==="
curl -s "$BASE/problems?keyword=DP" | grep -o '"total":[0-9]*'
curl -s "$BASE/problems?keyword=DP" | grep -o '"title":"[^"]*"'

echo ""
echo "=== 3) 출처 source=KAKAO_BLIND (기대: '카카오 택배' 1건) ==="
curl -s "$BASE/problems?source=KAKAO_BLIND" | grep -o '"title":"[^"]*"'

echo ""
echo "=== 4) 태그 tagId=1(DP) (기대: 2건 — 두 수의 합, DP 배낭문제) ==="
curl -s "$BASE/problems?tagId=1" | grep -o '"total":[0-9]*'
curl -s "$BASE/problems?tagId=1" | grep -o '"title":"[^"]*"'

echo ""
echo "=== 5) 티어 범위 tierMin=6&tierMax=8 (GOLD계열, 기대: '카카오 택배' 1건) ==="
curl -s "$BASE/problems?tierMin=6&tierMax=8" | grep -o '"title":"[^"]*"'

echo ""
echo "=== 6) 페이지네이션 size=2&page=0 (기대: items 2건, total=3, totalPages=2) ==="
curl -s "$BASE/problems?size=2&page=0" | grep -oE '"(total|size|totalPages)":[0-9]*'

echo ""
echo "=== 7) 정렬 sort=level,desc (기대: 첫 항목 level=4) ==="
curl -s "$BASE/problems?sort=level,desc&size=1" | grep -o '"level":[0-9]*' | head -1

echo ""
echo "=== 8) 상세 GET /problems/1 (기대: '두 수의 합' + tags DP,BFS) ==="
curl -s "$BASE/problems/1" | grep -oE '"title":"[^"]*"|"name":"[^"]*"'

echo ""
echo "=== 9) 없는 id GET /problems/999999 (기대: 404) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/problems/999999"

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
