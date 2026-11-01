# codeXray

[![CI / CD](https://github.com/lovejg/codeXray_Spring/actions/workflows/ci.yml/badge.svg)](https://github.com/lovejg/codeXray_Spring/actions/workflows/ci.yml)

알고리즘 풀이를 문제 단위로 모으고, 막히면 힌트를, 풀고 나면 내 코드의 설명을 Claude에게 받는 학습 서비스. https://codexray.org

프로그래머스에서 문제를 풀면 코드는 남아도 왜 틀렸는지, 어떤 패턴이었는지는 남지 않는다.
codeXray에서는 풀이와 메모(틀린 이유, 로직, 핵심 함수)를 문제마다 모으고, 체감 난이도를 남기면 그 값으로 문제의 티어가 다시 계산된다.
브라우저 확장을 켜 두면 프로그래머스 화면에서 버튼 하나로 풀이가 저장된다.

이 문서는 서비스 소개와 함께, 만들면서 재고 고친 과정을 정리한 기록이다.
숫자는 모두 저장소에 있는 스크립트로 다시 잴 수 있고, 원본 결과 파일을 함께 남겼다.

| | |
| --- | --- |
| [한눈에](#한눈에) | 개선 전후 숫자 요약 |
| [어떻게 돌아가나](#어떻게-돌아가나) · [화면](#화면) | 서비스 규칙과 실제 화면 |
| [기술 스택](#기술-스택) · [구조](#구조) | 무엇을 왜 썼나, 컨테이너와 AI 요청 흐름 |
| [측정 방법](#측정-방법) | 어디서, 어떤 조건으로, 무엇을 기준으로 쟀나 |
| [성능 개선](#성능-개선) | 문제 목록 N+1과 캐시, 풀이 목록 N+1, 첫 화면 로딩 |
| [동시성과 정합성](#동시성과-정합성) | AI 중복 호출, 토큰 회전, 피드백 동시 제출, 티어 경계 |
| [운영과 관측](#운영과-관측) | 배포, 백업, 보안, 운영에서 찾은 버그 |
| [로컬에서 띄우기](#로컬에서-띄우기) · [문서](#문서) | 실행 방법, 테스트, 다른 문서 |

## 한눈에

| 무엇을 | 전 | 후 | 어떻게 |
| --- | --- | --- | --- |
| 문제 목록 API 처리량 (k6 50 VU) | 1,676 req/s | **12,950 req/s** (7.73배) | 태그를 하나씩 읽던 N+1을 `@BatchSize`로, 그 위에 Redis 캐시 |
| 문제 목록 API p95 | 55.0ms | **8.0ms** | 〃 |
| 문제 목록 요청 1회당 SQL | 21 | **4** (캐시 적중이면 0) | 〃 |
| 풀이 목록 20개 한 페이지의 SQL | 72 | **4 이하** | `join fetch` + 별도 count 쿼리 |
| 첫 화면에서 받는 JS / FCP (느린 4G) | 1,741KB / 9,785ms | **111KB / 1,034ms** | 화면 nginx gzip(HTTP/1.0 조건까지) + 화면 나누기 |

## 어떻게 돌아가나

```
문제 찾기 -> 풀이 등록(웹 또는 확장) -> 메모 -> AI 설명·최적화 제안
   |                                    |
   +-> 막히면 AI 힌트(정답 코드 없이)       +-> 체감 난이도(0~5) -> 그 문제의 티어 재계산 -> 약점 태그로 추천
```

- 문제는 프로그래머스 689개다. 공식 레벨과 정답률로 시작 티어를 정하고, 사용자가 남긴 체감 난이도로 보정한다.
- 체감 난이도는 풀이를 등록한 문제에만 남길 수 있다. 남기면 그 문제의 티어를 바로 다시 계산하고, 매주 일요일 새벽 전체를 한 번 더 계산한다.
- 체감 난이도가 높게 남은 태그가 그 사람의 약점 태그가 된다. 문제 목록 위의 추천은 약점 태그에서 적정 난이도의 문제를 고른다.
- AI(풀이 설명, 최적화 제안, 힌트)는 계정마다 하루 2번이다. 하루는 한국 시간 자정에 바뀐다.
- 브라우저 확장은 프로그래머스 문제 화면의 코드를 그대로 저장한다. 같은 문제에 풀이가 이미 있으면 덮어쓸지 먼저 묻는다.
- 탈퇴하면 풀이, 메모, 노트, AI 기록이 바로 지워진다. 커뮤니티 글과 댓글은 남기고 작성자만 비운다.

## 화면

시연 계정으로 찍었다. 닉네임은 모두 시연용이다(DP장인, 그래프탐험가 등). 01~14는 [`docs/capture-screens.sh`](docs/capture-screens.sh)가 시연 데이터를 넣고 자동으로 찍고, AI 결과와 브라우저 확장은 운영 사이트에서 직접 찍었다.

### 문제 찾기

![문제 목록](docs/screenshots/03-problems.png)

위는 약점 태그로 고른 추천 문제다. 아래 목록은 제목 검색, 출처, 티어 범위, 태그, 정렬을 조합해 거른다. 티어는 체감 난이도로 보정한 값이다.

![문제 상세](docs/screenshots/04-problem-detail.png)

공식 레벨은 3인데 체감 난이도 피드백이 쌓여 플래티넘 II로 올라간 문제다. 풀이가 있으면 "내 풀이 보기", 없으면 "이 문제 풀이 작성"이 뜬다.

### 풀이 남기기

![풀이 등록](docs/screenshots/07-solution-form.png)

언어별 문법 강조 편집기(CodeMirror). 편집기 묶음은 크기가 커서 이 화면에 들어올 때만 받는다([성능 개선 3](#3-첫-화면-로딩-꺼져-있던-압축과-한-파일에-모든-화면)).

![브라우저 확장으로 저장](docs/screenshots/17-extension-save.png)

프로그래머스 문제 화면 오른쪽의 `❯ 저장`을 누르면 지금 편집기의 코드와 언어를 읽어 codeXray에 저장한다. 문제는 주소로 찾는다.

<table>
  <tr>
    <td valign="top" width="50%"><img src="docs/screenshots/18-extension-login.png" width="300"><br>확장 로그인. 확장은 쿠키를 쓸 수 없어서 refresh 토큰을 <code>X-Refresh-Token</code> 헤더로 주고받는다.</td>
    <td valign="top" width="50%"><img src="docs/screenshots/02-login.png" width="300"><br>웹 로그인. 이메일 가입과 Google, 네이버 로그인. 웹의 refresh 토큰은 httpOnly 쿠키에 둔다.</td>
  </tr>
</table>

### AI에게 묻기

![AI 풀이 설명](docs/screenshots/15-ai-explain.png)

내 풀이(C++, 유니온 파인드)를 Claude가 문제 요약, 사용 알고리즘, 단계별 논리, 동작 예시, 복잡도 순서로 설명한다. 요청은 바로 잡 번호를 돌려받고, 결과는 화면이 잡 상태를 물어 가져온다([구조](#구조)).

![AI 힌트](docs/screenshots/16-ai-hint.png)

아직 못 푼 문제의 힌트. 정답 코드 없이 접근 방향, 핵심 아이디어, 구현 순서로 단계를 나눠 준다. 막힌 코드를 붙이면 그 코드에 맞춘 힌트를 준다.

### 복습하기

![풀이 상세](docs/screenshots/06-solution-detail.png)

풀이와 메모. 메모는 틀린 이유, 풀이 로직, 핵심 함수, 자유 메모 네 칸이다.

![노트](docs/screenshots/09-note-detail.png)

학습 노트는 마크다운이다. 코드 블록에는 복사 버튼이 붙고, 표도 그대로 그린다.

![프로필](docs/screenshots/14-profile.png)

푼 문제 수, 연속으로 푼 날, 다시 풀 문제(별표), 최근 활동, 티어·언어·태그 분포, 약점 태그.

### 같이 풀기

![커뮤니티 글](docs/screenshots/11-community-post.png)

질문과 풀이 공유 글. 추천과 비추천은 1인 1표, 댓글, 신고.

![건의사항](docs/screenshots/13-suggestion-post.png)

건의사항에는 처리 상태와 관리자 답변이 붙는다.

<details>
<summary>나머지 화면 (메인, 내 풀이, 노트 목록, 커뮤니티 목록, 건의사항 목록)</summary>

![메인](docs/screenshots/01-landing.png)
![내 풀이](docs/screenshots/05-solutions.png)
![노트 목록](docs/screenshots/08-notes.png)
![커뮤니티 목록](docs/screenshots/10-community.png)
![건의사항 목록](docs/screenshots/12-suggestions.png)

</details>

## 기술 스택

| | 쓴 것 | 왜 |
| --- | --- | --- |
| 백엔드 | Spring Boot 4.1, Java 21, JPA, QueryDSL | 검색어, 출처, 티어 범위, 태그, 정렬을 조합하는 문제 목록을 동적 쿼리로 |
| DB | PostgreSQL, Flyway | 테이블은 마이그레이션 파일(V1~V11)로만 바꾸고 Hibernate는 검증만 한다 |
| 캐시, 토큰 | Redis | refresh 토큰과 사용자별 토큰 목록, 문제 목록·상세·태그 캐시, 로그인 실패와 AI 하루 사용 횟수 |
| 이벤트 | Kafka (KRaft 한 대) | 수 초에서 수십 초 걸리는 Claude 호출을 요청 흐름에서 떼어 낸다 |
| AI | Anthropic Java SDK (Claude) | 풀이 설명, 최적화 제안, 힌트 |
| 화면 | React 19, Vite, TypeScript, TanStack Query, CodeMirror | |
| 확장 | Chrome 확장 (Manifest V3) | 프로그래머스 화면에서 바로 저장 |
| 배포 | Oracle Cloud Arm 서버 한 대, Docker Compose, GitHub Actions, GHCR | 다른 앱(Bandal)과 서버 한 대를 나눠 쓴다 |
| 측정 | k6, Prometheus, Grafana, Playwright | 같은 조건으로 다시 잴 수 있게 스크립트로 남긴다 |

## 구조

[![codeXray 아키텍처](docs/architecture.svg)](docs/architecture.png)

- 밖으로 열린 포트는 바깥 Nginx의 80, 443뿐이다. 바깥 Nginx와 같은 네트워크에 붙는 건 화면 컨테이너(frontend) 하나이고, backend와 PostgreSQL, Redis, Kafka는 `ports`가 없다.
- 화면 nginx가 정적 파일을 주고 `/api`만 backend로 넘긴다. 화면과 API가 같은 주소라 웹은 CORS가 필요 없다. 다른 출처는 브라우저 확장 하나뿐이라 그 확장의 ID만 허용한다.
- AI 요청은 잡을 먼저 PENDING으로 커밋하고 Kafka에 발행한 뒤 잡 번호를 바로 돌려준다. 컨슈머는 잡을 PENDING에서 PROCESSING으로 바꾼 쪽만 Claude를 부른다. 화면은 잡 상태를 2초마다 묻는다.
- Claude 호출은 트랜잭션 밖에서 한다. 응답을 기다리는 동안 DB 커넥션을 쥐고 있지 않는다.
- 컨테이너마다 메모리 상한을 둔다(backend 768MB에 JVM 힙은 그 75%, Kafka 힙 512MB). 같은 서버의 다른 앱이 메모리를 다 써도 서로 살아남는다.

## 측정 방법

조건을 글로만 남겼던 예전 측정은 다시 잴 때마다 절대값이 1.6~2.3배씩 달라졌다. 그래서 조건을 스크립트와 설정 파일로 고정하고, 원본 결과를 저장소에 그대로 남겼다([perf/README.md](perf/README.md)).

**어디서**
- 로컬 한 대(Ryzen 5 7500F 6코어, WSL2 15GB)다. 앱과 k6가 같은 기계에서 CPU를 나눠 쓰므로 절대값은 운영 서버의 성능이 아니다. 비교하는 것은 같은 조건에서의 단계 사이 배수다.
- 측정 전용 PostgreSQL, Redis, Prometheus, Grafana를 따로 띄운다(`perf/compose.yml`). 개발용 DB와 섞이지 않는다.

**문제 목록 API** (`perf/run-backend.sh`)
- 대상은 `GET /api/problems?page=0&size=50`(화면의 첫 진입과 같은 요청), 데이터는 문제 689개, 태그 25개다.
- k6 50 VU, 기다림 없이 포화 부하. 단계마다 JVM을 새로 띄우고 Redis를 비운 뒤 워밍업 30초(버림), 본 측정 30초 × 2의 중앙값.
- 세 단계는 지금 코드를 임시 worktree로 꺼내 패치로 만든다. 운영 코드에 측정용 스위치를 두지 않으려는 것이다.

**첫 화면 로딩** (`perf/run-frontend.sh`)
- Playwright(Chromium), 캐시 없음, 느린 4G 흉내(지연 150ms · 1.6Mbps/750kbps), CPU 4배 느리게, 5번의 중앙값.
- 같은 Dockerfile로 만든 화면 이미지를 단계별로 띄워 쟀다. 운영과 같은 HTTP/1.0 프록시도 앞에 세워 쟀다.

**숫자를 읽을 때 지킨 것**
- 처리량과 p95의 정본은 k6 요약이다(실행마다 요청 수 ÷ k6가 잰 실행 시간). Grafana 막대 중 p95와 요청당 SQL은 같은 값이 나오게 쿼리를 맞췄고, 처리량 막대는 길이만 보여 준다. Prometheus의 구간 값으로는 k6 요약을 똑같이 만들 수 없어서 숫자가 두 개로 보이지 않게 했다.
- 요청당 SQL은 앱이 센 값이다. Hibernate가 실행한 SQL 수 ÷ 요청 수, ①은 2,856,990 ÷ 136,047 = 21.00이다.
- SQL 로그를 켜 두면 SQL이 많은 ①만 크게 느려져 배수가 부풀려진다(2026-08 측정에서 약 3배). 측정 프로필에서는 끈다.

## 성능 개선

### 1. 문제 목록: 태그를 하나씩 읽던 N+1, 그 위에 캐시

<table>
  <tr>
    <th>처리량 (막대 길이 = 비율)</th>
    <th>p95 응답 시간</th>
  </tr>
  <tr>
    <td valign="top" width="50%"><img src="perf/results/20261101-0918/grafana/bar-throughput.png"><br>1,676 → 3,603 → 12,950 req/s</td>
    <td valign="top" width="50%"><img src="perf/results/20261101-0918/grafana/bar-p95.png"><br>55.0 → 27.3 → 8.0ms</td>
  </tr>
</table>

**증상**
문제 목록 한 페이지(50문제)에 SQL이 21개 나갔다. 그중 18개가 `select … from algorithm_tags where id=?`였다.

**원인**
목록 응답은 문제마다 태그 이름을 붙인다. 문제와 태그를 잇는 표(`problem_tags`)는 한 번에 읽었지만, 거기서 태그(`AlgorithmTag`)로 넘어가는 연관이 지연 로딩이라 서로 다른 태그 수만큼 하나씩 읽었다.
21 = 본문 1 + count 1 + 태그 연결 1 + 태그 개별 18(첫 페이지 50문제의 서로 다른 태그 수)이다.

**해결**
1. `AlgorithmTag`에 `@BatchSize(100)`을 달아 개별 조회 18번을 `id = ANY(?)` 한 번으로 줄였다. 요청당 SQL은 4가 됐고, 캐시가 비어 있어도 그대로다.
2. 그 위에 Redis 캐시(`@Cacheable(sync = true)`)를 얹었다. `sync`는 캐시가 비는 순간 같은 키의 요청이 몰려도 DB에는 한 번만 가게 한다.

**결과** (원본: [`perf/results/20261101-0918/`](perf/results/20261101-0918/summary.md))

| 단계 | 처리량 | p95 | 요청당 SQL | ① 대비 |
| --- | --- | --- | --- | --- |
| ① 개선 전 | 1,676 req/s | 55.0ms | 21 | 1.00× |
| ② `@BatchSize` | 3,603 req/s | 27.3ms | 4 | 2.15× |
| ③ ② + Redis 캐시 | 12,950 req/s | 8.0ms | 0 | **7.73×** |

![요청 1회당 SQL 수](perf/results/20261101-0918/grafana/ts-sql-per-request.png)

요청 1회당 SQL 21 → 4 → 0. ③의 0은 캐시가 적중해 DB에 가지 않은 것이고, 쿼리 최적화의 결과는 ②의 4다. 상한 4는 테스트(`ProblemListQueryCountTest`)로 고정했다.

**캐시를 비우는 기준**
- 문제 상세: 체감 난이도로 그 문제의 티어가 바뀔 때 그 키만 지운다.
- 문제 목록: 어떤 필터, 어떤 페이지에 그 문제가 있는지 알 수 없어 전체를 지우고, 30분 TTL을 안전망으로 둔다.
- 태그 목록: 바뀌지 않는 데이터라 TTL(1시간)만.
- 검색어가 있는 요청은 키가 사실상 끝없이 늘어나 캐시하지 않는다.

**남은 것**
③은 같은 요청을 반복한 측정이라 캐시 적중이 거의 100%다. 실제 서비스의 적중률은 재지 않았다.
지금 규모(689행)에서는 `EXPLAIN ANALYZE`로 PostgreSQL이 Seq Scan + Hash Join(0.2~0.5ms)을 고른다. 인덱스보다 왕복 횟수가 병목이라 그것부터 잡았고, 데이터가 커지면 인덱스를 다시 본다.

### 2. 풀이 목록: 문제와 메모를 따로 읽던 N+1

**증상**
내 풀이 목록 20개 한 페이지에 SQL이 72개 나갔다(고치기 전 코드로 같은 테스트를 돌려 센 값). 풀이마다 문제와 메모를 지연 로딩으로 따로 읽었다.

**해결**
목록 쿼리에서 문제와 메모를 `join fetch`로 함께 읽고, 페이지 수를 세는 count 쿼리는 따로 둬서(`countQuery`) fetch join과 페이지가 섞이지 않게 했다. 72 → 4 이하가 됐고, 상한 4를 테스트(`SolutionListQueryCountTest`)로 고정했다.
풀이, 노트, 커뮤니티, 건의사항 목록은 모두 페이지(기본 20, 최대 50)로 바꿨다. 커뮤니티 추천순은 추천 합계를 서브쿼리로 정렬해 글 수만큼 쿼리가 늘지 않는다.

### 3. 첫 화면 로딩: 꺼져 있던 압축과 한 파일에 모든 화면

**증상**
운영 사이트를 느린 회선 조건으로 재 보니 첫 화면이 그려지기까지 약 10초(FCP 9,916ms, JS 1,743KB)였다.

**원인**
1. 화면 nginx에 gzip이 꺼져 있어 1.7MB JS를 그대로 보냈다.
2. `App.tsx`가 모든 화면을 처음부터 import해서, 풀이 작성 화면에만 쓰는 편집기(CodeMirror와 언어 9개), 코드 강조, 마크다운까지 메인에서 받았다.

**해결**
화면 nginx에서 JS, CSS, JSON, SVG를 gzip으로 보내고, 메인을 뺀 화면을 `React.lazy`로 나눠 들어갈 때 받게 했다.

**결과** (원본: [`perf/results/20261101-0950-fcp/`](perf/results/20261101-0950-fcp/summary.md))

| 단계 | 메인 받은 JS | 메인 FCP | `/problems` LCP |
| --- | --- | --- | --- |
| 고치기 전 | 1,741KB | 9,785ms | 9,881ms |
| ① gzip | 570KB | 3,569ms | 3,665ms |
| ② gzip + 화면 나누기 | 111KB | **1,034ms** | 1,485ms |

**운영에서만 압축이 빠졌다**
배포 뒤 운영을 다시 재니 FCP가 2,502ms(JS 340KB)였다. 화면 나누기는 들어갔는데 압축이 빠진 값이다. 응답에 `Vary: Accept-Encoding`은 있는데 `Content-Encoding`이 없었다.
바깥 Nginx는 안쪽으로 HTTP/1.0으로 넘기고(`proxy_http_version` 기본값), nginx gzip은 기본으로 HTTP/1.1 요청에만 압축한다(`gzip_http_version 1.1`). 브라우저가 화면 컨테이너에 바로 붙는 로컬에서는 드러나지 않았다.
화면 nginx에 `gzip_http_version 1.0`을 넣었다. 운영과 같은 프록시를 앞에 세워 다시 재면 2,405ms → 1,040ms이고, 응답 헤더 원본은 [`headers.txt`](perf/results/20261101-0950-fcp/headers.txt)에 있다. 고친 뒤의 운영 사이트를 다시 잰 기록은 아직 없다.

**버린 대안**
바깥 Nginx에서 `proxy_http_version 1.1`로 넘기면 같은 서버의 앱이 한 번에 해결된다. 하지만 바깥 Nginx는 배포 워크플로가 올리지 않아 손으로 적용해야 하고, 앱의 응답이 앱 저장소 밖의 설정에 기대게 된다. 앱 저장소 안에서 끝나는 쪽을 골랐다.

## 동시성과 정합성

코드를 다시 읽으며 동시에 들어오는 요청에서 깨지는 곳을 찾아 고쳤다. 각각 테스트가 함께 들어갔다.

**Kafka가 같은 AI 요청을 두 번 주면 Claude가 두 번 불렸다.**
컨슈머가 잡을 읽고 바로 Claude를 불렀다. 재전달이나 재시도로 같은 메시지가 두 번 오면 비용도 두 번 나갔다.
잡의 상태를 조건부 UPDATE(`PENDING → PROCESSING`)로 가져가고, 바뀐 행이 1일 때만 부르게 했다(`AiJobTransitionTest`).
처리 중 실패는 1초 간격으로 2번 다시 시도한 뒤 `ai-jobs.DLT`로 보낸다. 15분 넘게 멈춘 잡은 5분마다 도는 정리 작업이 FAILED로 바꿔 화면의 폴링이 끝나게 한다.

**Kafka 발행이 실패하면 잡이 PENDING에 영원히 머물렀다.**
발행 결과를 보지 않았다. 브로커가 죽어 있으면 사용자는 끝나지 않는 로딩을 봤다.
발행이 실패하면 잡을 바로 FAILED로 바꾸고 하루 사용 횟수를 1번 돌려준다.

**같은 refresh 토큰으로 동시에 갱신하면 둘 다 새 토큰을 받았다.**
"토큰 확인 → 삭제 → 새 토큰 발급"이 나뉘어 있어 두 요청이 모두 확인을 통과할 수 있었다.
확인과 삭제를 Redis `GETDEL` 한 번으로 합쳤다. 같은 토큰으로 동시에 갱신하면 한쪽만 성공한다(`RefreshTokenStoreTest`).

**같은 문제에 체감 난이도가 동시에 들어오면 한쪽 계산이 사라졌다.**
두 요청이 각자 피드백 목록을 읽어 티어를 계산하고 덮어써서, 늦게 커밋한 쪽이 먼저 커밋한 피드백을 빼고 계산한 값을 남겼다(lost update).
문제 행을 `SELECT … FOR UPDATE`로 잠그고 읽은 뒤 계산한다.

**티어 경계의 문제가 아래 티어로 떨어졌다.**
보정 레벨을 3등분해 티어를 정하는데, 부동소수점 오차로 경계값(예: 1/3 지점)이 아래 칸으로 떨어졌다. 경계에 아주 가까운 값을 경계로 맞춘 뒤 나눈다(`RatingCalculatorTest`).

**캐시가 비는 순간 같은 목록 요청이 DB로 몰렸다.**
`@Cacheable(sync = true)`로 같은 키는 한 요청만 DB에 가고 나머지는 그 결과를 기다린다.

## 운영과 관측

**배포**
작업은 브랜치에서 하고 PR로 master에 합친다. CI에서 PostgreSQL 16, Redis 7 서비스 컨테이너로 백엔드 테스트를 돌리고, 화면은 lint와 타입 검사 빌드를 돈다. 하나라도 실패하면 이미지를 만들지 않는다.
통과하면 Arm 이미지를 만들어 GHCR에 커밋 SHA 태그로 올리고, SSH로 서버에 배포한다. Gradle, npm 빌드는 러너의 x86에서 돌리고 Arm 흉내(QEMU)는 마지막 실행 단계만 탄다. 새 버전이 healthy가 안 되면 마지막으로 성공한 SHA로 되돌리고 워크플로를 실패로 끝낸다.

**백업**
DB는 매일 새벽과 배포 직전에 백업한다. 빈 PostgreSQL에 실제로 복구해 행 수를 비교한 뒤에만 Cloudflare R2에 올린다. 성공 신호가 끊기면 healthchecks.io가 메일을 보낸다.

**보안**
- 관리자 API는 주소 규칙과 메서드마다 붙인 `@PreAuthorize` 두 겹으로 막는다. 주소 규칙을 실수로 바꿔도 열리지 않는다.
- 로그인은 이메일마다 15분에 5번 실패하면 잠긴다. 없는 이메일에도 같은 bcrypt 비용을 들여 응답 시간으로 가입 여부가 드러나지 않게 했다. 인증 메일 재발송은 1분 간격이고 가입 여부와 상관없이 같은 응답을 준다.
- 비밀번호를 바꾸거나 탈퇴하면 사용자별 토큰 목록(Redis Set)으로 모든 기기의 refresh 토큰을 지운다. 이미 나간 access 토큰(30분)은 요청마다 블랙리스트를 보는 비용 대신 만료까지 둔다.
- 정렬 키는 허용한 필드만 받고, 같은 값이면 `id desc`로 순서를 고정해 페이지가 넘어가도 글이 겹치거나 빠지지 않는다.
- 사용자 코드는 `<user_code>` 태그 안의 데이터로만 Claude에 넘기고, 코드 안의 닫는 태그는 이스케이프한다(`AiPromptTest`).
- 화면 응답에 `nosniff`, `X-Frame-Options: DENY`, `frame-ancestors 'none'`, `Referrer-Policy`, HSTS를 붙인다.

**화면을 찍다가 찾은 운영 버그**
포트폴리오용으로 운영 화면을 직접 찍다가 발견했다. 모니터링으로 잡은 것이 아니다.

| 증상 | 원인 | 고친 것 |
| --- | --- | --- |
| 확장 로그인이 운영에서 403 (첫 배포부터) | 확장의 요청에는 `Origin: chrome-extension://<ID>`가 붙는데 운영 CORS 허용 목록이 비어 있었다 | `manifest.json`에 공개키를 넣어 확장 ID를 고정하고 그 ID 하나만 허용. 모든 확장(`chrome-extension://*`)은 쿠키 인증까지 열리는 설정이라 쓰지 않았다 |
| 확장의 "이미 저장된 풀이" 확인이 깨짐 | 목록 API를 페이지 응답으로 바꿀 때 확장 코드를 함께 바꾸지 않았다 | 문제별 조회(`/api/solutions/by-problem/{id}`, 없으면 204)로 |
| AI 답변의 표가 칸 구분 없이 붙어 보임 | 마크다운 표에 스타일이 없었다 | 표 테두리, 머리글, 칸 여백 |
| AI 답변의 `**"…"**를`이 별표째 보임 | CommonMark는 닫는 `**` 앞이 문장부호이고 뒤가 글자면 굵게로 보지 않는다. 한국어는 조사가 바로 붙는다 | `remark-cjk-friendly` |

<details>
<summary>AWS EC2 두 대에서 오라클 서버 한 대로 옮긴 이유 (2026-10)</summary>

처음에는 AWS EC2 두 대(애플리케이션 / 데이터)에 배포하고 Watchtower로 자동 배포했다. SSH를 운영자 IP로 잠근 보안그룹에 IP가 매번 바뀌는 Actions 러너가 들어오려면 22번을 열어야 해서, 서버가 스스로 `:latest`를 확인하는 방식을 골랐다. EC2는 확인 뒤 내리고, 이미 운영 중이던 오라클 Arm 서버(다른 앱과 공유)로 옮기며 배포를 SSH로 바꿨다.

- 이 서버는 SSH를 키로만 열어 IP 제한 없이도 러너가 들어올 수 있다.
- Watchtower는 Docker 소켓(서버 root 권한)을 쥐어야 해서, 다른 앱과 같이 쓰는 서버에서는 위험이 크다.
- `:latest`는 지금 무엇이 떠 있는지 알 수 없고 되돌릴 기준도 없다. SHA 태그, 되돌리기, 배포 전 백업이 실제 운영에서 더 중요했다.
- 계층 분리(EC2 두 대)는 서버 한 대 안에서 Docker 네트워크 격리(저장소에 포트 없음)로 대신했다.

</details>

## 로컬에서 띄우기

```bash
cd backend
cp .env.example .env                        # DB 비밀번호, JWT_SECRET(32자 이상), ANTHROPIC_API_KEY 등
docker compose up -d                        # PostgreSQL 5432, Redis 6379, Kafka 9092
./gradlew bootRun                           # http://localhost:8080

cd ../frontend && npm install && npm run dev   # http://localhost:5173 (/api 는 8080 으로 프록시)
```

`ANTHROPIC_API_KEY`를 비워 두면 AI 요청만 실패(AI_UNAVAILABLE)로 끝나고 나머지는 그대로 돈다.

| 하고 싶은 것 | 명령 |
| --- | --- |
| 문제 목록 API 다시 재기 (약 12분) | `perf/run-backend.sh` → Grafana http://localhost:3300 |
| 첫 화면 로딩 다시 재기 | `perf/run-frontend.sh` |
| README 화면 다시 찍기 | `docs/capture-screens.sh` |

측정과 캡처 스크립트는 별도 컨테이너(`perf/compose.yml`)를 써서 개발 DB를 건드리지 않는다.

테스트는 `cd backend && ./gradlew test`(47개). 쿼리 수, 페이지, 탈퇴 정리, 토큰 동시성 테스트는 실제 PostgreSQL과 Redis가 필요하다.

| 무엇을 | 테스트 |
| --- | --- |
| 목록 쿼리 수 상한 | `ProblemListQueryCountTest`(4), `SolutionListQueryCountTest`(4, 고치기 전 72) |
| AI 잡 상태 전이, 프롬프트 이스케이프 | `AiJobTransitionTest`, `AiPromptTest` |
| refresh 토큰 회전 동시성, 사용자별 일괄 만료 | `RefreshTokenStoreTest` |
| 로그인 시도 제한 | `LoginAttemptLimiterTest` |
| 티어 계산과 경계값 | `RatingCalculatorTest` |
| 탈퇴 시 데이터 정리(FK) | `AiJobUserCascadeTest` |
| 커뮤니티 페이지와 추천순 | `CommunityPostPageTest` |
| 오류 응답 코드(400/401/404/500) | `GlobalExceptionHandlerTest` |

## 문서

| 파일 | 내용 |
| --- | --- |
| [perf/README.md](perf/README.md) | 성능 측정 방법, 단계를 만드는 패치, 결과 파일 읽는 법 |
| [perf/results/](perf/results/) | 측정 원본(k6 요약 JSON, 서버 지표, 실행 로그, Grafana 그림) |
| [extension/README.md](extension/README.md) | 브라우저 확장 설치, 확장 ID와 CORS |
| [deploy/](deploy/) | 서버에 올라가는 compose, 배포·되돌리기·백업 스크립트, 시연 데이터 |
| [docs/capture-screens.sh](docs/capture-screens.sh) | 이 문서의 화면을 시연 데이터로 다시 찍는 스크립트 |
