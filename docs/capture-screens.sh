#!/bin/bash
# README·포트폴리오에 쓰는 서비스 화면을 시연 데이터로 다시 찍는다.  bash docs/capture-screens.sh
#
# - 측정 환경(perf/compose.yml)의 Postgres·Redis 를 쓴다. 개발 DB(codexray-db)와 운영 DB 는 건드리지 않는다
# - 시연 데이터(deploy/demo-seed.sql)를 넣고, 그 계정(demo2, DP장인)으로 로그인해 찍는다.
#   비밀번호는 실행할 때마다 새로 만들고 어디에도 남기지 않는다
# - 백엔드는 지금 작업 트리로 bootJar 를 만들어 perf 프로필로 띄운다(Kafka 없음 → AI 요청 화면은 찍지 않는다)
# - 화면은 vite build 결과를 vite preview 로 띄운다(/api 는 vite.config.ts 의 프록시로 8080 에)
# 결과: docs/screenshots/*.png (1440×900, 2배율)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
OUT=$ROOT/docs/screenshots
PW_IMAGE=mcr.microsoft.com/playwright:v1.49.1-noble
DIST=$(mktemp -d)
LOG=$(mktemp)
log() { echo "[$(date +%T)] $*"; }

PIDS=()
cleanup() {
  for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null || true; done
  rm -rf "$DIST"
}
trap cleanup EXIT

psql() { docker compose -f perf/compose.yml exec -T postgres psql -U perf -d codexray "$@"; }

log "측정용 Postgres·Redis 기동"
docker compose -f perf/compose.yml up -d --wait postgres redis > /dev/null

log "백엔드 빌드"
(cd backend && ./gradlew -q bootJar -x test)
JAR=$(ls backend/build/libs/*.jar | grep -v plain | head -1)

export DB_URL=jdbc:postgresql://localhost:55432/codexray DB_USERNAME=perf DB_PASSWORD=perf
export REDIS_HOST=localhost REDIS_PORT=56379 KAFKA_BOOTSTRAP_SERVERS=localhost:1
export JWT_SECRET=screens-only-secret-screens-only-0123456789 JWT_ACCESS_EXPIRATION_MS=1800000 REFRESH_EXPIRATION_DAYS=14
export COOKIE_SECURE=false MAIL_MODE=console MAIL_FROM=screens@codexray.local
export APP_BASE_URL=http://localhost:4173 ADMIN_EMAIL=screens@codexray.local PERF_STATE=screens
# vite 프록시는 브라우저의 Origin(localhost:4173)을 그대로 넘겨서, 백엔드(8080) 입장에서는 교차 오리진이다
export APP_CORS_ORIGINS=http://localhost:4173

start_backend() {
  java -jar "$ROOT/$JAR" --spring.profiles.active=perf >> "$LOG" 2>&1 &
  BACKEND_PID=$!
  PIDS+=("$BACKEND_PID")
  for _ in $(seq 90); do
    curl -sf localhost:8080/actuator/health > /dev/null && return 0
    kill -0 "$BACKEND_PID" 2>/dev/null || { tail -40 "$LOG"; exit 1; }
    sleep 1
  done
  echo "백엔드가 90초 안에 뜨지 않았습니다. 로그: $LOG"; exit 1
}

# 처음 띄울 때 Flyway 가 표와 문제 시드를 만든다. 그다음 시연 데이터를 넣고, 다시 띄우면 TierBootstrap 이 티어를 채운다
log "백엔드 1차 기동(스키마 준비)"
start_backend
kill "$BACKEND_PID"; wait "$BACKEND_PID" 2>/dev/null || true

log "시연 데이터 넣기"
DEMO_PASSWORD=$(head -c 18 /dev/urandom | base64 | tr -d '/+=')
HASH=$(printf '%s' "$DEMO_PASSWORD" | docker run --rm -i httpd:2.4-alpine htpasswd -niBC 10 x | cut -d: -f2 | sed 's/^\$2y\$/$2a$/')
psql -q -v pw_hash="'$HASH'" < deploy/demo-seed.sql
docker compose -f perf/compose.yml exec -T redis redis-cli FLUSHALL > /dev/null

log "백엔드 2차 기동(티어 재계산)"
start_backend

ids=$(psql -At <<'SQL'
select
  (select s.id from solutions s join users u on u.id = s.user_id where u.email = 'demo2@codexray-demo.example' and s.problem_id = 560),
  (select n.id from notes n join users u on u.id = n.user_id where u.email = 'demo2@codexray-demo.example' and n.type = 'CODE'),
  (select id from community_posts where title like 'BFS에서 방문 체크%'),
  (select id from community_posts where title like '"N으로 표현" 티어%');
SQL
)
IFS='|' read -r SOL_ID NOTE_ID POST_ID SUGG_ID <<< "$ids"

log "화면 빌드·기동"
(cd frontend && npx vite build --outDir "$DIST" --emptyOutDir > /dev/null)
(cd frontend && exec npx vite preview --outDir "$DIST" --port 4173 --strictPort > /dev/null 2>&1) &
PIDS+=($!)
for _ in $(seq 30); do curl -sf localhost:4173 > /dev/null && break; sleep 1; done

log "캡처"
rm -rf "$OUT"; mkdir -p "$OUT"
docker run --rm --network host --ipc=host --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -e DEMO_EMAIL=demo2@codexray-demo.example -e DEMO_PASSWORD="$DEMO_PASSWORD" \
  -e PROBLEM_ID=561 -e SOL_ID="$SOL_ID" -e NOTE_ID="$NOTE_ID" -e POST_ID="$POST_ID" -e SUGG_ID="$SUGG_ID" \
  -v "$ROOT/perf/browser:/work" -v "$OUT:/out" -w /work "$PW_IMAGE" \
  node shoot-app.mjs http://localhost:4173 /out

log "끝: $OUT"
