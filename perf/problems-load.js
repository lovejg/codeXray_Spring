// 문제 목록 조회(GET /api/problems) 부하 테스트 스크립트.
//
// 이 스크립트는 2026-08-12 재측정에 사용한 것입니다.
// README "성능 재측정(2026-08-12)" 절의 ①/①'/②/③ 네 상태를 모두 이 스크립트 하나로 측정했습니다.
//
// ⚠️ 2026-07 최초 측정에 사용한 스크립트가 아닙니다.
//    당시 스크립트는 보존되지 않았고, 이 파일은 재측정 시점에 새로 작성한 것입니다.
//    따라서 2026-07 표의 수치를 이 스크립트로 재현할 수 있다고 보장하지 않습니다.
//
// 실행:
//   docker run --rm --network host -v "$PWD/perf:/scripts" -i grafana/k6:latest \
//     run /scripts/problems-load.js
//
// 측정 조건은 README의 "부하 조건"을 따릅니다.
// 특히 앱은 SQL 로깅을 끈 상태로 기동해야 합니다(README 유의사항 1번 참고):
//   ./gradlew bootRun --args='--logging.level.org.hibernate.SQL=OFF --spring.jpa.show-sql=false'

import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 50,
  duration: '30s',
  // 확인용 측정이라 임계값(thresholds)은 걸지 않는다. 처리량과 p95 수치만 본다.
};

export default function () {
  const res = http.get('http://localhost:8080/api/problems');
  check(res, { 'status is 200': (r) => r.status === 200 });
  // think-time 0(포화 부하) → sleep() 없음
}
