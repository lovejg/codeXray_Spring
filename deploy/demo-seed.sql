-- 시연(샘플) 데이터. demo-seed.sh 가 비밀번호 해시(:pw_hash)를 넘겨 부른다. 직접 부르지 않는다
--
-- 시연 계정 = 메일 도메인이 codexray-demo.example 인 계정 5개. .example 은 인터넷에서 쓸 수 없게 정해 둔 이름(RFC 2606)이라
-- 진짜 사용자와 겹치지 않고, 그 주소로 메일을 받을 수도 없다.
-- 몇 번을 돌려도 같은 모양이다: 시연 계정에 딸린 것(풀이, 메모, 피드백, 노트, 글, 댓글, 추천)만 지우고 다시 넣는다.
-- 진짜 사용자의 데이터는 건드리지 않는다. 다만 진짜 사용자가 시연 글에 단 댓글·추천은 그 글과 함께 지워진다.
--
-- 티어: 피드백이 바뀌므로 모든 문제의 티어를 비운다. demo-seed.sh 가 backend 를 다시 띄우면
-- TierBootstrap 이 앱의 계산식(RatingCalculator)으로 전체를 다시 채운다(SQL 로 계산식을 따로 두지 않는다)
\set ON_ERROR_STOP on
begin;

-- ===== 1. 지우기 =====
-- 글·댓글의 작성자 FK 는 ON DELETE SET NULL(탈퇴해도 글은 남김)이라 계정을 지우기 전에 직접 지운다.
-- 풀이(메모), 피드백, 노트(태그), 추천, 신고, 알림은 계정을 지우면 CASCADE 로 같이 지워진다
create temp table demo_users on commit drop as
  select id from users where email like '%@codexray-demo.example';
delete from comments where user_id in (select id from demo_users);
delete from community_posts where user_id in (select id from demo_users);
delete from ai_jobs where user_id in (select id from demo_users);
delete from email_verification_token where user_id in (select id from demo_users);
delete from users where id in (select id from demo_users);

-- ===== 2. 계정 =====
-- 비밀번호 로그인 계정(LOCAL), 메일 인증 끝. 모두 같은 비밀번호(DEMO_PASSWORD)
insert into users (email, password, nickname, role, email_verified, provider, created_at, updated_at) values
  ('demo1@codexray-demo.example', :pw_hash, '알고리즘새싹', 'USER', true, 'LOCAL', now() - interval '40 days', now() - interval '40 days'),
  ('demo2@codexray-demo.example', :pw_hash, 'DP장인',       'USER', true, 'LOCAL', now() - interval '38 days', now() - interval '38 days'),
  ('demo3@codexray-demo.example', :pw_hash, '그래프탐험가', 'USER', true, 'LOCAL', now() - interval '35 days', now() - interval '35 days'),
  ('demo4@codexray-demo.example', :pw_hash, '그리디한하루', 'USER', true, 'LOCAL', now() - interval '30 days', now() - interval '30 days'),
  ('demo5@codexray-demo.example', :pw_hash, '해시맵러버',   'USER', true, 'LOCAL', now() - interval '25 days', now() - interval '25 days');

-- demo<n> 계정의 id. 아래에서 pg_temp.u(1) 처럼 쓴다(이 세션에서만 있는 함수)
create function pg_temp.u(n int) returns bigint language sql as
  $$ select id from users where email = 'demo' || n || '@codexray-demo.example' $$;

-- ===== 3. 풀이 =====
-- 문제 id 는 문제 시드(V2)가 고정한 값이다(예: 554 타겟 넘버). 체감 난이도 피드백은 "풀이를 등록한 문제만" 남길 수 있어서
-- 피드백을 남길 문제는 모두 여기 풀이가 있다
create temp table demo_solutions (n int, problem_id bigint, language varchar, starred boolean, days int, code text) on commit drop;
insert into demo_solutions values
  -- demo1 알고리즘새싹: 입문 문제 위주, python
  (1, 591, 'python', true, 33, $c$def solution(participant, completion):
    count = {}
    for name in participant:
        count[name] = count.get(name, 0) + 1
    for name in completion:
        count[name] -= 1
    return next(name for name, c in count.items() if c > 0)$c$),
  (1, 578, 'python', false, 31, $c$def solution(array, commands):
    return [sorted(array[i - 1:j])[k - 1] for i, j, k in commands]$c$),
  (1, 576, 'python', false, 29, $c$def solution(answers):
    patterns = [[1, 2, 3, 4, 5], [2, 1, 2, 3, 2, 4, 2, 5], [3, 3, 1, 1, 2, 2, 4, 4, 5, 5]]
    scores = [sum(a == p[i % len(p)] for i, a in enumerate(answers)) for p in patterns]
    best = max(scores)
    return [i + 1 for i, s in enumerate(scores) if s == best]$c$),
  (1, 663, 'python', false, 27, $c$def solution(s):
    depth = 0
    for ch in s:
        depth += 1 if ch == '(' else -1
        if depth < 0:
            return False
    return depth == 0$c$),
  (1, 554, 'python', true, 20, $c$def solution(numbers, target):
    def dfs(i, total):
        if i == len(numbers):
            return 1 if total == target else 0
        return dfs(i + 1, total + numbers[i]) + dfs(i + 1, total - numbers[i])
    return dfs(0, 0)$c$),
  (1, 674, 'python', false, 15, $c$from collections import deque

def solution(maps):
    n, m = len(maps), len(maps[0])
    dist = [[-1] * m for _ in range(n)]
    dist[0][0] = 1
    q = deque([(0, 0)])
    while q:
        x, y = q.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < n and 0 <= ny < m and maps[nx][ny] == 1 and dist[nx][ny] == -1:
                dist[nx][ny] = dist[x][y] + 1
                q.append((nx, ny))
    return dist[n - 1][m - 1]$c$),
  -- demo2 DP장인: DP 위주, java
  (2, 558, 'java', true, 34, $c$class Solution {
    public int solution(int[][] triangle) {
        for (int i = triangle.length - 2; i >= 0; i--) {
            for (int j = 0; j <= i; j++) {
                triangle[i][j] += Math.max(triangle[i + 1][j], triangle[i + 1][j + 1]);
            }
        }
        return triangle[0][0];
    }
}$c$),
  (2, 559, 'java', false, 32, $c$class Solution {
    public int solution(int m, int n, int[][] puddles) {
        int MOD = 1_000_000_007;
        int[][] dp = new int[n + 1][m + 1];
        boolean[][] blocked = new boolean[n + 1][m + 1];
        for (int[] p : puddles) blocked[p[1]][p[0]] = true;
        dp[1][1] = 1;
        for (int y = 1; y <= n; y++) {
            for (int x = 1; x <= m; x++) {
                if (blocked[y][x] || (x == 1 && y == 1)) continue;
                dp[y][x] = (dp[y - 1][x] + dp[y][x - 1]) % MOD;
            }
        }
        return dp[n][m];
    }
}$c$),
  (2, 561, 'java', false, 26, $c$import java.util.*;

class Solution {
    public int solution(int N, int number) {
        List<Set<Integer>> dp = new ArrayList<>();
        for (int k = 1; k <= 8; k++) {
            Set<Integer> set = new HashSet<>();
            set.add(Integer.parseInt(String.valueOf(N).repeat(k)));
            for (int i = 1; i < k; i++) {
                for (int a : dp.get(i - 1)) for (int b : dp.get(k - i - 1)) {
                    set.add(a + b); set.add(a - b); set.add(a * b);
                    if (b != 0) set.add(a / b);
                }
            }
            if (set.contains(number)) return k;
            dp.add(set);
        }
        return -1;
    }
}$c$),
  (2, 560, 'java', true, 18, $c$class Solution {
    public int solution(int[] money) {
        int n = money.length;
        return Math.max(rob(money, 0, n - 2), rob(money, 1, n - 1));
    }

    private int rob(int[] money, int from, int to) {
        int take = 0, skip = 0;
        for (int i = from; i <= to; i++) {
            int next = Math.max(skip + money[i], take);
            skip = take;
            take = next;
        }
        return take;
    }
}$c$),
  (2, 552, 'java', false, 12, $c$class Solution {
    public long solution(int n, int[] times) {
        long lo = 1, hi = (long) java.util.Arrays.stream(times).max().getAsInt() * n;
        while (lo < hi) {
            long mid = (lo + hi) / 2, done = 0;
            for (int t : times) done += mid / t;
            if (done >= n) hi = mid; else lo = mid + 1;
        }
        return lo;
    }
}$c$),
  (2, 553, 'java', false, 6, $c$import java.util.*;

class Solution {
    public int solution(int distance, int[] rocks, int n) {
        Arrays.sort(rocks);
        int lo = 1, hi = distance, answer = 0;
        while (lo <= hi) {
            int mid = (lo + hi) / 2, removed = 0, prev = 0;
            for (int r : rocks) {
                if (r - prev < mid) removed++; else prev = r;
            }
            if (distance - prev < mid) removed++;
            if (removed <= n) { answer = mid; lo = mid + 1; } else hi = mid - 1;
        }
        return answer;
    }
}$c$),
  -- demo3 그래프탐험가: DFS/BFS 위주
  (3, 554, 'python', false, 30, $c$def solution(numbers, target):
    sums = {0: 1}
    for x in numbers:
        nxt = {}
        for s, c in sums.items():
            nxt[s + x] = nxt.get(s + x, 0) + c
            nxt[s - x] = nxt.get(s - x, 0) + c
        sums = nxt
    return sums.get(target, 0)$c$),
  (3, 674, 'python', true, 28, $c$from collections import deque

def solution(maps):
    n, m = len(maps), len(maps[0])
    q = deque([(0, 0, 1)])
    maps[0][0] = 0
    while q:
        x, y, d = q.popleft()
        if (x, y) == (n - 1, m - 1):
            return d
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < n and 0 <= ny < m and maps[nx][ny]:
                maps[nx][ny] = 0
                q.append((nx, ny, d + 1))
    return -1$c$),
  (3, 557, 'cpp', true, 24, $c$#include <vector>
using namespace std;

int parent[200];
int find(int x) { return parent[x] == x ? x : parent[x] = find(parent[x]); }

int solution(int n, vector<vector<int>> computers) {
    for (int i = 0; i < n; i++) parent[i] = i;
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            if (computers[i][j]) parent[find(i)] = find(j);
    int groups = 0;
    for (int i = 0; i < n; i++) if (find(i) == i) groups++;
    return groups;
}$c$),
  (3, 551, 'python', false, 19, $c$from collections import deque, defaultdict

def solution(n, edge):
    graph = defaultdict(list)
    for a, b in edge:
        graph[a].append(b)
        graph[b].append(a)
    dist = [-1] * (n + 1)
    dist[1] = 0
    q = deque([1])
    while q:
        v = q.popleft()
        for w in graph[v]:
            if dist[w] == -1:
                dist[w] = dist[v] + 1
                q.append(w)
    far = max(dist)
    return dist.count(far)$c$),
  (3, 555, 'python', false, 14, $c$from collections import defaultdict

def solution(tickets):
    graph = defaultdict(list)
    for a, b in sorted(tickets, reverse=True):
        graph[a].append(b)
    route, stack = [], ['ICN']
    while stack:
        while graph[stack[-1]]:
            stack.append(graph[stack[-1]].pop())
        route.append(stack.pop())
    return route[::-1]$c$),
  (3, 558, 'python', false, 9, $c$def solution(triangle):
    dp = triangle[-1][:]
    for row in reversed(triangle[:-1]):
        dp = [v + max(dp[i], dp[i + 1]) for i, v in enumerate(row)]
    return dp[0]$c$),
  -- demo4 그리디한하루: 그리디 위주
  (4, 572, 'python', false, 29, $c$def solution(n, lost, reserve):
    lost_set = set(lost) - set(reserve)
    reserve_set = set(reserve) - set(lost)
    for r in sorted(reserve_set):
        for want in (r - 1, r + 1):
            if want in lost_set:
                lost_set.remove(want)
                break
    return n - len(lost_set)$c$),
  (4, 574, 'python', false, 25, $c$def solution(name):
    vertical = sum(min(ord(c) - ord('A'), ord('Z') - ord(c) + 1) for c in name)
    n = len(name)
    move = n - 1
    for i in range(n):
        j = i + 1
        while j < n and name[j] == 'A':
            j += 1
        move = min(move, 2 * i + n - j, i + 2 * (n - j))
    return vertical + move$c$),
  (4, 571, 'cpp', true, 21, $c$#include <string>
using namespace std;

string solution(string number, int k) {
    string stack;
    for (char c : number) {
        while (k > 0 && !stack.empty() && stack.back() < c) { stack.pop_back(); k--; }
        stack.push_back(c);
    }
    stack.resize(stack.size() - k);
    return stack;
}$c$),
  (4, 570, 'python', false, 16, $c$def solution(routes):
    routes.sort(key=lambda r: r[1])
    cameras, last = 0, -30001
    for start, end in routes:
        if start > last:
            cameras += 1
            last = end
    return cameras$c$),
  (4, 569, 'python', false, 11, $c$def solution(people, limit):
    people.sort()
    i, j, boats = 0, len(people) - 1, 0
    while i <= j:
        if people[i] + people[j] <= limit:
            i += 1
        j -= 1
        boats += 1
    return boats$c$),
  (4, 591, 'python', false, 7, $c$from collections import Counter

def solution(participant, completion):
    return list((Counter(participant) - Counter(completion)).keys())[0]$c$),
  -- demo5 해시맵러버: 해시, 힙, 이분탐색
  (5, 590, 'python', true, 22, $c$def solution(phone_book):
    phone_book.sort()
    return not any(b.startswith(a) for a, b in zip(phone_book, phone_book[1:]))$c$),
  (5, 588, 'python', false, 20, $c$from collections import defaultdict

def solution(genres, plays):
    total = defaultdict(int)
    songs = defaultdict(list)
    for i, (g, p) in enumerate(zip(genres, plays)):
        total[g] += p
        songs[g].append((-p, i))
    answer = []
    for g in sorted(total, key=total.get, reverse=True):
        answer += [i for _, i in sorted(songs[g])[:2]]
    return answer$c$),
  (5, 583, 'python', false, 17, $c$import heapq

def solution(scoville, K):
    heapq.heapify(scoville)
    mixes = 0
    while scoville[0] < K:
        if len(scoville) < 2:
            return -1
        a, b = heapq.heappop(scoville), heapq.heappop(scoville)
        heapq.heappush(scoville, a + b * 2)
        mixes += 1
    return mixes$c$),
  (5, 552, 'python', false, 10, $c$def solution(n, times):
    lo, hi = 1, max(times) * n
    while lo < hi:
        mid = (lo + hi) // 2
        if sum(mid // t for t in times) >= n:
            hi = mid
        else:
            lo = mid + 1
    return lo$c$),
  (5, 554, 'python', false, 5, $c$from itertools import product

def solution(numbers, target):
    return sum(1 for signs in product((1, -1), repeat=len(numbers))
               if sum(s * x for s, x in zip(signs, numbers)) == target)$c$),
  (5, 561, 'python', false, 3, $c$def solution(N, number):
    dp = [set() for _ in range(9)]
    for k in range(1, 9):
        dp[k].add(int(str(N) * k))
        for i in range(1, k):
            for a in dp[i]:
                for b in dp[k - i]:
                    dp[k] |= {a + b, a - b, a * b}
                    if b:
                        dp[k].add(a // b)
        if number in dp[k]:
            return k
    return -1$c$);

insert into solutions (user_id, problem_id, code, language, starred, created_at, updated_at)
select pg_temp.u(n), problem_id, code, language, starred, now() - make_interval(days => days), now() - make_interval(days => days)
from demo_solutions;

-- 풀이별 메모(1:1). 풀이 화면의 "틀린 이유 / 로직 / 핵심 함수 / 자유 메모" 칸
insert into memos (solution_id, wrong_reason, logic, key_functions, free_note)
select s.id, m.wrong_reason, m.logic, m.key_functions, m.free_note
from (values
  (1, 591, '처음엔 리스트에서 remove 를 반복해 시간 초과가 났다', '이름별 개수를 세고 완주자만큼 빼면 하나 남는다', 'dict.get(key, 0)', '동명이인이 있어서 set 으로는 안 된다'),
  (1, 554, null, '모든 부호 조합을 DFS 로 다 본다(2^20)', '재귀', 'BFS 로도 되지만 DFS 가 더 짧다'),
  (2, 560, '첫 집과 마지막 집이 이웃이라는 조건을 놓쳤다', '첫 집을 터는 경우 / 안 터는 경우로 나눠 선형 DP 두 번', 'Math.max', '원형 배열은 경우를 나눠 선형으로 만든다'),
  (2, 553, '이분탐색 대상을 "바위 위치"로 잡았다가 헤맸다', '최솟값의 최댓값 → 거리 mid 를 정하고 지워야 하는 바위 수를 센다', 'Arrays.sort', '파라메트릭 서치'),
  (3, 557, null, '연결된 컴퓨터끼리 합친 뒤 대표 노드 수를 센다', 'find(경로 압축)', 'DFS 풀이보다 코드가 짧다'),
  (4, 574, '좌우 이동을 무조건 오른쪽으로만 계산했다', '연속된 A 구간을 건너뛰는 세 가지 이동을 비교한다', 'ord', '그리디처럼 보이지만 사실 경우 비교'),
  (5, 552, null, '시간 mid 동안 심사할 수 있는 사람 수가 n 이상인 최소 mid', 'sum(mid // t)', 'hi 를 max(times) * n 으로')
) as m(n, problem_id, wrong_reason, logic, key_functions, free_note)
join solutions s on s.user_id = pg_temp.u(m.n) and s.problem_id = m.problem_id;

-- ===== 4. 체감 난이도 피드백(0~5) =====
-- 풀이가 있는 문제에만. 같은 문제에 여러 사람이 남겨서 보정 티어가 원본 레벨에서 움직이는 게 보인다
-- (예: N으로 표현 — 원본 Lv3, 정답률 31% → 시연 피드백 5, 4)
insert into level_feedback (user_id, problem_id, level, created_at, updated_at)
select pg_temp.u(n), problem_id, level, now() - make_interval(days => days), now() - make_interval(days => days)
from (values
  (1, 591, 1, 32), (1, 578, 1, 30), (1, 576, 1, 28), (1, 663, 2, 26), (1, 554, 3, 19), (1, 674, 3, 14),
  (2, 558, 2, 33), (2, 559, 3, 31), (2, 561, 5, 25), (2, 560, 4, 17), (2, 552, 3, 11), (2, 553, 5, 5),
  (3, 554, 2, 29), (3, 674, 2, 27), (3, 557, 2, 23), (3, 551, 3, 18), (3, 555, 4, 13), (3, 558, 2, 8),
  (4, 572, 2, 28), (4, 574, 4, 24), (4, 571, 3, 20), (4, 570, 3, 15), (4, 569, 2, 10), (4, 591, 1, 6),
  (5, 590, 2, 21), (5, 588, 3, 19), (5, 583, 2, 16), (5, 552, 4, 9), (5, 554, 2, 4), (5, 561, 4, 2)
) as f(n, problem_id, level, days);

-- ===== 5. 학습 노트 =====
create temp table demo_notes (n int, type varchar, title varchar, body text, language varchar, days int, tags text[]) on commit drop;
insert into demo_notes values
  (2, 'PATTERN', '원형 배열 DP는 경우를 둘로 나눈다', '첫 원소를 쓰는 경우(마지막 제외)와 안 쓰는 경우(첫 원소 제외)로 나눠 선형 DP 두 번을 돌리고 큰 값을 고른다. 예: 도둑질', null, 17, array['DP', '원형']),
  (2, 'CODE', '이분탐색 템플릿 (조건을 만족하는 최솟값)', $n$"이 값이면 되는가"가 단조(작으면 안 되다가 어느 순간부터 계속 됨)일 때 쓴다. 입국심사, 징검다리.

```java
long lo = 1, hi = maxAnswer;
while (lo < hi) {
    long mid = (lo + hi) / 2;
    if (possible(mid)) hi = mid;   // mid 로 되면 더 작은 답을 찾는다
    else lo = mid + 1;
}
return lo;
```

| 문제 | 답(mid) | possible(mid) |
|---|---|---|
| 입국심사 | 걸리는 시간 | `sum(mid / t) >= n` |
| 징검다리 | 바위 사이 최소 거리 | 지워야 할 바위 수 `<= n` |

- `hi = mid` 쪽이므로 `mid` 는 내림. 올림으로 하면 무한 루프
- 범위가 10^9 을 넘을 수 있으면 `long`$n$, 'java', 6, array['이분탐색', '템플릿']),
  (2, 'MISTAKE', 'int 곱셈 오버플로', '입국심사에서 mid * n 을 int 로 계산해 큰 입력에서 음수가 나왔다. 범위가 10^9 을 넘을 수 있으면 처음부터 long 으로 둔다', null, 11, array['실수', '자료형']),
  (3, 'CODE', 'BFS 템플릿 (격자)', $n$```python
from collections import deque
q = deque([(sx, sy)])
dist[sx][sy] = 0
while q:
    x, y = q.popleft()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if in_range(nx, ny) and dist[nx][ny] == -1:
            dist[nx][ny] = dist[x][y] + 1
            q.append((nx, ny))
```$n$, 'python', 27, array['BFS', '템플릿']),
  (4, 'MISTAKE', '그리디인 줄 알았는데 경우 비교였던 문제', '조이스틱: "가까운 쪽으로 이동"만 하면 틀린다. 연속된 A 구간을 기준으로 오른쪽 왕복, 왼쪽 왕복, 그냥 오른쪽 셋을 다 비교해야 한다', null, 24, array['그리디', '반례']),
  (5, 'PATTERN', '최솟값의 최댓값 = 파라메트릭 서치', '답(mid)을 정해 두고 "이 답이 가능한가"를 O(n)에 판정할 수 있으면 이분탐색. 입국심사, 징검다리', null, 9, array['이분탐색']),
  (1, 'OTHER', '코딩테스트 준비 루틴', '하루 2문제: 하나는 푼 유형 복습, 하나는 새 유형. 막히면 30분 뒤 힌트 보기', null, 12, array['루틴']);

insert into notes (user_id, type, title, body, language, created_at, updated_at)
select pg_temp.u(n), type, title, body, language, now() - make_interval(days => days), now() - make_interval(days => days)
from demo_notes;

insert into note_tags (note_id, tag)
select nt.id, unnest(d.tags)
from demo_notes d join notes nt on nt.user_id = pg_temp.u(d.n) and nt.title = d.title;

-- ===== 6. 커뮤니티 글과 건의사항 =====
-- 커뮤니티: QUESTION, SOLUTION_SHARE(상태 없음). 건의사항: FEEDBACK, BUG_REPORT, FEATURE_REQUEST(상태, 관리자 답변 가능)
create temp table demo_posts (n int, problem_id bigint, type varchar, title varchar, content text,
                              status varchar, admin_reply text, reply_days int, days int) on commit drop;
insert into demo_posts values
  (1, 674, 'QUESTION', 'BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?',
   $p$게임 맵 최단거리를 풀다가 궁금해졌어요. 큐에서 **꺼낼 때** 방문 체크를 했더니 효율성 테스트에서 시간 초과가 났고, **넣을 때** 체크하니까 통과했습니다.

둘 다 결과는 같은데 왜 속도 차이가 이렇게 나나요?$p$, null, null, null, 14),
  (4, 574, 'QUESTION', '조이스틱 문제는 그리디로 풀면 안 되나요?',
   '매번 가까운 쪽의 A 아닌 글자로 이동하는 그리디로 풀었는데 테스트 11번이 틀립니다. 반례가 있을까요?', null, null, null, 22),
  (5, null, 'QUESTION', '해시 문제인데 정렬로 풀어도 될까요?',
   $p$전화번호 목록을 정렬 후 이웃끼리만 비교하는 방식으로 풀었어요. 분류는 해시인데, 정렬 풀이도 면접에서 괜찮은 답일까요?

```python
phone_book.sort()
return not any(b.startswith(a) for a, b in zip(phone_book, phone_book[1:]))
```$p$, null, null, null, 20),
  (2, 558, 'SOLUTION_SHARE', '정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다',
   $p$위에서 아래로 내려가면 양 끝 원소의 부모가 하나뿐이라 분기가 필요합니다. 맨 아랫줄부터 올라오면 모든 칸이 자식 두 개 중 큰 값만 더하면 돼서 코드가 훨씬 짧아져요.

```java
for (int i = triangle.length - 2; i >= 0; i--)
    for (int j = 0; j <= i; j++)
        triangle[i][j] += Math.max(triangle[i + 1][j], triangle[i + 1][j + 1]);
return triangle[0][0];
```$p$, null, null, null, 33),
  (3, 557, 'SOLUTION_SHARE', '네트워크 문제를 유니온 파인드로 풀어 봤어요',
   'DFS로 연결 요소를 세는 게 정석이지만, 유니온 파인드로 합친 뒤 대표 노드 수를 세도 됩니다. 경로 압축만 넣으면 n=200에서 충분히 빠릅니다.', null, null, null, 23),
  (2, 561, 'FEEDBACK', '"N으로 표현" 티어가 체감보다 낮은 것 같아요',
   '공식 레벨 3이지만 정답률이 31%이고, 사칙연산 조합을 집합 DP로 쌓는 아이디어가 꽤 어렵습니다. 플래티넘 정도가 맞지 않을까요?',
   'RESOLVED', '의견 감사합니다! 체감 난이도 피드백이 쌓이면 다음 재계산(매주 일요일)부터 티어에 반영됩니다. 지금은 피드백이 반영되어 PLATINUM 구간으로 올라갔어요.', 20, 24),
  (5, 553, 'FEEDBACK', '"징검다리"에 그리디 태그가 맞을까요?',
   '징검다리에 이분탐색과 그리디 태그가 같이 붙어 있는데, 풀이의 핵심은 파라메트릭 서치(이분탐색)이고 판정 부분만 그리디처럼 보이는 것 같아요. 그리디 태그를 보고 들어오면 헷갈릴 것 같습니다.',
   'IN_PROGRESS', '확인했습니다. 태그 분류를 다시 볼 때 함께 검토하겠습니다.', 3, 4),
  (1, null, 'BUG_REPORT', '모바일에서 코드 에디터가 화면 밖으로 넘쳐요',
   '아이폰 사파리에서 풀이 등록 화면을 열면 코드 에디터 오른쪽이 잘립니다. 가로 스크롤도 안 됩니다.',
   'IN_PROGRESS', '제보 감사합니다. 모바일 레이아웃을 손보는 중입니다.', 1, 2),
  (3, null, 'FEATURE_REQUEST', '내 풀이를 언어별로 모아 보고 싶어요',
   '파이썬과 C++을 같이 쓰는데, 내 풀이 목록을 언어로 거를 수 있으면 좋겠습니다.', null, null, null, 6),
  (4, null, 'FEATURE_REQUEST', '같은 문제의 다른 사람 풀이를 보고 싶어요',
   '내가 푼 문제에 한해서 다른 사람 풀이를 볼 수 있으면 복습에 도움이 될 것 같아요. 풀이 공유 글로만 보기엔 찾기가 어렵습니다.',
   'IN_PROGRESS', '좋은 의견 감사합니다. 공개 범위(내가 푼 문제만 열기)와 공개 여부 설정을 어떻게 둘지 정리한 뒤 반영하겠습니다.', 10, 15);

insert into community_posts (user_id, problem_id, type, title, content, is_private, hidden, status, admin_reply, admin_reply_at, created_at, updated_at)
select pg_temp.u(n), problem_id, type, title, content, false, false, status, admin_reply,
       case when reply_days is null then null else now() - make_interval(days => reply_days) end,
       now() - make_interval(days => days), now() - make_interval(days => coalesce(reply_days, days))
from demo_posts;

-- 시연 글 id(제목으로). 아래 댓글, 추천에서 쓴다
create function pg_temp.p(t varchar) returns bigint language sql as
  $$ select id from community_posts where title = t and user_id in (select id from users where email like '%@codexray-demo.example') $$;

insert into comments (post_id, user_id, content, created_at, updated_at)
select pg_temp.p(t), pg_temp.u(n), content, now() - make_interval(days => days, hours => hours), now() - make_interval(days => days, hours => hours)
from (values
  ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 3,
   '꺼낼 때 체크하면 같은 칸이 큐에 여러 번 들어가요. 큐가 칸 수보다 훨씬 커져서 느려지는 거예요. 넣을 때 체크하면 칸마다 한 번만 들어갑니다.', 14, 3),
  ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 2,
   '다익스트라는 반대로 꺼낼 때 확정하는 게 맞으니 헷갈리지 않게 구분해 두면 좋아요!', 13, 20),
  ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 1,
   '아하 큐 크기 때문이었군요. 감사합니다 🙏', 13, 10),
  ('조이스틱 문제는 그리디로 풀면 안 되나요?', 2,
   '"ABAAAAAAABA" 같은 경우 가까운 쪽만 보면 손해예요. 연속된 A 구간을 기준으로 왕복 경로를 비교해야 합니다.', 21, 5),
  ('해시 문제인데 정렬로 풀어도 될까요?', 4,
   '정렬하면 접두어 관계인 번호가 반드시 붙어 있게 되니까 맞는 풀이예요. O(n log n)이라 해시(O(n·L))와 비교해서 말하면 좋은 답이 될 것 같아요.', 19, 8),
  ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 1,
   '이거 보고 다시 풀었더니 코드가 반으로 줄었어요!', 32, 2),
  ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 3,
   '배열을 그대로 덮어써서 메모리도 추가로 안 쓰네요. 좋은 팁입니다.', 30, 11),
  ('네트워크 문제를 유니온 파인드로 풀어 봤어요', 5,
   '랭크 기반 합치기까지 넣으면 더 안정적이에요. 이 문제 크기에선 경로 압축만으로 충분하지만요.', 22, 4),
  ('"N으로 표현" 티어가 체감보다 낮은 것 같아요', 5,
   '저도 동의해요. 집합으로 쌓는 아이디어를 떠올리기까지 한참 걸렸어요.', 23, 6),
  ('내 풀이를 언어별로 모아 보고 싶어요', 2,
   '+1 저도 자바랑 파이썬 섞어서 써요', 5, 9)
) as c(t, n, content, days, hours);

-- 추천(+1) / 비추천(-1). 1인 1표
insert into post_votes (user_id, post_id, value, created_at, updated_at)
select pg_temp.u(n), pg_temp.p(t), value, now() - make_interval(days => days), now() - make_interval(days => days)
from (values
  ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 2, 1, 13), ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 3, 1, 13),
  ('BFS에서 방문 체크는 큐에 넣을 때 하나요, 뺄 때 하나요?', 4, 1, 12),
  ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 1, 1, 32), ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 3, 1, 30),
  ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 4, 1, 29), ('정수 삼각형: 아래에서 위로 올라가면 경계 처리가 사라집니다', 5, 1, 28),
  ('네트워크 문제를 유니온 파인드로 풀어 봤어요', 2, 1, 22), ('네트워크 문제를 유니온 파인드로 풀어 봤어요', 5, 1, 22),
  ('해시 문제인데 정렬로 풀어도 될까요?', 4, 1, 19), ('해시 문제인데 정렬로 풀어도 될까요?', 1, -1, 18),
  ('조이스틱 문제는 그리디로 풀면 안 되나요?', 1, 1, 21),
  ('"N으로 표현" 티어가 체감보다 낮은 것 같아요', 5, 1, 23), ('"N으로 표현" 티어가 체감보다 낮은 것 같아요', 3, 1, 22),
  ('내 풀이를 언어별로 모아 보고 싶어요', 2, 1, 5), ('내 풀이를 언어별로 모아 보고 싶어요', 1, 1, 4),
  ('같은 문제의 다른 사람 풀이를 보고 싶어요', 3, 1, 14), ('같은 문제의 다른 사람 풀이를 보고 싶어요', 5, 1, 13)
) as v(t, n, value, days);

-- ===== 7. 티어 비우기 =====
-- 피드백이 바뀌었으니 다시 계산해야 한다. 비워 두면 backend 가 뜰 때 TierBootstrap 이 전체를 다시 채운다
update problems set tier = null, adjusted_level = null;

commit;

select (select count(*) from users where email like '%@codexray-demo.example') || ' 계정, '
    || (select count(*) from solutions where user_id in (select id from users where email like '%@codexray-demo.example')) || ' 풀이, '
    || (select count(*) from level_feedback where user_id in (select id from users where email like '%@codexray-demo.example')) || ' 피드백, '
    || (select count(*) from community_posts where user_id in (select id from users where email like '%@codexray-demo.example')) || ' 글, '
    || (select count(*) from comments where user_id in (select id from users where email like '%@codexray-demo.example')) || ' 댓글' as "시연 데이터";
