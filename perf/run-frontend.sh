#!/usr/bin/env bash
# 화면 첫 로딩(FCP/LCP) 측정. 같은 조건으로 다섯 경우를 재고 원본을 perf/results/<시각>-fcp/ 에 남긴다.
#
#   1-before        b58e5be(2026-10-30 고치기 전)의 화면 그대로: gzip 없음, 모든 화면이 JS 한 파일
#   2-gzip          같은 화면 코드 + 지금의 nginx 설정(gzip)
#   3-split         지금 코드: gzip + React.lazy 화면 나누기
#   4-proxy-nogzip  지금 코드에서 gzip_http_version 1.0 만 뺀 것을, 운영처럼 HTTP/1.0 으로 넘기는 nginx 뒤에 둔 것
#                   (2026-10-30 배포 직후 운영에서 압축이 빠졌던 상황의 재현)
#   5-proxy         지금 코드를 같은 HTTP/1.0 nginx 뒤에 둔 것(고친 뒤의 운영 경로)
#
# 백엔드 없이 화면 이미지만 띄운다(API 요청은 502 로 바로 끝난다. 첫 화면 그리기에는 쓰이지 않는다).
# Playwright 공식 이미지 안에서 Chromium 으로 잰다(perf/browser/measure-fcp.mjs).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BEFORE_COMMIT=${BEFORE_COMMIT:-b58e5be}
PW_IMAGE=mcr.microsoft.com/playwright:v1.49.1-noble
NET=codexray-fcp
STAMP=$(date +%Y%m%d-%H%M)
OUT="$ROOT/perf/results/$STAMP-fcp"
mkdir -p "$OUT"
log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$OUT/run.log"; }

{
  echo "date: $(date -Iseconds)"
  echo "commit: $(git -C "$ROOT" rev-parse --short HEAD), before: $BEFORE_COMMIT"
  echo "cpu: $(lscpu | sed -n 's/^Model name: *//p') / $(nproc) threads"
  echo "browser: $PW_IMAGE"
} > "$OUT/env.txt"

docker network create "$NET" > /dev/null 2>&1 || true
cleanup() {
  docker rm -f fcp-1-before fcp-2-gzip fcp-3-split fcp-4-nogzip-origin fcp-4-proxy-nogzip fcp-5-proxy > /dev/null 2>&1 || true
  for d in before gzip split nogzip; do git -C "$ROOT" worktree remove --force "/tmp/codexray-fcp-$d" 2>/dev/null || true; done
}
trap cleanup EXIT
cleanup

wt() { # $1=이름 $2=커밋
  git -C "$ROOT" worktree add -q --detach "/tmp/codexray-fcp-$1" "$2"
  echo "/tmp/codexray-fcp-$1/frontend"
}

log "화면 이미지 빌드(네 개)"
D_BEFORE=$(wt before "$BEFORE_COMMIT")
D_GZIP=$(wt gzip "$BEFORE_COMMIT"); cp "$ROOT/frontend/nginx.conf" "$D_GZIP/nginx.conf"
D_SPLIT=$(wt split HEAD)
D_NOGZIP=$(wt nogzip HEAD); sed -i '/^\s*gzip_http_version 1.0;/d' "$D_NOGZIP/nginx.conf"
for p in "before:$D_BEFORE" "gzip:$D_GZIP" "split:$D_SPLIT" "nogzip:$D_NOGZIP"; do
  docker build -q -t "codexray-fcp:${p%%:*}" "${p#*:}" >> "$OUT/run.log"
done

log "컨테이너 기동"
run() { docker run -d --rm --name "$1" --network "$NET" "${@:2}" > /dev/null; }
run fcp-1-before -p 5181:80 codexray-fcp:before
run fcp-2-gzip -p 5182:80 codexray-fcp:gzip
run fcp-3-split -p 5183:80 codexray-fcp:split
run fcp-4-nogzip-origin codexray-fcp:nogzip
# 운영의 바깥 Nginx 와 같은 기본값(proxy_http_version 1.0)으로 넘기는 프록시
proxy_conf() { printf 'server { listen 80; location / { proxy_pass http://%s; proxy_set_header Host $host; } }\n' "$1"; }
proxy_conf fcp-4-nogzip-origin > "$OUT/proxy-nogzip.conf"
proxy_conf fcp-3-split > "$OUT/proxy.conf"
run fcp-4-proxy-nogzip -p 5184:80 -v "$OUT/proxy-nogzip.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine
run fcp-5-proxy -p 5185:80 -v "$OUT/proxy.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine
sleep 3

# 응답 헤더 기록: 메인 JS 가 압축돼 오는지(Content-Encoding)
{
  for port in 5181 5182 5183 5184 5185; do
    js=$(curl -s "localhost:$port/" | grep -o '/assets/index-[^"]*\.js' | head -1)
    enc=$(curl -s -o /dev/null -D - -H 'Accept-Encoding: gzip' "localhost:$port$js" | tr -d '\r' | grep -i '^content-encoding' || echo 'content-encoding: (없음)')
    size=$(curl -s -H 'Accept-Encoding: gzip' "localhost:$port$js" | wc -c)
    echo "port $port  $js  $enc  전송 ${size} bytes"
  done
} | tee "$OUT/headers.txt"

log "FCP 측정(경우마다 5회 중앙값)"
(cd "$ROOT/perf/browser" && [ -d node_modules ] || PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm ci --no-audit --no-fund > /dev/null 2>&1 || PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install --no-audit --no-fund > /dev/null)
docker run --rm --network host --ipc=host -v "$ROOT/perf/browser:/work" -v "$OUT:/out" -w /work "$PW_IMAGE" \
  node measure-fcp.mjs /out/fcp.json \
  1-before=http://localhost:5181 2-gzip=http://localhost:5182 3-split=http://localhost:5183 \
  4-proxy-nogzip=http://localhost:5184 5-proxy=http://localhost:5185 | tee -a "$OUT/run.log"

python3 - "$OUT" <<'PY'
import json, sys
from pathlib import Path
out = Path(sys.argv[1])
rows = json.loads((out / "fcp.json").read_text())["results"]
label = {"1-before": "① 고치기 전", "2-gzip": "② gzip", "3-split": "③ gzip + 화면 나누기",
         "4-proxy-nogzip": "운영 경로 재현: 고치기 전(HTTP/1.0 프록시, 압축 빠짐)", "5-proxy": "운영 경로 재현: 고친 뒤"}
lines = ["# 화면 첫 로딩 측정 결과", "", "perf/run-frontend.sh 가 원본(fcp.json)에서 만든 표. 조건: 느린 4G(150ms·1.6Mbps/750kbps), CPU 4배 감속, 캐시 없음, 5회 중앙값.", "",
         "| 경우 | 화면 | 받은 JS | JS 압축 | FCP | LCP |", "|---|---|---|---|---|---|"]
for r in rows:
    lines.append(f"| {label.get(r['target'], r['target'])} | `{r['path']}` | {r['jsKB']:,} KB ({r['jsFiles']}개) | {'/'.join(r['encodings'])} | {r['fcpMs']:,} ms | {r['lcpMs']:,} ms |")
base = next(r for r in rows if r["target"] == "1-before" and r["path"] == "/")
best = next(r for r in rows if r["target"] == "3-split" and r["path"] == "/")
lines += ["", f"→ 메인 FCP {base['fcpMs']/1000:.1f}초 → {best['fcpMs']/1000:.1f}초 ({base['fcpMs']/best['fcpMs']:.1f}배), 받은 JS {base['jsKB']:,} KB → {best['jsKB']:,} KB"]
(out / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n".join(lines))
PY
log "끝. 요약: $OUT/summary.md"
