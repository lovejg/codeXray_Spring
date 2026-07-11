#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"

# ── 개발 DB 초기화 (Flyway가 V1부터 새로 만들도록) ──
$DB -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# ── 부팅 (Flyway V1 스키마 + V2 시드 적용, 그다음 Hibernate validate) ──
./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 120); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -50 /tmp/boot.log; kill $APP_PID 2>/dev/null; exit 1; }
  sleep 1
done

echo "=== Flyway 적용 이력 ==="
$DB -tAc "SELECT version || ' | ' || description || ' | ' || success FROM flyway_schema_history ORDER BY installed_rank;"

echo ""
echo "=== 시드 개수 (기대: tags 25 / problems 689 / problem_tags 858) ==="
$DB -tAc "SELECT 'tags='||count(*) FROM algorithm_tags;"
$DB -tAc "SELECT 'problems='||count(*) FROM problems;"
$DB -tAc "SELECT 'problem_tags='||count(*) FROM problem_tags;"

BASE=http://localhost:8080/api

echo ""
echo "=== 전체 목록 total (기대: 689) ==="
curl -s "$BASE/problems" | grep -o '"total":[0-9]*'

echo ""
echo "=== source=KAKAO_BLIND total ==="
curl -s "$BASE/problems?source=KAKAO_BLIND" | grep -o '"total":[0-9]*'

echo ""
echo "=== tagId=11 (BFS) total ==="
curl -s "$BASE/problems?tagId=11" | grep -o '"total":[0-9]*'

echo ""
echo "=== 페이지네이션 size=5 (기대: total=689, totalPages=138) ==="
curl -s "$BASE/problems?size=5" | grep -oE '"(total|size|totalPages)":[0-9]*'

echo ""
echo "=== 상세 /problems/1 (title + tags) ==="
curl -s "$BASE/problems/1" | grep -oE '"title":"[^"]*"|"name":"[^"]*"'

kill $APP_PID 2>/dev/null
echo ""
echo "=== stopped ==="
