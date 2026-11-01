"""perf/run-backend.sh 가 남긴 원본(k6 요약 JSON, 서버 지표)으로 표를 만든다.

  python3 perf/summarize.py perf/results/<시각>

결과: <폴더>/summary.md, <폴더>/summary.json
숫자는 전부 원본 파일에서 계산한다. 손으로 옮겨 적지 않는다.
"""
import json
import statistics
import sys
from pathlib import Path

ORDER = ["1-before", "2-batch", "3-cache"]
LABEL = {
    "1-before": "① 개선 전 (AlgorithmTag @BatchSize 없음, 캐시 없음)",
    "2-batch": "② @BatchSize",
    "3-cache": "③ @BatchSize + Redis 캐시",
}


def main(folder: Path) -> None:
    rows = []
    for state in ORDER:
        runs = sorted(folder.glob(f"{state}-run*.json"))
        if not runs:
            continue
        rps, p95, p99, fail = [], [], [], []
        for f in runs:
            m = json.loads(f.read_text())["metrics"]
            rps.append(m["http_reqs"]["values"]["rate"])
            p95.append(m["http_req_duration"]["values"]["p(95)"])
            p99.append(m["http_req_duration"]["values"]["p(99)"])
            fail.append(m["http_req_failed"]["values"]["rate"])
        server = json.loads((folder / f"{state}-server.json").read_text())
        reqs = server["problemRequests"] or 1
        rows.append({
            "state": state,
            "runs": len(runs),
            "rpsEach": [round(x, 1) for x in rps],
            "rps": statistics.median(rps),
            "p95Each": [round(x, 2) for x in p95],
            "p95": statistics.median(p95),
            "p99": statistics.median(p99),
            "failRate": max(fail),
            "sqlPerRequest": server["preparedStatements"] / reqs,
            "redisDbsize": server["redisDbsize"],
        })

    if not rows:
        sys.exit(f"{folder} 에 k6 요약 파일(*-run*.json)이 없다. run.log 를 확인할 것")
    base = next((r for r in rows if r["state"] == "1-before"), None)
    lines = [
        "# 문제 목록 API 성능 측정 결과",
        "",
        "perf/summarize.py 가 원본 파일(`*-run*.json`, `*-server.json`)에서 만든 표다. 측정 조건은 `env.txt`.",
        "",
        "| 단계 | 처리량 (req/s, 중앙값) | p95 (ms) | p99 (ms) | 요청당 SQL | 측정 후 Redis 키 | ① 대비 처리량 |",
        "|---|---|---|---|---|---|---|",
    ]
    for r in rows:
        ratio = f"{r['rps'] / base['rps']:.2f}×" if base else "-"
        lines.append(
            f"| {LABEL[r['state']]} | {r['rps']:,.0f} ({', '.join(f'{x:,.0f}' for x in r['rpsEach'])}) "
            f"| {r['p95']:.2f} ({', '.join(f'{x:.2f}' for x in r['p95Each'])}) | {r['p99']:.2f} "
            f"| {r['sqlPerRequest']:.2f} | {r['redisDbsize']} | {ratio} |"
        )
    lines += [
        "",
        f"- 실패율: 모든 실행에서 최대 {max(r['failRate'] for r in rows) * 100:.2f}%",
        "- 처리량·p95 괄호 안은 본 측정 각 회의 값, 바깥은 중앙값",
        "- 요청당 SQL = 측정 동안 앱이 실행한 SQL 수(Hibernate 통계, 워밍업 포함) ÷ /api/problems 요청 수",
        "- 측정 후 Redis 키: 1·2 단계는 0 이어야 캐시가 꺼진 것, 3 단계는 1(같은 요청이라 키 하나)",
    ]
    if base and len(rows) == 3:
        b, c = rows[1], rows[2]
        lines += [
            "",
            f"→ @BatchSize 로 {b['rps'] / base['rps']:.2f}배, 캐시를 더해 누적 {c['rps'] / base['rps']:.2f}배. "
            f"p95 {base['p95']:.1f} ms → {c['p95']:.1f} ms.",
        ]
    (folder / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (folder / "summary.json").write_text(json.dumps(rows, indent=2, ensure_ascii=False), encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
