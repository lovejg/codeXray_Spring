// 문제 목록 조회(GET /api/problems) 부하 테스트 스크립트.
//
// 기록
//   - 2026-08-12 재측정에 처음 쓴 스크립트다(README "성능 재측정(2026-08-12)").
//   - 2026-07 최초 측정의 스크립트가 아니다. 그 스크립트는 남아 있지 않다.
//   - 2026-11 부터는 perf/run-backend.sh 가 이 스크립트로 세 단계를 연달아 잰다(perf/README.md).
//
// 직접 실행(앱이 localhost:8080 에 떠 있어야 한다):
//   docker run --rm --network host -v "$PWD/perf:/scripts" grafana/k6:2.2.0 run /scripts/problems-load.js
//
// 환경 변수(모두 선택)
//   BASE_URL      기본 http://localhost:8080
//   VUS, DURATION 기본 50, 30s  (think-time 0 = 포화 부하)
//   SUMMARY_FILE  주면 결과 요약(JSON)을 이 경로에 쓴다. run-backend.sh 가 perf/results/ 아래로 준다

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: Number(__ENV.VUS || 50),
  duration: __ENV.DURATION || '30s',
  // 요약에 p95·p99 를 함께 남긴다(기본은 p90, p95)
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // 확인용 측정이라 임계값(thresholds)은 걸지 않는다. 처리량과 지연만 본다.
};

export default function () {
  // page 0, size 50, createdAt desc, 필터 없음 — 화면의 첫 진입과 같은 요청
  const res = http.get(`${BASE_URL}/api/problems?page=0&size=50&sort=createdAt,desc`);
  check(res, { 'status is 200': (r) => r.status === 200 });
  // think-time 0(포화 부하) → sleep() 없음
}

// 결과 요약. 원본 수치를 파일로 남겨 표·그래프의 근거로 쓴다(예전 측정은 원본 출력이 남지 않았다)
export function handleSummary(data) {
  const m = data.metrics;
  const line =
    `reqs/s=${m.http_reqs.values.rate.toFixed(1)} ` +
    `p95=${m.http_req_duration.values['p(95)'].toFixed(2)}ms ` +
    `p99=${m.http_req_duration.values['p(99)'].toFixed(2)}ms ` +
    `fail=${(m.http_req_failed.values.rate * 100).toFixed(2)}% ` +
    `reqs=${m.http_reqs.values.count}\n`;
  const out = { stdout: line };
  if (__ENV.SUMMARY_FILE) out[__ENV.SUMMARY_FILE] = JSON.stringify(data, null, 2);
  return out;
}
