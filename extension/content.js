// ─────────────────────────────────────────────────────────────
// content script: 프로그래머스 문제 페이지에 "저장" 버튼 주입.
// - 우측 세로 중앙 탭 (하단 제출/상단 헤더와 안 겹침)
// - 이미 풀이가 있으면 저장 전 "덮어쓰기 확인" 모달
// - 성공 시 정보 토스트(문제·언어·줄 수·새 풀이/덮어씀 + 앱에서 보기)
// - SPA 재렌더로 버튼이 사라지면 자동 재삽입
// ─────────────────────────────────────────────────────────────
;(function () {
  if (window.__codexrayInjected) return
  window.__codexrayInjected = true

  const APP_ORIGIN = 'http://localhost:5173'
  const LABEL = '❯ 저장'
  let btn = null

  // 확장이 새로고침/업데이트되면 이 오래된 content script의 컨텍스트가 무효화됨.
  // chrome.runtime.id 가 사라지므로, chrome.* 호출 전에 반드시 확인.
  function contextValid() {
    try { return !!(chrome.runtime && chrome.runtime.id) } catch (e) { return false }
  }

  // ── 버튼 ──
  function makeButton() {
    const b = document.createElement('button')
    b.id = 'codexray-save-btn'
    b.textContent = LABEL
    Object.assign(b.style, {
      position: 'fixed', right: '0', top: '42%', zIndex: '2147483647',
      display: 'flex', alignItems: 'center', gap: '6px', padding: '9px 13px',
      background: 'rgba(13,18,28,0.92)', color: '#2dd4bf',
      border: '1px solid rgba(45,212,191,0.55)', borderRight: 'none',
      borderRadius: '9px 0 0 9px', fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
      fontSize: '12px', fontWeight: '700', cursor: 'pointer', backdropFilter: 'blur(6px)',
      boxShadow: '0 4px 18px rgba(0,0,0,0.45)', transition: 'transform .12s ease, background .12s ease',
    })
    b.addEventListener('mouseenter', () => { b.style.transform = 'translateX(-3px)'; b.style.background = 'rgba(45,212,191,0.14)' })
    b.addEventListener('mouseleave', () => { b.style.transform = 'translateX(0)'; b.style.background = 'rgba(13,18,28,0.92)' })
    b.addEventListener('click', () => doSave(false))
    return b
  }

  function setBusy(busy) {
    if (!btn) return
    btn.disabled = busy
    btn.textContent = busy ? '❯ 저장 중…' : LABEL
  }

  // ── 토스트 (기본/에러) ──
  function toast(msg, ok = true) {
    const t = document.createElement('div')
    t.textContent = msg
    Object.assign(t.style, baseCardStyle(ok))
    document.body.appendChild(t)
    setTimeout(() => t.remove(), 5000)
  }

  // ── 성공 토스트 (저장 요약 + 메모는 홈페이지에서 추가하도록 유도) ──
  function showSuccessToast(res) {
    const wrap = document.createElement('div')
    Object.assign(wrap.style, {
      position: 'fixed', right: '16px', top: 'calc(42% + 44px)', zIndex: '2147483647',
      width: '300px', padding: '13px 15px',
      background: '#0d121c', border: '1px solid rgba(45,212,191,0.5)', borderRadius: '9px',
      fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', color: '#c8d2e0',
      fontSize: '12px', lineHeight: '1.55', boxShadow: '0 6px 24px rgba(0,0,0,0.55)',
    })

    const action = res.created ? '새 풀이로 저장됨' : '기존 풀이를 덮어썼어요'
    const langNote = res.detected ? res.language : `${res.language}(기본값)`
    const head = document.createElement('div')
    head.innerHTML =
      `<div style="color:#2dd4bf;font-weight:700">✔ ${action}</div>` +
      `<div style="color:#94a3b8;margin-top:3px">${escapeHtml(res.title)} · ${escapeHtml(langNote)} · ${res.lines}줄</div>`
    wrap.appendChild(head)

    const hint = document.createElement('div')
    hint.textContent = '틀린 이유·풀이 로직·핵심 함수 메모는 홈페이지에서 추가할 수 있어요.'
    Object.assign(hint.style, { color: '#64748b', margin: '9px 0 8px', fontSize: '11px' })
    wrap.appendChild(hint)

    const row = document.createElement('div')
    Object.assign(row.style, { display: 'flex', gap: '8px', alignItems: 'center' })

    const memoBtn = document.createElement('button')
    memoBtn.textContent = '메모 추가하기 ↗'
    Object.assign(memoBtn.style, {
      padding: '7px 12px', border: 'none', borderRadius: '6px', cursor: 'pointer',
      background: '#2dd4bf', color: '#0a0d13', fontFamily: 'inherit', fontWeight: '700', fontSize: '12px',
    })
    // 그 풀이의 편집 페이지로 (메모 섹션이 자동으로 열림)
    memoBtn.addEventListener('click', () => {
      window.open(res.id ? `${APP_ORIGIN}/solutions/${res.id}/edit?focus=memo` : `${APP_ORIGIN}/solutions`, '_blank')
    })

    const closeBtn = document.createElement('button')
    closeBtn.textContent = '닫기'
    Object.assign(closeBtn.style, {
      marginLeft: 'auto', padding: '7px 10px', border: '1px solid #334155', borderRadius: '6px',
      cursor: 'pointer', background: 'transparent', color: '#94a3b8', fontFamily: 'inherit', fontSize: '12px',
    })
    closeBtn.addEventListener('click', () => wrap.remove())

    row.appendChild(memoBtn)
    row.appendChild(closeBtn)
    wrap.appendChild(row)
    document.body.appendChild(wrap)
    setTimeout(() => wrap.remove(), 12000) // 클릭할 시간 여유
  }

  function baseCardStyle(ok) {
    return {
      position: 'fixed', right: '16px', top: 'calc(42% + 44px)', zIndex: '2147483647',
      maxWidth: '320px', padding: '11px 14px',
      background: ok ? '#0f172a' : '#3f1d2e', color: ok ? '#c8d2e0' : '#fecaca',
      border: `1px solid ${ok ? '#2dd4bf' : '#f43f5e'}`, borderRadius: '8px',
      fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: '12px',
      lineHeight: '1.55', whiteSpace: 'pre-wrap', boxShadow: '0 6px 22px rgba(0,0,0,0.5)',
    }
  }

  // ── 덮어쓰기 확인 모달 ──
  function showConfirm(info) {
    const wrap = document.createElement('div')
    Object.assign(wrap.style, {
      position: 'fixed', right: '20px', top: '34%', zIndex: '2147483647', width: '300px',
      padding: '16px', background: '#0d121c', border: '1px solid rgba(251,191,36,0.5)',
      borderRadius: '10px', fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
      color: '#c8d2e0', fontSize: '12px', lineHeight: '1.6', boxShadow: '0 8px 30px rgba(0,0,0,0.6)',
    })
    wrap.innerHTML =
      `<div style="color:#fbbf24;font-weight:700;margin-bottom:8px">⚠ 이미 저장된 풀이가 있어요</div>` +
      `<div style="color:#94a3b8">문제: <span style="color:#e2e8f0">${escapeHtml(info.title)}</span></div>` +
      `<div style="color:#94a3b8">기존: ${escapeHtml(info.existingLanguage)} · ${info.existingLines}줄</div>` +
      `<div style="color:#94a3b8">새로: ${escapeHtml(info.newLanguage)} · ${info.newLines}줄</div>` +
      `<div style="margin-top:8px;color:#fca5a5">저장하면 이전 코드는 덮어써지고 되돌릴 수 없어요.<br>(별표·메모는 유지)</div>`

    const row = document.createElement('div')
    Object.assign(row.style, { display: 'flex', gap: '8px', marginTop: '12px' })

    const overwrite = document.createElement('button')
    overwrite.textContent = '덮어쓰기'
    Object.assign(overwrite.style, {
      flex: '1', padding: '7px', border: 'none', borderRadius: '6px', cursor: 'pointer',
      background: '#2dd4bf', color: '#0a0d13', fontFamily: 'inherit', fontWeight: '700', fontSize: '12px',
    })
    overwrite.addEventListener('click', () => { wrap.remove(); doSave(true) })

    const cancel = document.createElement('button')
    cancel.textContent = '취소'
    Object.assign(cancel.style, {
      flex: '1', padding: '7px', border: '1px solid #334155', borderRadius: '6px', cursor: 'pointer',
      background: 'transparent', color: '#94a3b8', fontFamily: 'inherit', fontSize: '12px',
    })
    cancel.addEventListener('click', () => { wrap.remove(); setBusy(false) })

    row.appendChild(overwrite)
    row.appendChild(cancel)
    wrap.appendChild(row)
    document.body.appendChild(wrap)
  }

  function escapeHtml(s) {
    return String(s ?? '').replace(/[&<>"']/g, (c) => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ))
  }

  // ── 저장 ──
  function doSave(confirmed) {
    if (!contextValid()) {
      toast('확장이 업데이트됐어요.\n이 페이지를 새로고침(F5) 해주세요.', false)
      setBusy(false)
      return
    }
    setBusy(true)
    chrome.runtime.sendMessage({ type: 'SAVE', confirmed }, (res) => {
      if (chrome.runtime.lastError) { setBusy(false); toast('확장 오류: ' + chrome.runtime.lastError.message, false); return }
      if (res?.needAuth) { setBusy(false); toast('로그인이 필요해요.\n우측 상단 codeXray 확장 아이콘을 눌러 로그인하세요.', false); return }
      if (res?.needConfirm) { setBusy(false); showConfirm(res); return } // 모달에서 이어서 처리
      setBusy(false)
      if (!res || !res.ok) { toast('저장 실패\n' + (res?.error ?? '알 수 없는 오류'), false); return }
      showSuccessToast(res)
    })
  }

  // ── 버튼 유지(SPA 대응) ──
  function ensureButton() {
    if (!document.body) return
    if (!btn || !document.body.contains(btn)) {
      btn = makeButton()
      document.body.appendChild(btn)
    }
  }
  ensureButton()

  let scheduled = false
  const observer = new MutationObserver(() => {
    // 컨텍스트가 죽었으면(확장 새로고침) 관찰 중단 — 죽은 스크립트가 계속 돌지 않게
    if (!contextValid()) { observer.disconnect(); return }
    if (scheduled) return
    scheduled = true
    requestAnimationFrame(() => { scheduled = false; ensureButton() })
  })
  observer.observe(document.documentElement, { childList: true, subtree: true })
})()
