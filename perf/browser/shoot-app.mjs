// 서비스 화면 캡처. docs/capture-screens.sh 가 Playwright 도커 이미지 안에서 부른다
//   node shoot-app.mjs <앱 주소> <저장 폴더>
// 환경변수: DEMO_EMAIL, DEMO_PASSWORD, SOL_ID, NOTE_ID, POST_ID, SUGG_ID, PROBLEM_ID
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';

const [base, outDir] = process.argv.slice(2);
const env = (k) => {
  const v = process.env[k];
  if (!v) throw new Error(`${k} 가 없습니다`);
  return v;
};
mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch();
const ctx = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2,
  colorScheme: 'dark',
  locale: 'ko-KR',
  timezoneId: 'Asia/Seoul',
});
const page = await ctx.newPage();
page.on('pageerror', (e) => console.log('  [화면 오류]', e.message));

// Playwright 도커 이미지의 system-ui 는 한글을 보통 굵기만 있는 글꼴(WenQuanYi)로 그려서 <strong> 한글이 굵게 안 보인다.
// 캡처할 때만 본문 글꼴을 앱 CSS 목록(index.css)에 이미 있는 Noto Sans KR(웹 글꼴)로 둔다. 사용자 PC 에서는 OS 글꼴로 정상이다
const KR_FONT = 'https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;600;700&display=block';

// 화면이 다 그려질 때까지: 네트워크가 잠잠해지고, 로딩 표시가 사라지고, 글꼴까지 들어온 뒤 조금 더 기다린다
async function settle() {
  await page.addStyleTag({ url: KR_FONT }).catch(() => {});
  await page.addStyleTag({ content: "body { font-family: 'Noto Sans KR', sans-serif; }" });
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForFunction(() => !document.querySelector('.animate-pulse, .animate-spin'), null, { timeout: 10000 }).catch(() => {});
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(600);
}

async function shot(name, path, { full = false, prepare } = {}) {
  await page.goto(base + path);
  await settle();
  if (prepare) {
    await prepare();
    await settle();
  }
  await page.mouse.move(0, 0); // hover 상태가 찍히지 않게
  // fullPage 로 찍으면 화면 아래에 붙은 상태바(position: fixed)가 첫 화면 높이에 그대로 남아 본문 가운데에 겹친다.
  // 대신 창 높이를 문서 높이만큼 늘려서 찍는다
  const vp = page.viewportSize();
  if (full) {
    const h = await page.evaluate(() => document.documentElement.scrollHeight);
    if (h > vp.height) {
      await page.setViewportSize({ width: vp.width, height: h });
      await page.waitForTimeout(300);
    }
  }
  await page.screenshot({ path: `${outDir}/${name}.png` });
  await page.setViewportSize(vp);
  console.log(`  ${name}.png  ←  ${path}`);
}

// 로그인 전 화면
await shot('01-landing', '/');
await shot('02-login', '/login');

// 시연 계정으로 로그인. refresh 토큰이 httpOnly 쿠키로 남아 이후 page.goto 로 새로 열어도 로그인이 이어진다
await page.fill('input[type=email]', env('DEMO_EMAIL'));
await page.fill('input[type=password]', env('DEMO_PASSWORD'));
const [res] = await Promise.all([
  page.waitForResponse((r) => r.url().includes('/api/auth/login')),
  page.click('button[type=submit]'),
]);
if (!res.ok()) throw new Error(`로그인 실패: ${res.status()} ${await res.text()}`);
await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15000 });

await shot('03-problems', '/problems');
await shot('04-problem-detail', `/problems/${env('PROBLEM_ID')}`, { full: true });
await shot('05-solutions', '/solutions');
await shot('06-solution-detail', `/solutions/${env('SOL_ID')}`, { full: true });
// 풀이 등록: 빈 편집기 대신 작성 중인 모습으로(저장은 하지 않는다)
const draft = `import java.util.*;

class Solution {
    public int solution(int N, int number) {
        List<Set<Integer>> dp = new ArrayList<>();
        for (int k = 0; k <= 8; k++) dp.add(new HashSet<>());
        for (int k = 1; k <= 8; k++) {
            dp.get(k).add(Integer.parseInt(String.valueOf(N).repeat(k)));
            for (int i = 1; i < k; i++)
                for (int a : dp.get(i))
                    for (int b : dp.get(k - i)) {
                        dp.get(k).add(a + b);
                        dp.get(k).add(a - b);
                        dp.get(k).add(a * b);
                        if (b != 0) dp.get(k).add(a / b);
                    }
            if (dp.get(k).contains(number)) return k;
        }
        return -1;
    }
}`;
await shot('07-solution-form', `/solutions/new?problemId=${env('PROBLEM_ID')}`, {
  prepare: async () => {
    await page.selectOption('select', { label: 'Java' }).catch(() => {});
    await page.click('.cm-content');
    await page.keyboard.insertText(draft);
    await page.locator('summary', { hasText: '메모' }).click().catch(() => {});
  },
});
await shot('08-notes', '/notes');
await shot('09-note-detail', `/notes/${env('NOTE_ID')}`);
await shot('10-community', '/community');
await shot('11-community-post', `/community/${env('POST_ID')}`, { full: true });
await shot('12-suggestions', '/suggestions');
await shot('13-suggestion-post', `/suggestions/${env('SUGG_ID')}`, { full: true });
await shot('14-profile', '/profile', { full: true });

await browser.close();
