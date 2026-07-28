// ─────────────────────────────────────────────────────────────
// service worker: 저장 흐름 오케스트레이션 + 토큰 자동 갱신 + 아이콘.
//  1) MAIN world 주입으로 에디터 코드·언어·URL·제목 추출
//  2) URL로 문제 매칭(/problems/lookup, 공개)
//  3) 풀이 저장(POST /solutions) — Bearer, 401이면 refresh 후 1회 재시도
// 확장은 host_permissions 덕에 background에서 CORS 없이 localhost 호출 가능.
// ─────────────────────────────────────────────────────────────
const API = 'http://localhost:8080/api'

// ── 페이지 컨텍스트(MAIN world)에서 실행될 추출 함수 (self-contained) ──
function extractInPage() {
  function normLang(mode) {
    let s = ''
    if (typeof mode === 'string') s = mode
    else if (mode && mode.name) s = mode.name
    s = s.toLowerCase()
    if (!s) return null
    if (s.includes('python')) return 'python'
    if (s.includes('x-java') || s === 'java') return 'java'
    if (s.includes('c++') || s.includes('cpp')) return 'cpp'
    if (s.includes('kotlin')) return 'kotlin'
    if (s.includes('typescript')) return 'typescript'
    if (s.includes('javascript') || s.includes('jsx')) return 'javascript'
    if (s === 'go' || s.includes('x-go') || s.includes('/go')) return 'go'
    if (s.includes('rust')) return 'rust'
    if (s.includes('swift')) return 'swift'
    if (s.includes('sql')) return 'sql'
    if (s.includes('x-csrc') || s === 'c' || s.includes('text/x-c')) return 'c'
    return null
  }

  function readTitle() {
    const el = document.querySelector('.algorithm-title, .lesson-title, h1')
    if (el && el.textContent.trim()) return el.textContent.trim()
    const m = document.title.split(' - ')[1]
    return (m ? m.split('|')[0] : document.title).trim()
  }

  const base = { url: location.href, title: readTitle() }

  // 1) Ace 에디터
  try {
    const el = document.querySelector('.ace_editor')
    if (el && window.ace) {
      const ed = window.ace.edit(el)
      let lang = null
      try { lang = normLang(String(ed.session.getMode().$id).split('/').pop()) } catch (e) {}
      return { ok: true, code: ed.getValue(), language: lang, strategy: 'ace', ...base }
    }
  } catch (e) {}

  // 2) CodeMirror 5 (프로그래머스 현재 에디터)
  try {
    const el = document.querySelector('.CodeMirror')
    if (el && el.CodeMirror) {
      const cm = el.CodeMirror
      let lang = null
      try { lang = normLang(cm.getOption('mode')) || normLang(cm.getMode().name) } catch (e) {}
      return { ok: true, code: cm.getValue(), language: lang, strategy: 'cm5', ...base }
    }
  } catch (e) {}

  // 3) 최후: ace 렌더된 줄 DOM 텍스트 (누락 위험)
  try {
    const lines = document.querySelectorAll('.ace_line')
    if (lines.length) {
      const code = Array.from(lines).map((l) => l.textContent).join('\n')
      return { ok: true, code, language: null, strategy: 'dom-fallback', ...base }
    }
  } catch (e) {}

  return { ok: false, error: '지원되는 에디터를 찾지 못했습니다.' }
}

// ── 토큰 갱신: 저장된 refreshToken 으로 새 access(+refresh) 발급 ──
async function refreshAccessToken() {
  const { refreshToken } = await chrome.storage.local.get('refreshToken')
  if (!refreshToken) return null
  const res = await fetch(`${API}/auth/refresh`, {
    method: 'POST',
    headers: { 'X-Refresh-Token': refreshToken, 'X-Client': 'extension' },
  })
  if (!res.ok) {
    await chrome.storage.local.remove(['token', 'refreshToken']) // refresh 만료 → 재로그인 필요
    return null
  }
  const data = await res.json()
  await chrome.storage.local.set({ token: data.accessToken, refreshToken: data.refreshToken ?? refreshToken })
  return data.accessToken
}

// Bearer 자동 첨부 + 401 시 1회 갱신 재시도
async function authFetch(url, opts = {}) {
  const { token } = await chrome.storage.local.get('token')
  if (!token) return { authFailed: true }

  const call = (t) => fetch(url, { ...opts, headers: { ...(opts.headers || {}), Authorization: `Bearer ${t}` } })
  let res = await call(token)
  if (res.status === 401) {
    const fresh = await refreshAccessToken()
    if (!fresh) return { authFailed: true }
    res = await call(fresh)
  }
  return { res }
}

// 이 문제에 이미 저장된 내 풀이가 있으면 반환 (없으면 null)
async function findExisting(problemId) {
  const { res, authFailed } = await authFetch(`${API}/solutions`)
  if (authFailed) return { authFailed: true }
  if (!res.ok) return { match: null }
  const list = await res.json()
  return { match: list.find((s) => s.problem?.id === problemId) ?? null }
}

// ── 저장 흐름 ──
// confirmed=false 로 처음 호출 → 기존 풀이가 있으면 needConfirm 반환(덮어쓰기 경고용).
// 사용자가 확인하면 confirmed=true 로 다시 호출 → 실제 저장.
async function handleSave(tabId, confirmed) {
  const results = await chrome.scripting.executeScript({ target: { tabId }, world: 'MAIN', func: extractInPage })
  const ext = results?.[0]?.result
  if (!ext || !ext.ok) return { ok: false, error: ext?.error ?? '코드 추출 실패' }

  const { token } = await chrome.storage.local.get('token')
  if (!token) return { needAuth: true }

  // URL → 문제 매칭 (공개 API)
  const lookup = await fetch(`${API}/problems/lookup?url=${encodeURIComponent(ext.url)}`)
  if (!lookup.ok) return { ok: false, error: 'codeXray에서 이 문제를 찾지 못했어요.' }
  const problem = await lookup.json()

  const language = ext.language || 'python'
  const newLines = ext.code.split('\n').length

  // 이미 있으면(그리고 아직 확인 전이면) 덮어쓰기 경고
  const { match, authFailed } = await findExisting(problem.id)
  if (authFailed) return { needAuth: true }
  if (match && !confirmed) {
    return {
      needConfirm: true,
      title: problem.title,
      existingLanguage: match.language,
      existingLines: (match.code || '').split('\n').length,
      newLanguage: language,
      newLines,
      updatedAt: match.updatedAt,
    }
  }

  // 풀이 저장 (Bearer + 자동 갱신)
  const save = await authFetch(`${API}/solutions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId: problem.id, code: ext.code, language }),
  })
  if (save.authFailed) return { needAuth: true }
  if (!save.res.ok) return { ok: false, error: `저장 실패 (${save.res.status})` }

  const saved = await save.res.json()
  return { ok: true, id: saved.id, created: !match, title: problem.title, language, lines: newLines, detected: ext.language != null }
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg?.type !== 'SAVE') return
  const tabId = sender.tab?.id
  if (tabId == null) { sendResponse({ ok: false, error: '탭 정보 없음' }); return }
  handleSave(tabId, msg.confirmed === true)
    .then(sendResponse)
    .catch((err) => sendResponse({ ok: false, error: String(err) }))
  return true // 비동기 응답
})

// ── 아이콘: 서비스워커에서 OffscreenCanvas 로 ❯ 아이콘을 그려 등록 (별도 PNG 불필요) ──
async function setupIcon() {
  try {
    const size = 32
    const canvas = new OffscreenCanvas(size, size)
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#0a0d13'
    ctx.fillRect(0, 0, size, size)
    // 청록 chevron(❯)
    ctx.strokeStyle = '#2dd4bf'
    ctx.lineWidth = 3.5
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.beginPath()
    ctx.moveTo(11, 8)
    ctx.lineTo(21, 16)
    ctx.lineTo(11, 24)
    ctx.stroke()
    const imageData = ctx.getImageData(0, 0, size, size)
    await chrome.action.setIcon({ imageData })
  } catch (e) {}
}
chrome.runtime.onInstalled.addListener(setupIcon)
chrome.runtime.onStartup.addListener(setupIcon)
