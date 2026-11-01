# codeXray

> 알고리즘 문제풀이 기록과 **AI 코드 분석**을 한곳에서. 내 풀이를 X-ray 하듯 들여다보는 학습 플랫폼

codeXray는 알고리즘 문제를 풀고, 풀이·메모·노트를 남기고, **Claude로 내 코드를 분석하거나 힌트를 받고**, 커뮤니티에서 함께 성장하는 서비스입니다. 웹과 **브라우저 확장** 두 클라이언트를 지원하며, GitHub Actions → GHCR → 오라클 클라우드 Arm 서버로 이어지는 **CI/CD 위에서 컨테이너로 배포**됩니다.

**서비스** https://codexray.org · **기간** 2026.06 ~ (1인 개발) · **스택** Java 21 · Spring Boot 4.1 · PostgreSQL · Redis · Kafka · React 19

| 숫자로 보는 개선 | 전 → 후 | 근거 |
|---|---|---|
| 문제 목록 API 처리량 | 1,676 → **12,950 req/s (7.73배)** | [측정 원본](perf/results/20261101-0918/summary.md) · Grafana |
| 문제 목록 API p95 | 55.0 → **8.0 ms** | 〃 |
| 요청당 SQL(문제 목록) | 21 → 4 → **0** | 〃 · `ProblemListQueryCountTest` |
| 풀이 목록 SQL(20개 페이지) | 72 → **4 이하** | `SolutionListQueryCountTest` |
| 첫 화면 FCP(느린 4G) | 9.8 → **1.0초**, JS 1,741 → 111 KB | [측정 원본](perf/results/20261101-0950-fcp/summary.md) |

---

## 📚 목차
- [화면](#-화면)
- [주요 기능](#-주요-기능)
- [시스템 아키텍처](#-시스템-아키텍처)
- [성능 최적화](#-성능-최적화)
- [안정성 · 보안](#-안정성--보안)
- [기술적 의사결정](#-기술적-의사결정)
- [CI/CD & 배포](#-cicd--배포)
- [테스트](#-테스트)
- [로컬 실행](#-로컬-실행)
- [프로젝트 구조](#-프로젝트-구조)

---

## 🖥 화면

시연 데이터([`deploy/demo-seed.sql`](deploy/demo-seed.sql))를 넣고 [`docs/capture-screens.sh`](docs/capture-screens.sh)로 자동 캡처한 화면입니다. 화면이 바뀌면 같은 명령으로 다시 찍습니다. AI 결과(15·16)와 브라우저 확장(17·18)은 운영 사이트(https://codexray.org)에서 시연 계정으로 직접 찍었습니다.

| | |
|---|---|
| ![AI 풀이 설명](docs/screenshots/15-ai-explain.png) **AI 풀이 설명** · 내 풀이(C++)를 Claude가 단계별로 설명. 요청은 Kafka로 넘기고 화면은 잡 상태를 폴링 | ![AI 힌트](docs/screenshots/16-ai-hint.png) **AI 힌트** · 아직 못 푼 문제에 정답 코드 없이 단계별 힌트 |
| ![메인](docs/screenshots/01-landing.png) **메인** | ![문제 목록](docs/screenshots/03-problems.png) **문제 목록** · 약점 태그 기반 추천, 티어·출처·태그 필터 |
| ![문제 상세](docs/screenshots/04-problem-detail.png) **문제 상세** · 보정 티어, AI 힌트, 체감 난이도 피드백 | ![풀이 등록](docs/screenshots/07-solution-form.png) **풀이 등록** · 언어별 문법 강조 편집기 |
| ![풀이 상세](docs/screenshots/06-solution-detail.png) **풀이 상세** · AI 분석(풀이 설명·최적화 제안), 메모 | ![내 풀이](docs/screenshots/05-solutions.png) **내 풀이** · 별표, 메모 표시 |
| ![노트](docs/screenshots/09-note-detail.png) **학습 노트** · 마크다운, 코드 블록 복사 | ![프로필](docs/screenshots/14-profile.png) **프로필 대시보드** · 스트릭, 티어·언어·태그 분포, 약점 태그 |
| ![커뮤니티 글](docs/screenshots/11-community-post.png) **커뮤니티** · 질문/풀이 공유, 추천, 댓글, 신고 | ![건의사항](docs/screenshots/13-suggestion-post.png) **건의사항** · 처리 상태, 관리자 답변 |
| ![확장 저장](docs/screenshots/17-extension-save.png) **브라우저 확장** · 프로그래머스 문제 화면의 `❯ 저장` 버튼, 원클릭 저장 | ![확장 로그인](docs/screenshots/18-extension-login.png) **확장 로그인** · 쿠키 대신 `X-Refresh-Token` 헤더로 인증 |

<details>
<summary>나머지 화면 (로그인, 노트 목록, 커뮤니티 목록, 건의사항 목록)</summary>

| | |
|---|---|
| ![로그인](docs/screenshots/02-login.png) | ![노트 목록](docs/screenshots/08-notes.png) |
| ![커뮤니티 목록](docs/screenshots/10-community.png) | ![건의사항 목록](docs/screenshots/12-suggestions.png) |

</details>

---

## ✨ 주요 기능

| 도메인 | 내용 |
|---|---|
| **인증** | 이메일 가입·인증메일, JWT(access) + refresh 토큰 **회전**, 소셜 로그인(Google/Naver), 로그인 시도 제한 |
| **계정** | 닉네임·비밀번호 변경(바꾸면 다른 기기 로그아웃), 탈퇴 시 개인 데이터는 DB `ON DELETE CASCADE`로 즉시 삭제. [개인정보처리방침](https://codexray.org/privacy) |
| **문제** | 689문제, 티어·출처·태그 필터와 정렬, 페이지 — QueryDSL 동적 쿼리 + Redis 캐시 |
| **풀이/기록** | 풀이 제출·조회, 풀이별 메모(틀린 이유·로직·핵심 함수), 개인 노트(마크다운) |
| **AI 분석** | 풀이 설명·최적화 제안·문제 힌트를 **Claude로 생성**. Kafka 비동기 + 잡 폴링, 사용자별 하루 한도 |
| **체감 난이도** | 사용자 피드백(0~5)을 모아 문제 티어를 다시 계산 |
| **추천 · 대시보드** | 약점 태그(체감 난이도가 높은 태그) 기반 문제 추천, 학습 스트릭과 분포 |
| **커뮤니티 · 건의사항** | 글·댓글·추천·신고, 건의사항 처리 상태와 관리자 답변, 활동 알림 |
| **브라우저 확장** | 프로그래머스 문제 화면에서 풀이를 원클릭 저장 ([`extension/`](extension/README.md)) |

웹은 refresh 토큰을 httpOnly 쿠키로, 쿠키를 쓸 수 없는 **확장은 `X-Refresh-Token` 헤더**로 주고받도록 인증 계층을 나눴습니다.

---

## 🏗 시스템 아키텍처

![시스템 아키텍처](docs/architecture.svg)

<sub>슬라이드용 PNG: [`docs/architecture.png`](docs/architecture.png)</sub>

오라클 클라우드 Arm 서버 한 대에서 Docker Compose 묶음으로 돕니다. 같은 서버에 다른 앱(Bandal)이 자기 묶음으로 함께 살고, HTTPS를 푸는 **바깥 Nginx**만 같이 씁니다. 서버가 밖으로 여는 포트는 80, 443뿐입니다.

- **same-origin**: 화면 nginx가 React 정적 파일을 주고 `/api`만 백엔드로 넘깁니다. 화면과 API가 같은 오리진이라 CORS가 필요 없고 쿠키 인증이 단순해집니다.
- **네트워크 격리**: 바깥 Nginx와 같은 `edge` 네트워크에 붙는 건 입구(frontend) 하나뿐입니다. backend와 PG·Redis·Kafka는 `ports`가 없어 인터넷에서도, 바깥 Nginx에서도 닿지 않습니다. 저장소는 앱마다 따로 두어 한 앱의 실수(디스크, 잘못된 마이그레이션, Redis 비우기)가 다른 앱으로 번지지 않습니다.
- **메모리 상한**: 컨테이너마다 `mem_limit`을 두고(JVM은 그 75%, Kafka 힙 512MB) 한 앱이 메모리를 다 먹어도 다른 앱이 살게 했습니다.
- **AI 비동기**: 수 초~수십 초 걸리는 LLM 호출을 요청 흐름에서 떼어 Kafka로 처리합니다. 사용자는 바로 잡 ID를 받고 결과를 폴링합니다.

---

## ⚡ 성능 최적화

### 문제 목록 API — N+1 제거 → 캐시

**문제**: 목록 응답은 문제마다 태그를 붙이는데, `ProblemTag → AlgorithmTag` 연관이 지연 로딩이라 **태그를 하나씩 따로 조회**했습니다. 한 페이지(50문제)에 `select … from algorithm_tags where id=?`가 서로 다른 태그 수만큼 나갔습니다.

1. **근본 해결 — `@BatchSize`**: `AlgorithmTag`에 `@BatchSize(100)`를 달아 개별 조회를 `id = ANY(?)` 한 번으로 줄였습니다. **캐시가 비어 있어도 N+1이 없습니다.**
2. **반복 조회 캐시 — Redis `@Cacheable(sync = true)`**: 자주 읽고 거의 안 바뀌는 목록·상세·태그를 캐시합니다. `sync`로 캐시가 비는 순간 같은 키 요청이 몰려도 DB에는 한 번만 갑니다.

| 단계 | 처리량 (req/s) | p95 | 요청당 SQL | ① 대비 |
|---|---|---|---|---|
| ① 개선 전 | 1,676 | 55.0 ms | 21 | 1.00× |
| ② `@BatchSize` | 3,603 | 27.3 ms | 4 | **2.15×** |
| ③ `@BatchSize` + Redis 캐시 | 12,950 | 8.0 ms | 0 | **7.73×** |

<sub>2026-11-01 · 로컬(Ryzen 5 7500F, WSL2) · k6 50 VU, think-time 0, 워밍업 30초 + 본 측정 30초 × 2의 중앙값 · 단계마다 JVM 재시작·Redis 비움 · 실패율 0% · 원본: [`perf/results/20261101-0918/`](perf/results/20261101-0918/summary.md)</sub>

| 처리량 | p95 | 요청당 SQL |
|---|---|---|
| ![처리량](perf/results/20261101-0918/grafana/bar-throughput.png) | ![p95](perf/results/20261101-0918/grafana/bar-p95.png) | ![요청당 SQL](perf/results/20261101-0918/grafana/ts-sql-per-request.png) |

<sub>p95·SQL 막대의 숫자는 표와 같습니다. 처리량 막대는 길이(비율)만 보여 줍니다. 표의 처리량은 k6 요약(요청 수 ÷ k6가 잰 실행 시간)이라 Prometheus의 구간 값으로는 똑같이 만들 수 없어서, 숫자가 두 개로 보이지 않게 막대에서는 숨겼습니다.</sub>

- **같은 조건으로 누구나 다시 잴 수 있게** 만들었습니다. [`perf/run-backend.sh`](perf/README.md) 한 번이면 ①②③ 코드 상태를 패치로 만들어(운영 코드에 측정용 스위치 없음) 측정 전용 Postgres·Redis·Prometheus·Grafana를 띄우고, k6 원본 JSON과 서버 지표(Hibernate SQL 수, Redis 키 수)를 저장합니다.
- **요청당 SQL 21** = 본문 1 + count 1 + 태그 연결 배치 1 + 태그 개별 조회 18(첫 페이지 50문제의 서로 다른 태그 수). ②의 4는 회귀 테스트(`ProblemListQueryCountTest`)로 상한을 고정했습니다.
- **무효화 전략**: 문제 상세는 체감 난이도로 그 문제의 티어가 바뀔 때 해당 키만, 목록은 어떤 필터 키가 영향받는지 알 수 없어 전체 + 30분 TTL, 태그는 바뀌지 않는 참조 데이터라 TTL(1시간)만. 자유 검색어 요청은 키가 사실상 무한해 캐시하지 않습니다.
- 현재 규모(689행)에선 `EXPLAIN ANALYZE` 기준 PostgreSQL이 Seq Scan + Hash Join(0.2~0.5ms)을 고릅니다. 인덱스보다 **왕복 횟수(N+1)** 가 병목이라 그것부터 잡았습니다.

> **이전 측정과의 관계** — 2026-07(9.5배)과 2026-08(8.6배)에도 같은 개선을 쟀지만, 조건을 글로만 남겨 다시 잴 때마다 절대값이 1.6~2.3배씩 달라졌습니다. 이번에 조건을 스크립트로 고정하고 원본을 저장소에 남긴 2026-11 측정(7.73배)으로 모두 바꿨습니다. 이전 값은 인용하지 않습니다.

### 풀이·커뮤니티 목록 — N+1 제거와 페이지

풀이 목록은 풀이마다 문제와 메모를 따로 읽어 20개 페이지에 **SQL 72개**가 나갔습니다. `join fetch` + 별도 count 쿼리로 바꿔 **4개 이하**로 줄이고 테스트(`SolutionListQueryCountTest`)로 고정했습니다. 풀이·노트·커뮤니티·건의사항 목록은 전부 페이지(기본 20, 최대 50)로 바꿨습니다. 커뮤니티 추천순은 추천 합계를 상관 서브쿼리로 정렬해 글 수만큼 쿼리가 늘지 않습니다.

### 화면 첫 로딩 — gzip + 화면 나누기

운영 사이트를 느린 회선 조건으로 재 보니 첫 화면까지 **약 10초**였습니다. 원인은 ① 화면 nginx에 gzip이 꺼져 1.7MB JS를 그대로 보냈고, ② `App.tsx`가 모든 화면을 처음부터 import해 풀이 작성 화면에만 쓰는 편집기(CodeMirror와 언어 9개)·코드 강조·마크다운까지 메인에서 받았기 때문입니다. gzip을 켜고 메인을 뺀 화면을 `React.lazy`로 나눴습니다.

| 단계 | 메인 받은 JS | 메인 FCP | `/problems` LCP |
|---|---|---|---|
| 고치기 전 | 1,741 KB | 9,785 ms | 9,881 ms |
| ① gzip | 570 KB | 3,569 ms | 3,665 ms |
| ② gzip + 화면 나누기 | 111 KB | **1,034 ms** | 1,485 ms |

<sub>2026-11-01 · Playwright(Chromium), 캐시 없음, 느린 4G(150ms · 1.6Mbps/750kbps), CPU 4배 감속, 5회 중앙값 · 원본: [`perf/results/20261101-0950-fcp/`](perf/results/20261101-0950-fcp/summary.md)</sub>

**운영에서만 gzip이 빠졌다** — 배포 뒤 운영을 다시 재니 메인 FCP가 2.5초(JS 340KB, 압축 안 됨)였습니다. 응답에 `Vary: Accept-Encoding`은 있는데 `Content-Encoding`이 없었습니다. 운영은 브라우저 → 바깥 Nginx → 화면 nginx 순서인데, 바깥 Nginx는 안쪽으로 **HTTP/1.0**으로 넘기고(`proxy_http_version` 기본값) nginx gzip은 기본으로 **HTTP/1.1 요청만** 압축합니다(`gzip_http_version 1.1`). 로컬은 브라우저가 화면 nginx에 바로 붙어 드러나지 않았습니다. 화면 nginx에 `gzip_http_version 1.0`을 넣어 앱 저장소 안에서 고쳤고, 측정 스크립트가 운영과 같은 프록시를 앞에 세워 이 경로도 재현합니다(고치기 전 2,405ms → 고친 뒤 1,040ms, 헤더 원본 [`headers.txt`](perf/results/20261101-0950-fcp/headers.txt)).

---

## 🛡 안정성 · 보안

포트폴리오로 정리하며 코드 전체를 다시 읽고 고친 것들입니다. 각 항목은 테스트가 함께 들어갔습니다.

**AI 잡 (Kafka)**
- **중복 호출 방지**: 컨슈머가 `PENDING → PROCESSING`을 조건부 UPDATE로 가져가고, 바뀐 행이 1일 때만 Claude를 부릅니다. Kafka가 같은 메시지를 다시 줘도 비용이 두 번 나가지 않습니다(`AiJobTransitionTest`).
- **발행 실패**: Kafka 발행이 실패하면 잡을 바로 `FAILED`로 바꾸고 하루 한도를 1회 돌려줍니다. 처리 중 실패는 2번 재시도 뒤 `ai-jobs.DLT`로 보내고, 15분 넘게 멈춘 잡은 스케줄러가 `FAILED`로 정리합니다.
- **프롬프트 인젝션 완화**: 사용자 코드는 `<user_code>` 태그 안의 데이터로만 넘기고, 코드 안의 닫는 태그는 이스케이프합니다(`AiPromptTest`). 모델이 거절하면 `AI_REFUSED`로 구분해 알려 줍니다.

**인증**
- **refresh 회전 원자화**: 기존 토큰 확인과 삭제를 Redis `GETDEL` 한 번으로 합쳤습니다. 같은 토큰으로 동시에 두 번 갱신해도 한 쪽만 성공합니다(`RefreshTokenStoreTest`의 동시성 테스트).
- **로그인 시도 제한**: 이메일당 15분에 5회 실패하면 잠급니다. 없는 이메일에도 같은 bcrypt 비용을 들여 응답 시간으로 가입 여부가 드러나지 않게 했습니다. 인증메일 재발송은 1분 간격이며 가입 여부와 상관없이 같은 응답을 줍니다.
- **확장 로그아웃**: 확장이 로그아웃할 때 서버의 refresh 토큰도 지웁니다(이전에는 팝업 상태만 지웠습니다).

**데이터 정확성**
- **티어 경계 오분류**: 보정 레벨을 3등분해 티어를 정할 때 부동소수점 오차로 경계값(예: 1/3 지점)이 아래 티어로 떨어지던 것을 바로잡았습니다(`RatingCalculatorTest`).
- **피드백 동시 제출**: 같은 문제에 피드백이 동시에 들어오면 티어 재계산이 서로 덮어썼습니다(lost update). 문제 행을 `SELECT … FOR UPDATE`로 잠그고 계산합니다.

**API · 화면**
- **관리자 API 이중 방어**: URL 규칙에 더해 메서드마다 `@PreAuthorize`를 달아, 경로 규칙을 실수로 바꿔도 막힙니다.
- **정렬 허용 목록**: 사용자가 준 정렬 키는 허용한 필드만 받습니다. 같은 값일 때 `id desc`로 순서를 고정해 페이지가 넘어가도 글이 겹치거나 빠지지 않습니다.
- **보안 헤더**: `nosniff`, `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `Referrer-Policy`, HSTS.

---

## 💡 기술적 의사결정

- **AI 호출은 Kafka로 비동기화** — LLM 응답은 수 초~수십 초가 걸려, 동기로 처리하면 요청 스레드를 오래 잡습니다. 잡을 먼저 커밋하고 이벤트를 발행해 API는 바로 응답하고, Claude 호출은 트랜잭션 밖에서 합니다(DB 커넥션을 잡은 채 기다리지 않게).
- **JWT access + refresh 회전, 웹·확장 두 경로** — 수명이 짧은 access와 Redis에 두고 쓸 때마다 바꾸는 refresh를 나눴습니다. 웹은 httpOnly 쿠키로 XSS 탈취 위험을 낮추고, 쿠키를 못 쓰는 확장은 `X-Client`/`X-Refresh-Token` 헤더로 같은 로직을 씁니다.
- **비밀번호를 바꾸면 다른 기기 로그아웃** — refresh를 "토큰 → 사용자"로만 두면 비밀번호를 바꿔도 다른 기기 로그인이 최대 14일 살아 있습니다. 사용자별 토큰 목록(Redis Set)을 함께 두고 비밀번호 변경·탈퇴 때 한 번에 지웁니다. 이미 나간 access(30분)는 요청마다 블랙리스트를 조회하는 비용 대신 만료까지 두는 쪽을 골랐습니다.
- **Flyway가 스키마를 소유, Hibernate는 `validate`만** — 마이그레이션(V1~V11)으로 변경 이력을 남기고, 엔티티와 스키마가 어긋나면 부팅 때 잡습니다.
- **탈퇴 정리는 DB 외래키가 맡는다** — 사용자 소유 테이블은 `ON DELETE CASCADE`, 다른 사람과 얽힌 글·댓글은 `ON DELETE SET NULL`. 붙여넣은 코드가 담기는 `ai_jobs`만 외래키가 빠져 탈퇴해도 남던 것을 V11에서 바로잡았습니다(`AiJobUserCascadeTest`).
- **성능은 캐시부터가 아니라 쿼리부터** — 캐시로 덮기 전에 N+1을 고쳐 캐시가 비어도 느려지지 않게 하고, 그 위에 캐시를 얹었습니다. 이미 refresh 토큰용으로 쓰던 Redis를 재사용해 인프라를 늘리지 않았습니다.
- **측정은 다시 잴 수 있어야 숫자다** — 성능 숫자마다 스크립트·조건·원본 JSON을 저장소에 둡니다. 측정 단계는 운영 코드에 스위치를 두지 않고 패치로 만듭니다.
- **전역 예외 처리 일원화** — `ErrorCode` enum + `BusinessException` + `@RestControllerAdvice`. 잘못된 파라미터·깨진 JSON은 400, 없는 주소는 404, `@PreAuthorize` 거부는 로그인 여부에 따라 401/403, 예상 못 한 예외만 500 + 서버 로그(내부 메시지는 응답에 싣지 않음).
- **배포 구조의 트레이드오프** — 처음엔 AWS EC2 두 대(애플리케이션 / 데이터)로 나눴고, 지금은 다른 앱과 함께 쓰는 Arm 서버 한 대입니다. 계층 분리는 Docker 네트워크 격리(저장소에 포트 없음)와 컨테이너별 메모리 상한으로 대신했습니다([CI/CD & 배포](#-cicd--배포)의 이전 이유 참고).

---

## 🚀 CI/CD & 배포

`master`에 머지하면 **테스트 → Arm 이미지 빌드 → GHCR 푸시 → 서버 배포(실패하면 자동 되돌림)** 까지 자동으로 돕니다.

**① CI** (`.github/workflows/ci.yml`)
- 백엔드는 Postgres 16 · Redis 7 서비스 컨테이너를 띄우고 `./gradlew test`(실제 DB가 필요한 쿼리 수·페이지·탈퇴 정리·토큰 동시성 테스트 포함). 프론트는 `npm run lint`와 `npm run build`(타입 검사). 하나라도 실패하면 이미지 빌드로 넘어가지 않습니다.

**② 이미지 빌드 & 푸시**
- 서버가 Arm이라 `linux/arm64` 이미지를 만듭니다. Gradle·npm 빌드는 `--platform=$BUILDPLATFORM`으로 **러너의 x86 CPU에서** 돌리고(결과물은 CPU와 무관), Arm 흉내(QEMU)는 마지막 실행 단계만 탑니다. 흉내 속에서 Gradle을 돌리면 몇 배 느려집니다.
- 이미지는 **커밋 SHA 태그**로 푸시합니다. 소셜 로그인 Client ID(공개값)는 저장소 Variables에서 `build-args`로, secret은 `production` 환경 Secrets에서 백엔드 `.env`로만 들어갑니다.

**③ 배포 — SSH + 자동 되돌리기** (`deploy/deploy.sh`)
- Actions가 SSH로 compose 파일·스크립트·`.env`(Secrets로 만들어 600 권한)를 올리고 `deploy.sh <커밋SHA>`를 실행합니다.
- `deploy.sh`는 **배포 직전 DB를 백업**(빈 Postgres에 복구해 행 수를 비교한 뒤 R2에 업로드)하고, 새 SHA로 띄운 뒤 모든 컨테이너가 healthy가 되길 기다립니다. 안 되면 **마지막으로 성공한 SHA로 되돌리고** 워크플로를 실패로 끝냅니다.
- SSH는 키로만 열고 배포 전용 사용자(sudo 없음)로 들어가며, 호스트 키를 Secrets에 고정해 둡니다. 매일 새벽 백업은 같은 스크립트를 cron이 돌리고, 성공 신호가 끊기면 healthchecks.io가 메일을 보냅니다.

<details>
<summary>AWS EC2 2대 → 오라클 서버 1대로 옮긴 이유 (2026-10)</summary>

처음에는 AWS EC2 두 대(애플리케이션 / 데이터 계층)에 배포하고 Watchtower로 자동 배포했습니다. SSH를 운영자 IP로 잠근 보안그룹에 IP가 매번 바뀌는 Actions 러너가 들어오려면 22번을 열어야 해서, 서버가 스스로 `:latest`를 확인하는 pull 방식을 골랐습니다. EC2는 확인 뒤 내리고, 이미 운영 중이던 오라클 Arm 서버(다른 앱과 공유)로 옮기며 배포를 SSH로 바꿨습니다.

- 이 서버는 SSH를 **키 전용**으로만 열어 IP 제한 없이도 러너가 들어올 수 있습니다.
- Watchtower는 Docker 소켓(= 서버 root 권한)을 쥐어야 해서, 다른 앱과 같이 쓰는 서버에서는 위험이 큽니다.
- `:latest`는 지금 무엇이 떠 있는지 알 수 없고 되돌릴 기준도 없습니다. **SHA 태그 + 되돌리기 + 배포 전 백업**이 실제 운영에서 더 중요했습니다.

</details>

---

## 🧪 테스트

백엔드 테스트 47개(12개 클래스). 단위 테스트로 끝나지 않는 것은 실제 Postgres·Redis에 붙여 확인합니다.

| 무엇을 | 테스트 |
|---|---|
| 목록 쿼리 수 상한(N+1 회귀 방지) | `ProblemListQueryCountTest`(≤4), `SolutionListQueryCountTest`(≤4, 고치기 전 72) |
| 커뮤니티 페이지·추천순 정렬 | `CommunityPostPageTest` |
| AI 잡 상태 전이(한 번만 가져감, 실패 처리) | `AiJobTransitionTest` |
| 프롬프트 구성(코드 이스케이프) | `AiPromptTest` |
| refresh 토큰 회전 동시성, 사용자별 일괄 만료 | `RefreshTokenStoreTest` |
| 로그인 시도 제한 | `LoginAttemptLimiterTest` |
| 티어 계산·경계값 | `RatingCalculatorTest` |
| 탈퇴 시 데이터 정리(FK) | `AiJobUserCascadeTest` |
| 닉네임·비밀번호 변경(중복, 소셜 계정) | `UserServiceTest` |
| 오류 응답 코드(400/401/404/500, 내부 메시지 숨김) | `GlobalExceptionHandlerTest` |

---

## 💻 로컬 실행

**사전 준비**: JDK 21, Node 20+, Docker

```bash
# 1) 데이터 인프라 (Postgres / Redis / Kafka) — backend/docker-compose.yml
cd backend
cp .env.example .env        # DB 비밀번호, JWT_SECRET(32자 이상), ANTHROPIC_API_KEY 등
docker compose up -d

# 2) 백엔드
./gradlew bootRun

# 3) 프론트엔드
cd ../frontend
npm install
npm run dev                 # Vite 개발 서버, /api 는 8080 으로 프록시
```

| 하고 싶은 것 | 명령 |
|---|---|
| 성능 다시 재기(문제 목록 API, 약 12분) | `perf/run-backend.sh` → Grafana http://localhost:3300 |
| 화면 첫 로딩 다시 재기 | `perf/run-frontend.sh` |
| README 화면 다시 찍기 | `docs/capture-screens.sh` |

> `.env` 파일은 비밀값을 담으므로 커밋하지 않습니다(`.env.example`만 제공). 측정·캡처 스크립트는 별도 컨테이너(`perf/compose.yml`)를 써서 개발 DB를 건드리지 않습니다.

---

## 📁 프로젝트 구조

```
codeXray/
├── backend/                  # Spring Boot (Java 21)
│   ├── src/main/java/...      # 도메인별 패키지: auth, user, problem, solution, rating, note,
│   │                          #   community, notification, stats, recommend, ai, scheduler, common, config
│   ├── src/main/resources/
│   │   ├── application.yml    # (+ application-perf.yml: 측정 전용 프로필)
│   │   └── db/migration/      # Flyway V1~V11 (스키마 + 문제 시드)
│   └── Dockerfile
├── frontend/                 # React 19 + Vite + TS
│   ├── nginx.conf            # 정적 서빙 + /api 프록시 + gzip + 보안 헤더
│   └── Dockerfile
├── extension/                # 크롬 확장(Manifest V3): 프로그래머스 → 풀이 저장
├── deploy/                   # 서버(/srv/codexray)에 올라가는 파일
│   ├── compose.yml           # frontend + backend + pg + redis + kafka 한 묶음
│   ├── deploy.sh             # SHA 태그로 띄우고, 안 되면 이전 SHA로 되돌림
│   ├── backup.sh             # 덤프 → 복구 검증 → R2 업로드 (매일 + 배포 직전)
│   └── demo-seed.sh/.sql     # 시연 데이터(.example 계정 5개). 몇 번 돌려도 같은 모양
├── perf/                     # 성능 측정: k6 · Prometheus · Grafana · Playwright, 원본 결과(results/)
├── docs/                     # 아키텍처 그림, 화면 캡처와 그 스크립트
└── .github/workflows/ci.yml  # CI(test) + CD(Arm 이미지 → GHCR → SSH 배포)
```
