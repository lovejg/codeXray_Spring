// SVG 를 PNG 로(슬라이드용). docs/architecture.svg 를 고치면 다시 뽑는다:
//   docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp -v "$PWD:/repo" -w /repo/perf/browser \
//     mcr.microsoft.com/playwright:v1.49.1-noble node render-svg.mjs /repo/docs/architecture.svg /repo/docs/architecture.png
import { chromium } from 'playwright';
import { readFileSync } from 'node:fs';

const [src, out] = process.argv.slice(2);
const svg = readFileSync(src, 'utf8');
const [, w, h] = svg.match(/viewBox="0 0 (\d+) (\d+)"/);
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: +w, height: +h }, deviceScaleFactor: 2 });
await page.setContent(`<html><body style="margin:0">${svg}</body></html>`);
await page.evaluate(() => document.fonts.ready);
await page.locator('svg').screenshot({ path: out });
await browser.close();
console.log(`${out} (${w}×${h}, 2배율)`);
