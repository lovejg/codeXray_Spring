"""perf/grafana/dashboards/codexray-perf.json 생성기. 패널을 고칠 때는 이 파일을 고치고 다시 뽑는다:  python3 perf/grafana/make-dashboard.py perf/grafana/dashboards/codexray-perf.json"""
import json, sys

DS = {"type": "prometheus", "uid": "prom"}
COLORS = {"1-before": "#F2495C", "2-batch": "#FF9830", "3-cache": "#73BF69"}
NAMES = {"1-before": "① 개선 전", "2-batch": "② @BatchSize", "3-cache": "③ + Redis 캐시"}


def overrides():
    return [
        {
            "matcher": {"id": "byName", "options": s},
            "properties": [
                {"id": "color", "value": {"mode": "fixed", "fixedColor": c}},
                {"id": "displayName", "value": NAMES[s]},
            ],
        }
        for s, c in COLORS.items()
    ]


pid = 0


def panel(kind, title, desc, expr, unit, x, y, w, h, legend="{{state}}", instant=False, decimals=None, extra=None,
          extra_options=None):
    global pid
    pid += 1
    p = {
        "id": pid,
        "type": kind,
        "title": title,
        "description": desc,
        "datasource": DS,
        "gridPos": {"x": x, "y": y, "w": w, "h": h},
        "targets": [{
            "refId": "A", "datasource": DS, "expr": expr, "legendFormat": legend,
            "instant": instant, "range": not instant,
        }],
        "fieldConfig": {"defaults": {"unit": unit, "min": 0}, "overrides": overrides()},
        "options": {},
    }
    if decimals is not None:
        p["fieldConfig"]["defaults"]["decimals"] = decimals
    if kind == "timeseries":
        p["fieldConfig"]["defaults"]["custom"] = {"lineWidth": 2, "fillOpacity": 12, "showPoints": "never", "spanNulls": False}
        p["options"] = {"legend": {"displayMode": "list", "placement": "bottom"}, "tooltip": {"mode": "multi"}}
    if kind == "bargauge":
        # 단계 이름은 막대 위에 한 줄로(왼쪽에 두면 폭이 모자라 "② @BatchSi…"처럼 잘렸다)
        p["options"] = {"orientation": "horizontal", "displayMode": "basic", "showUnfilled": True,
                        "valueMode": "color", "namePlacement": "top", "sizing": "auto",
                        "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False}}
        p["fieldConfig"]["defaults"]["color"] = {"mode": "fixed", "fixedColor": "#8AB8FF"}
    if extra_options:
        p["options"].update(extra_options)
    if extra:
        p.update(extra)
    return p


RUN = 'run=~"run.*"'
panels = [
    {"id": 100, "type": "text", "title": "", "gridPos": {"x": 0, "y": 0, "w": 24, "h": 3},
     "options": {"mode": "markdown", "content":
        "**GET /api/problems (50건) · k6 50 VU · think-time 0 · 단계마다 워밍업 30초 + 본 측정 30초×2 · JVM 재시작·캐시 비움**  \n"
        "처리량 막대는 **길이(비율)만** 보여 준다. 숫자의 정본은 `perf/results/<시각>/summary.md`(k6 요약: 요청 수 ÷ k6 가 잰 실행 시간, 2회 중앙값)다. "
        "p95·SQL 막대의 숫자는 그 표와 같다. 선 그래프의 회색 띠는 단계 구간(annotation)이다."}},
    # 처리량: k6 요약값은 요청 수 ÷ k6 가 잰 실행 시간이라 Prometheus 로는 똑같이 못 만든다(10초 구간 중앙값은 시작·끝의
    # 덜 찬 구간 때문에 약 7% 낮았다: 1,556 vs 1,676). 숫자가 두 개로 보이지 않게 값은 숨기고 막대 길이만 쓴다
    panel("bargauge", "단계별 처리량 (막대 길이 = 비율, 수치는 k6 요약)",
          "본 측정 구간의 10초 처리량 값들의 중앙값으로 길이만 그린다. 수치는 summary.md 의 k6 요약값(1,676 / 3,603 / 12,950)",
          f'quantile_over_time(0.5, (sum by (state) (rate(k6_http_reqs_total{{{RUN}}}[10s])))[$__range:5s])',
          "suffix: req/s", 0, 3, 8, 7, instant=True, decimals=0, extra_options={"valueMode": "hidden"}),
    panel("bargauge", "단계별 p95 응답 시간 (ms, k6)",
          "본 측정 2회 각각의 최종 p95(k6 가 실행 시작부터 누적 계산)의 평균 = 2회 중앙값. summary.md 와 같은 값",
          f'avg by (state) (last_over_time(k6_http_req_duration_p95{{{RUN}}}[$__range])) * 1000',
          "ms", 8, 3, 8, 7, instant=True, decimals=1),
    # SQL: 단계마다 JVM 을 새로 띄워 카운터가 0 에서 시작하므로 마지막 값끼리 나누면 앱이 센 값과 같다(21.00 / 4.00 / 0.00).
    # increase() 는 구간 가장자리를 추정해서 20.86 처럼 어긋났다
    panel("bargauge", "단계별 요청 1회당 SQL 수",
          "앱이 실행한 SQL(Hibernate prepared statement) ÷ /api/problems 요청 수. 워밍업 포함, 단계 전체(JVM 기동부터)",
          'max by (state) (max_over_time(hibernate_statements_total{status="prepared"}[$__range])) / '
          'sum by (state) (max_over_time(http_server_requests_seconds_count{uri="/api/problems"}[$__range]))',
          "short", 16, 3, 8, 7, instant=True, decimals=2),
    panel("timeseries", "처리량 (req/s, k6)", "부하 도구가 받은 응답 수. 워밍업 포함",
          "sum by (state) (rate(k6_http_reqs_total[10s]))", "reqps", 0, 10, 12, 8),
    panel("timeseries", "p95 응답 시간 (ms, k6, 본 측정)", "k6 가 계산한 p95. 실행(run)마다 처음부터 누적이라 시작 직후에는 출렁인다",
          f"max by (state) (k6_http_req_duration_p95{{{RUN}}}) * 1000", "ms", 12, 10, 12, 8),
    panel("timeseries", "요청 1회당 SQL 수", "15초 구간의 SQL 수 ÷ 요청 수. ① 21 → ② 4 → ③ 0(캐시 적중이면 DB 를 읽지 않는다)",
          'sum by (state) (rate(hibernate_statements_total{status="prepared"}[15s])) / '
          'sum by (state) (rate(http_server_requests_seconds_count{uri="/api/problems"}[15s]))',
          "short", 0, 18, 12, 8, decimals=1),
    panel("timeseries", "목록 캐시 적중률", "problemList 캐시의 hit ÷ (hit+miss). 캐시를 뺀 ①·② 에는 값이 없다",
          'sum by (state) (rate(cache_gets_total{cache="problemList",result="hit"}[15s])) / '
          'sum by (state) (rate(cache_gets_total{cache="problemList",result=~"hit|miss"}[15s]))',
          "percentunit", 12, 18, 12, 8),
    panel("timeseries", "서버 p95 (ms, 앱 측정)", "앱 안에서 잰 요청 처리 시간의 p95(네트워크·k6 대기 제외)",
          'histogram_quantile(0.95, sum by (le, state) (rate(http_server_requests_seconds_bucket{uri="/api/problems"}[15s]))) * 1000',
          "ms", 0, 26, 8, 8),
    panel("timeseries", "DB 커넥션 사용 중", "Hikari 풀에서 사용 중인 커넥션(최대 10). ①·② 는 DB 가 병목이다",
          "max by (state) (hikaricp_connections_active)", "short", 8, 26, 8, 8),
    panel("timeseries", "앱 CPU 사용률", "JVM 프로세스 CPU. ③ 은 DB 대신 직렬화·네트워크가 CPU 를 쓴다",
          "max by (state) (process_cpu_usage)", "percentunit", 16, 26, 8, 8),
]

dash = {
    "uid": "codexray-perf",
    "title": "codeXray · 문제 목록 성능 (①→②→③)",
    "tags": ["codexray", "perf"],
    "timezone": "browser",
    "schemaVersion": 39,
    "refresh": "",
    "time": {"from": "now-15m", "to": "now"},
    "annotations": {"list": [
        {"builtIn": 1, "datasource": {"type": "grafana", "uid": "-- Grafana --"}, "enable": True,
         "hide": True, "iconColor": "rgba(0, 211, 255, 1)", "name": "Annotations & Alerts", "type": "dashboard"},
        {"datasource": {"type": "grafana", "uid": "-- Grafana --"}, "enable": True, "iconColor": "#8e8e8e",
         "name": "측정 단계", "target": {"type": "tags", "tags": ["perf"], "limit": 100, "matchAny": True}},
    ]},
    "panels": panels,
}
json.dump(dash, open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False, indent=2)
