# perf — 성능 측정

README의 성능 숫자를 **누가 다시 재도 같은 방법으로 나오게** 하려고 만든 폴더다.
예전 측정은 조건을 글로만 남겨 다시 잴 때 값이 1.6~2.3배씩 달라졌다(2026-07 → 08 → 09). 여기서는 조건을 스크립트와 설정 파일로 고정하고, 원본 결과를 `results/`에 그대로 남긴다.

| 무엇을 | 스크립트 | 결과 |
|---|---|---|
| 문제 목록 API: 개선 전 → `@BatchSize` → Redis 캐시 | `run-backend.sh` | `results/<시각>/` (k6 원본 JSON, 서버 지표, `summary.md`) + Grafana |
| 화면 첫 로딩: 고치기 전 → gzip → 화면 나누기, 운영 경로(HTTP/1.0) 재현 | `run-frontend.sh` | `results/<시각>-fcp/` (`fcp.json`, `headers.txt`, `summary.md`) |
| 대시보드 이미지 | `browser/shoot-grafana.mjs` | `results/<시각>/grafana/*.png` |

## 1. 문제 목록 API (`run-backend.sh`)

```bash
perf/run-backend.sh                    # 세 단계 전부, 약 12분
STATES="3-cache" perf/run-backend.sh   # 한 단계만
```

하는 일
1. `perf/compose.yml`로 측정 전용 Postgres(55432)·Redis(56379)·Prometheus(9390)·Grafana(3300)를 띄운다. 개발용 `codexray-db`/`codexray-redis`와 섞이지 않는다.
2. 단계마다 **지금 커밋을 임시 worktree로 꺼내 패치를 적용**하고 jar를 새로 만든다. 운영 코드에 측정용 스위치를 두지 않으려는 것이다.

   | 단계 | 코드 | 패치 |
   |---|---|---|
   | `1-before` ① | `AlgorithmTag` 클래스 `@BatchSize` 없음 + 목록 `@Cacheable` 없음 (2026-08-05 이전 실제 상태) | `states/1-before.patch` |
   | `2-batch` ② | 목록 `@Cacheable` 없음 | `states/2-batch.patch` |
   | `3-cache` ③ | 지금 코드 그대로 | — |

3. Redis를 비우고 JVM을 새로 띄운 뒤(`perf` 프로필) 30초 쉬고, k6로 **워밍업 30초(버림) + 본 측정 30초 × 2**.
4. k6 원본 요약(`<단계>-run<n>.json`), 측정 후 Redis 키 수와 요청당 SQL 수(`<단계>-server.json`)를 남기고 `summarize.py`가 `summary.md`를 만든다.

고정한 조건(`backend/src/main/resources/application-perf.yml`, `run-backend.sh`)
- 대상 `GET /api/problems?page=0&size=50&sort=createdAt,desc`, k6 50 VU, think-time 0(포화 부하)
- SQL 로그 끔(켜면 개선 전 단계만 3배 느려져 배수가 부풀려진다), Hibernate 통계 켬(요청당 SQL 수를 세려고. 세 단계 모두 같은 조건)
- 앱은 `java -jar`로 띄우고 Gradle 데몬을 끈다(`bootRun`은 데몬이 측정 내내 CPU·메모리를 나눠 써서 32% 낮게 나왔다)
- 모든 지표에 `state` 라벨 → Grafana가 세 단계를 **같은 축**에 색만 바꿔 그린다

Grafana: `http://localhost:3300/d/codexray-perf` (로그인 없음, 로컬 전용). 측정이 끝나면 그 실행의 시간 범위가 들어간 주소가 `results/<시각>/grafana-url.txt`에 남는다. 이미지로 남기려면:

```bash
docker run --rm --network host --ipc=host -v "$PWD/perf/browser:/work" -v "$PWD/perf/results:/results" -w /work \
  mcr.microsoft.com/playwright:v1.49.1-noble node shoot-grafana.mjs "$(cat perf/results/<시각>/grafana-url.txt)" /results/<시각>/grafana
```

## 2. 화면 첫 로딩 (`run-frontend.sh`)

```bash
perf/run-frontend.sh    # 이미지 4개 빌드 + 측정, 약 10분
```

| 경우 | 무엇 |
|---|---|
| `1-before` | `b58e5be`(2026-10-30 고치기 전) 화면 그대로. gzip 없음, 모든 화면이 JS 한 파일 |
| `2-gzip` | 같은 화면 코드 + 지금 nginx 설정 |
| `3-split` | 지금 코드(gzip + `React.lazy`) |
| `4-proxy-nogzip` | 지금 코드에서 `gzip_http_version 1.0`만 뺀 것을 HTTP/1.0으로 넘기는 nginx 뒤에 둠 — 배포 직후 운영에서 압축이 빠졌던 상황 |
| `5-proxy` | 지금 코드를 같은 프록시 뒤에 둠 — 고친 뒤의 운영 경로 |

조건: Playwright Chromium, 매번 새 브라우저 컨텍스트(캐시 없음), 지연 150ms·내려받기 1.6Mbps·올리기 750kbps, CPU 4배 감속, 경우·화면마다 5회 중앙값. 백엔드 없이 화면 이미지만 띄운다.
`headers.txt`에 경우마다 메인 JS의 `Content-Encoding`과 실제 전송 크기를 남긴다(HTTP/1.0 문제의 직접 증거).

## 결과를 옮겨 쓸 때 지킬 것

- **숫자의 정본은 `summary.md`(원본 JSON에서 계산)다.** Grafana 막대는 시각화이고, 집계 방식이 달라 소수점이 조금 다를 수 있다.
- 로컬 한 대에서 앱·DB·부하 도구가 CPU를 나눠 쓴 값이다. **운영 서버의 처리량이 아니다.** 절대값보다 같은 조건에서의 배수를 본다.
- ③은 같은 요청(같은 캐시 키)만 반복한 값이라 **캐시 적중률이 사실상 100%**다. 실제 서비스의 적중률이 아니다.
- 화면 측정은 로컬 이미지 + 네트워크 흉내다. `4-proxy-nogzip`/`5-proxy`도 운영 경로를 **재현**한 것이지 운영 실측이 아니다.
- 다시 재면 절대값은 달라진다. 새로 재면 슬라이드·README의 숫자도 그 실행의 `summary.md`로 바꾼다. 다른 실행의 숫자를 섞지 않는다.
