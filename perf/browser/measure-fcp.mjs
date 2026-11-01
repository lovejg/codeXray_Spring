// 화면 첫 로딩 측정. 느린 4G + CPU 4배 감속 조건에서 FCP·LCP·받은 JS 크기를 잰다.
//
//   node measure-fcp.mjs <결과.json> 이름=http://주소 [이름=http://주소 ...]
//
// 환경 변수: RUNS(기본 5, 중앙값을 쓴다), PATHS(기본 "/,/problems")
// 조건은 2026-10-30 README 측정과 같게 맞췄다: 지연 150ms · 내려받기 1.6Mbps · 올리기 750kbps · CPU 4배 느리게 · 캐시 없는 새 브라우저
import { chromium } from 'playwright'
import { writeFileSync } from 'node:fs'

const [, , outFile, ...targetArgs] = process.argv
const RUNS = Number(process.env.RUNS || 5)
const PATHS = (process.env.PATHS || '/,/problems').split(',')
const NETWORK = { offline: false, latency: 150, downloadThroughput: (1.6e6) / 8, uploadThroughput: (750e3) / 8 }
const CPU_SLOWDOWN = 4

const targets = targetArgs.map((a) => {
  const i = a.indexOf('=')
  return { name: a.slice(0, i), base: a.slice(i + 1) }
})

const median = (xs) => {
  const s = [...xs].sort((a, b) => a - b)
  const m = Math.floor(s.length / 2)
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2
}

async function once(browser, url) {
  // 매번 새 컨텍스트 = 빈 캐시·빈 쿠키
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, locale: 'ko-KR', timezoneId: 'Asia/Seoul' })
  const page = await context.newPage()
  const cdp = await context.newCDPSession(page)
  await cdp.send('Network.enable')
  await cdp.send('Network.setCacheDisabled', { cacheDisabled: true })
  await cdp.send('Network.emulateNetworkConditions', NETWORK)
  await cdp.send('Emulation.setCPUThrottlingRate', { rate: CPU_SLOWDOWN })

  const scripts = new Map() // requestId → { url, encoding }
  const bytes = new Map()
  cdp.on('Network.responseReceived', (e) => {
    if (e.type === 'Script') {
      const h = Object.fromEntries(Object.entries(e.response.headers).map(([k, v]) => [k.toLowerCase(), v]))
      scripts.set(e.requestId, { url: e.response.url, encoding: h['content-encoding'] || 'none' })
    }
  })
  cdp.on('Network.loadingFinished', (e) => bytes.set(e.requestId, e.encodedDataLength))

  await page.addInitScript(() => {
    window.__lcp = 0
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) window.__lcp = entry.startTime
    }).observe({ type: 'largest-contentful-paint', buffered: true })
  })

  await page.goto(url, { waitUntil: 'load', timeout: 120_000 })
  await page.waitForTimeout(2000) // 늦게 그려지는 큰 요소(LCP)를 기다린다
  const fcp = await page.evaluate(() => performance.getEntriesByName('first-contentful-paint')[0]?.startTime ?? null)
  const lcp = await page.evaluate(() => window.__lcp || null)

  let jsBytes = 0
  const encodings = new Set()
  for (const [id, s] of scripts) {
    jsBytes += bytes.get(id) || 0
    encodings.add(s.encoding)
  }
  await context.close()
  return { fcp, lcp, jsKB: Math.round(jsBytes / 1024), jsFiles: scripts.size, encodings: [...encodings] }
}

const browser = await chromium.launch()
const results = []
for (const t of targets) {
  for (const path of PATHS) {
    const url = t.base + path
    const runs = []
    for (let i = 0; i < RUNS; i++) runs.push(await once(browser, url))
    const row = {
      target: t.name,
      path,
      fcpMs: Math.round(median(runs.map((r) => r.fcp))),
      lcpMs: Math.round(median(runs.map((r) => r.lcp))),
      jsKB: median(runs.map((r) => r.jsKB)),
      jsFiles: median(runs.map((r) => r.jsFiles)),
      encodings: runs[0].encodings,
      runs,
    }
    results.push(row)
    console.log(`${t.name.padEnd(16)} ${path.padEnd(10)} FCP ${row.fcpMs}ms  LCP ${row.lcpMs}ms  JS ${row.jsKB}KB (${row.jsFiles}개, ${row.encodings.join('/')})`)
  }
}
await browser.close()

writeFileSync(outFile, JSON.stringify({
  conditions: { network: NETWORK, cpuSlowdown: CPU_SLOWDOWN, runs: RUNS, viewport: '1280x800', browser: 'Chromium (Playwright 1.49.1)' },
  results,
}, null, 2))
