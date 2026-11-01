// 서비스 화면 캡처. docs/capture-screens.sh 가 Playwright 도커 이미지 안에서 부른다
//   node shoot-app.mjs <앱 주소> <저장 폴더>
// 환경변수: DEMO_EMAIL, DEMO_PASSWORD, SOL_ID, NOTE_ID, POST_ID, SUGG_ID, PROBLEM_ID
//
// 화면 전체(1440px, 양옆 여백 포함)를 찍으면 README 표 칸에 줄여 넣었을 때 글자를 읽을 수 없었다.
// 그래서 본문 영역만 잘라 찍는다: 목록 화면은 본문 폭(약 1100px), 상세 화면은 가운데 열(768px), 높이도 화면마다 제한한다
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
  // 높이를 넉넉히 두어 잘라 낼 부분이 한 화면 안에 들어오게 한다
  viewport: { width: 1200, height: 1600 },
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
// 화면 아래에 붙은 상태바(장식)는 잘라 낸 사진에 끼어들지 않게 숨긴다
const CAPTURE_CSS = "body { font-family: 'Noto Sans KR', sans-serif; } footer.fixed { display: none !important; }";

// 화면이 다 그려질 때까지: 네트워크가 잠잠해지고, 로딩 표시가 사라지고, 글꼴까지 들어온 뒤 조금 더 기다린다
async function settle() {
  await page.addStyleTag({ url: KR_FONT }).catch(() => {});
  await page.addStyleTag({ content: CAPTURE_CSS });
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForFunction(() => !document.querySelector('.animate-pulse, .animate-spin'), null, { timeout: 10000 }).catch(() => {});
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(600);
}

const PAD = 20; // 잘라 낸 영역 둘레 여백(px)

// 잘라 낼 영역. width: 'wide'(본문 전체 폭) | 'narrow'(가운데 768px 열), from: 이 글자가 있는 곳부터, maxH: 최대 높이
async function region({ width = 'wide', from, maxH = 900 }) {
  const box = await page.evaluate(() => {
    const m = document.querySelector('main');
    const r = m.getBoundingClientRect();
    const cs = getComputedStyle(m);
    const left = r.left + parseFloat(cs.paddingLeft);
    const right = r.right - parseFloat(cs.paddingRight);
    // 내용이 실제로 끝나는 곳: main 안 요소들의 가장 아래
    let bottom = r.top;
    for (const el of m.querySelectorAll('*')) {
      const b = el.getBoundingClientRect();
      if (b.height > 0 && b.width > 0) bottom = Math.max(bottom, b.bottom);
    }
    return { left, right, top: r.top + parseFloat(cs.paddingTop), bottom };
  });
  let top = box.top;
  if (from) {
    const fb = await page.getByText(from, { exact: false }).first().boundingBox();
    if (fb) top = fb.y;
  }
  let x = box.left, w = box.right - box.left;
  if (width === 'narrow') {
    const mid = (box.left + box.right) / 2;
    x = mid - 384; w = 768;
  }
  const h = Math.min(maxH, box.bottom - top);
  return { x: x - PAD, y: top - PAD, width: w + PAD * 2, height: h + PAD * 2 };
}

async function shot(name, path, opts = {}) {
  await page.goto(base + path);
  await settle();
  if (opts.prepare) {
    await opts.prepare();
    await settle();
  }
  await page.mouse.move(0, 0); // hover 상태가 찍히지 않게
  let clip = opts.clip;
  if (!clip && opts.selector) {
    // 공통 레이아웃(<main>) 밖의 화면(로그인)은 지정한 요소를 기준으로 둘레 여백을 넉넉히 준다
    const b = await page.locator(opts.selector).first().boundingBox();
    const p = 48;
    clip = { x: b.x - p, y: b.y - p, width: b.width + p * 2, height: b.height + p * 2 };
  }
  clip ??= await region(opts);
  await page.screenshot({ path: `${outDir}/${name}.png`, clip, fullPage: true });
  console.log(`  ${name}.png  ←  ${path}  (${Math.round(clip.width)}×${Math.round(clip.height)})`);
}

// 로그인 전 화면
await shot('01-landing', '/', { clip: { x: 0, y: 0, width: 1200, height: 520 } });
await shot('02-login', '/login', { selector: 'form >> xpath=../..' });

// 시연 계정으로 로그인. refresh 토큰이 httpOnly 쿠키로 남아 이후 page.goto 로 새로 열어도 로그인이 이어진다
await page.fill('input[type=email]', env('DEMO_EMAIL'));
await page.fill('input[type=password]', env('DEMO_PASSWORD'));
const [res] = await Promise.all([
  page.waitForResponse((r) => r.url().includes('/api/auth/login')),
  page.click('button[type=submit]'),
]);
if (!res.ok()) throw new Error(`로그인 실패: ${res.status()} ${await res.text()}`);
await page.waitForURL((u) => !u.pathname.startsWith('/login'), { timeout: 15000 });

await shot('03-problems', '/problems', { maxH: 760 });
await shot('04-problem-detail', `/problems/${env('PROBLEM_ID')}`, { width: 'narrow' });
await shot('05-solutions', '/solutions', { maxH: 560 });
await shot('06-solution-detail', `/solutions/${env('SOL_ID')}`, { width: 'narrow', maxH: 1000 });
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
  width: 'narrow',
  maxH: 880,
  prepare: async () => {
    await page.selectOption('select', { label: 'Java' }).catch(() => {});
    await page.click('.cm-content');
    await page.keyboard.insertText(draft);
  },
});
await shot('08-notes', '/notes', { maxH: 640 });
await shot('09-note-detail', `/notes/${env('NOTE_ID')}`, { width: 'narrow' });
await shot('10-community', '/community', { maxH: 760 });
await shot('11-community-post', `/community/${env('POST_ID')}`, { width: 'narrow', maxH: 1000 });
await shot('12-suggestions', '/suggestions', { maxH: 760 });
await shot('13-suggestion-post', `/suggestions/${env('SUGG_ID')}`, { width: 'narrow' });
// 프로필은 계정 정보 칸(이메일·닉네임 변경)을 빼고 학습 대시보드부터
await shot('14-profile', '/profile', { from: '내 활동', maxH: 1100 });

await browser.close();
