#!/usr/bin/env bash
# 문제 목록 API 세 단계 성능 측정. 같은 조건으로 연달아 재고 원본 결과를 perf/results/<시각>/ 에 남긴다.
#
#   1-before : 실제 개선 전(①′). AlgorithmTag 의 @BatchSize 없음 + 목록 캐시 없음  → perf/states/1-before.patch
#   2-batch  : @BatchSize 만(②). 목록 캐시 없음                                     → perf/states/2-batch.patch
#   3-cache  : 지금 코드 그대로(③). @BatchSize + Redis 목록 캐시
#
# 단계마다 현재 커밋(HEAD)을 임시 worktree 로 꺼내 패치를 적용하고 jar 를 새로 만든다. 저장소의 작업 폴더는 건드리지 않는다.
# 운영 코드에 측정용 스위치를 넣지 않으려고 이렇게 한다.
#
# 사용:  perf/run-backend.sh            (전부, 약 12분)
#        STATES="3-cache" perf/run-backend.sh
# 준비:  docker compose -f perf/compose.yml up -d   (스크립트가 알아서 띄운다)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STATES=${STATES:-"1-before 2-batch 3-cache"}
VUS=${VUS:-50}
DURATION=${DURATION:-30s}   # 본 측정 1회 길이
WARMUP=${WARMUP:-30s}       # 워밍업(결과 버림)
RUNS=${RUNS:-2}             # 본 측정 횟수
SETTLE=${SETTLE:-30}        # 앱이 뜬 뒤, 단계 사이 쉬는 시간(초)
K6_IMAGE=${K6_IMAGE:-grafana/k6:2.2.0}
STAMP=$(date +%Y%m%d-%H%M)
OUT="$ROOT/perf/results/$STAMP"
GRAFANA=http://localhost:3300
PROM=http://localhost:9390
mkdir -p "$OUT"

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$OUT/run.log"; }
now_ms() { date +%s%3N; }

# Grafana 그래프에 구간 표시(단계 이름)를 남긴다. 실패해도 측정은 계속한다
annotate() { # $1=시작ms $2=끝ms $3=글
  curl -s -X POST "$GRAFANA/api/annotations" -H 'Content-Type: application/json' \
    -d "{\"time\":$1,\"timeEnd\":$2,\"tags\":[\"perf\"],\"text\":\"$3\"}" > /dev/null || true
}

# ── 0. 측정 환경 기록 ──
{
  echo "date: $(date -Iseconds)"
  echo "commit: $(git -C "$ROOT" rev-parse --short HEAD) ($(git -C "$ROOT" log -1 --format=%s))"
  echo "cpu: $(lscpu | sed -n 's/^Model name: *//p') / $(nproc) threads"
  echo "mem: $(free -g | awk '/Mem:/{print $2}') GiB"
  echo "os: $(. /etc/os-release; echo "$PRETTY_NAME") / kernel $(uname -r)"
  echo "java: $(java -version 2>&1 | head -1)"
  echo "k6: $K6_IMAGE"
  echo "load: vus=$VUS duration=$DURATION warmup=$WARMUP runs=$RUNS settle=${SETTLE}s think-time=0"
  echo "target: GET /api/problems?page=0&size=50&sort=createdAt,desc"
  echo "other containers running:"; docker ps --format '  {{.Names}} ({{.Image}})'
} > "$OUT/env.txt"

# ── 1. 측정용 DB·Redis·Prometheus·Grafana ──
log "perf 컨테이너 기동"
docker compose -f "$ROOT/perf/compose.yml" up -d --wait postgres redis > /dev/null
docker compose -f "$ROOT/perf/compose.yml" up -d prometheus grafana > /dev/null
docker pull -q "$K6_IMAGE" > /dev/null

# 측정 중 Gradle 데몬(약 1GB JVM)이 CPU 를 나눠 쓰지 않게 끈다(예전 측정에서 ③ 이 20% 낮게 나온 원인)
(cd "$ROOT/backend" && ./gradlew --stop -q) || true

# 앱 환경 변수. 운영 비밀값은 쓰지 않는다(.env 가 없는 임시 폴더에서 실행)
export DB_URL=jdbc:postgresql://localhost:55432/codexray DB_USERNAME=perf DB_PASSWORD=perf
export REDIS_HOST=localhost REDIS_PORT=56379 KAFKA_BOOTSTRAP_SERVERS=localhost:1
export JWT_SECRET=perf-only-secret-perf-only-secret-0123456789 JWT_ACCESS_EXPIRATION_MS=1800000 REFRESH_EXPIRATION_DAYS=14
export COOKIE_SECURE=false MAIL_MODE=console MAIL_FROM=perf@codexray.local
export APP_BASE_URL=http://localhost:5173 ADMIN_EMAIL=perf@codexray.local

FIRST_MS=$(now_ms)
APP_PID=""
cleanup() { [ -n "$APP_PID" ] && kill "$APP_PID" 2>/dev/null || true; }
trap cleanup EXIT

for STATE in $STATES; do
  WT=/tmp/codexray-perf-$STATE
  log "== $STATE: jar 빌드 =="
  git -C "$ROOT" worktree remove --force "$WT" 2>/dev/null || rm -rf "$WT"
  git -C "$ROOT" worktree add -q --detach "$WT" HEAD
  if [ -f "$ROOT/perf/states/$STATE.patch" ]; then
    git -C "$WT" apply "$ROOT/perf/states/$STATE.patch"
    git -C "$WT" diff --stat | tee -a "$OUT/run.log"
  fi
  (cd "$WT/backend" && ./gradlew --no-daemon -q bootJar -x test) >> "$OUT/run.log" 2>&1
  JAR=$(ls "$WT"/backend/build/libs/*.jar | grep -v plain | head -1)

  # 단계마다 캐시를 비우고 JVM 을 새로 띄운다
  docker compose -f "$ROOT/perf/compose.yml" exec -T redis redis-cli FLUSHALL > /dev/null
  log "앱 기동 (PERF_STATE=$STATE)"
  (cd /tmp && PERF_STATE=$STATE exec java -jar "$JAR" --spring.profiles.active=perf) > "$OUT/$STATE-app.log" 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 120); do curl -sf localhost:8080/actuator/health > /dev/null && break; sleep 1; done
  curl -sf localhost:8080/actuator/health > /dev/null || { log "앱이 뜨지 않음. $OUT/$STATE-app.log 확인"; exit 1; }
  sleep "$SETTLE"

  k6run() { # $1=run 태그 $2=길이 $3=요약 파일(없으면 빈 값)
    # --user: k6 이미지의 기본 사용자로는 결과 폴더(내 사용자 소유)에 요약 파일을 쓸 수 없다
    docker run --rm --network host --user "$(id -u):$(id -g)" -v "$ROOT/perf:/scripts:ro" -v "$OUT:/out" \
      -e BASE_URL=http://localhost:8080 -e VUS="$VUS" -e DURATION="$2" \
      ${3:+-e SUMMARY_FILE=/out/$3} \
      -e K6_PROMETHEUS_RW_SERVER_URL=$PROM/api/v1/write \
      -e K6_PROMETHEUS_RW_TREND_STATS='p(95),avg' -e K6_PROMETHEUS_RW_PUSH_INTERVAL=5s \
      "$K6_IMAGE" run -q -o experimental-prometheus-rw --tag state="$STATE" --tag run="$1" /scripts/problems-load.js
  }

  START_MS=$(now_ms)
  log "워밍업 $WARMUP (결과 버림)"
  k6run warmup "$WARMUP" "" | tee -a "$OUT/run.log"
  for i in $(seq 1 "$RUNS"); do
    log "본 측정 run$i $DURATION"
    k6run "run$i" "$DURATION" "$STATE-run$i.json" | tee -a "$OUT/run.log"
  done
  END_MS=$(now_ms)
  annotate "$START_MS" "$END_MS" "$STATE"

  # 측정 뒤 캐시 키 수: 1·2 단계는 0, 3 단계는 1 이어야 캐시가 의도대로 꺼졌다/켜졌다는 뜻이다
  DBSIZE=$(docker compose -f "$ROOT/perf/compose.yml" exec -T redis redis-cli DBSIZE | tr -d '\r')
  # 요청 1회당 실행된 SQL 수(앱 지표로 직접 계산)
  METRICS=$(curl -s localhost:8080/actuator/prometheus)
  STMTS=$(echo "$METRICS" | awk -F' ' '/^hibernate_statements_total.*status="prepared"/{print $2}')
  REQS=$(echo "$METRICS" | awk -F' ' '/^http_server_requests_seconds_count.*uri="\/api\/problems"/{s+=$2} END{print s}')
  echo "{\"state\":\"$STATE\",\"redisDbsize\":$DBSIZE,\"preparedStatements\":${STMTS:-0},\"problemRequests\":${REQS:-0},\"startMs\":$START_MS,\"endMs\":$END_MS}" > "$OUT/$STATE-server.json"
  log "$STATE 끝: DBSIZE=$DBSIZE, SQL=$STMTS / 요청=$REQS"

  kill "$APP_PID"; wait "$APP_PID" 2>/dev/null || true; APP_PID=""
  git -C "$ROOT" worktree remove --force "$WT"
  sleep 10
done

LAST_MS=$(now_ms)
python3 "$ROOT/perf/summarize.py" "$OUT"
log "끝. 요약: $OUT/summary.md"
log "Grafana: $GRAFANA/d/codexray-perf?from=$((FIRST_MS - 30000))&to=$((LAST_MS + 30000))"
echo "$GRAFANA/d/codexray-perf?from=$((FIRST_MS - 30000))&to=$((LAST_MS + 30000))" > "$OUT/grafana-url.txt"
