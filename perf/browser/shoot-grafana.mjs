// Grafana 대시보드를 이미지로 남긴다(슬라이드·README 용).
//
//   node shoot-grafana.mjs <grafana-url.txt 의 주소> <저장 폴더>
//
// 전체 대시보드 한 장 + 패널마다 한 장(d-solo). Grafana 이미지 렌더러 플러그인 없이 브라우저로 찍는다
import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'

const [, , dashUrl, outDir] = process.argv
mkdirSync(outDir, { recursive: true })
const u = new URL(dashUrl)
const range = `from=${u.searchParams.get('from')}&to=${u.searchParams.get('to')}`
const base = `${u.origin}`

// 패널 id 와 파일 이름. 대시보드 생성기(perf/grafana/dashboards/codexray-perf.json)의 순서와 같다
const PANELS = [
  [1, 'bar-throughput'], [2, 'bar-p95'], [3, 'bar-sql-per-request'],
  [4, 'ts-throughput'], [5, 'ts-p95'], [6, 'ts-sql-per-request'], [7, 'ts-cache-hit'],
  [8, 'ts-server-p95'], [9, 'ts-db-connections'], [10, 'ts-cpu'],
]

const browser = await chromium.launch()
// locale 을 꼭 준다. 컨테이너 안 브라우저는 언어 설정이 비어 있어 Grafana 가 "Incorrect locale information" 으로 시작하지 못한다
const page = await browser.newPage({
  viewport: { width: 1600, height: 1250 }, deviceScaleFactor: 2, locale: 'ko-KR', timezoneId: 'Asia/Seoul',
})

// Grafana 는 첫 화면에 "앱 파일을 못 불러왔다"는 대체 문구를 깔아 두고 JS 가 뜨면 지운다.
// 네트워크가 잠잠해진 시점에도 아직 JS 가 화면을 그리기 전일 수 있어서, 패널 내용이 나타날 때까지 기다린다
async function settle() {
  await page.waitForLoadState('load')
  for (let i = 0; i < 30; i++) {
    await page.waitForTimeout(1000)
    const state = await page.evaluate(() => ({
      failed: document.body.innerText.includes('failed to load its application files'),
      canvases: document.querySelectorAll('canvas').length,
    }))
    if (!state.failed && state.canvases > 0) break
  }
  await page.waitForTimeout(4000) // 쿼리 결과가 그려질 시간
}

await page.goto(`${base}/d/codexray-perf?${range}&kiosk&theme=dark`)
await settle()
await page.screenshot({ path: `${outDir}/dashboard.png`, fullPage: true })

// 패널 오른쪽 위 메뉴(⋮)는 마우스가 패널 위에 있으면 나타난다. 마우스를 빼고, 혹시 남아도 보이지 않게 숨긴다
const HIDE_MENU = `[aria-label^="Menu for panel"], [data-testid*="panel-menu"], [data-testid*="Panel menu"],
  [data-testid="data-testid panel-menu-button"] { display: none !important; }`

for (const [id, name] of PANELS) {
  const wide = name.startsWith('ts-')
  await page.setViewportSize(wide ? { width: 1200, height: 520 } : { width: 900, height: 460 })
  await page.goto(`${base}/d-solo/codexray-perf?${range}&panelId=${id}&theme=dark`)
  await settle()
  await page.addStyleTag({ content: HIDE_MENU })
  await page.mouse.move(0, 0)
  await page.waitForTimeout(300)
  await page.screenshot({ path: `${outDir}/${name}.png` })
}
await browser.close()
console.log(`saved to ${outDir}`)
