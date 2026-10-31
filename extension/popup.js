// 팝업: codeXray 로그인 → accessToken을 chrome.storage에 저장.
// 확장 페이지도 host_permissions 덕에 API를 CORS 없이 호출 가능.
const API = CODEXRAY_API // config.js (popup.html 에서 먼저 로드)

const $ = (id) => document.getElementById(id)

function setMsg(text, kind) {
  const el = $('msg')
  el.textContent = text
  el.className = 'msg' + (kind ? ' ' + kind : '')
}

// 연결 상태에 따라 화면 전환
async function render() {
  const { token, nickname } = await chrome.storage.local.get(['token', 'nickname'])
  if (token && nickname) {
    $('connected').style.display = 'block'
    $('loginForm').style.display = 'none'
    $('nickname').textContent = '@' + nickname
  } else {
    $('connected').style.display = 'none'
    $('loginForm').style.display = 'block'
  }
}

$('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault()
  setMsg('로그인 중…')
  $('submit').disabled = true
  try {
    const res = await fetch(`${API}/auth/login`, {
      method: 'POST',
      // X-Client: extension → 백엔드가 쿠키 대신 refresh 토큰을 body 로 내려줌
      headers: { 'Content-Type': 'application/json', 'X-Client': 'extension' },
      body: JSON.stringify({ email: $('email').value, password: $('password').value }),
    })
    if (!res.ok) {
      setMsg(res.status === 401 ? '이메일 또는 비밀번호가 올바르지 않아요.' : `로그인 실패 (${res.status})`, 'err')
      return
    }
    const { accessToken, refreshToken } = await res.json()

    // 닉네임 조회
    const meRes = await fetch(`${API}/users/me`, { headers: { Authorization: `Bearer ${accessToken}` } })
    const me = meRes.ok ? await meRes.json() : {}

    await chrome.storage.local.set({
      token: accessToken,
      refreshToken: refreshToken ?? null,
      nickname: me.nickname ?? '사용자',
    })
    setMsg('연결됐어요!', 'ok')
    render()
  } catch (err) {
    setMsg(`네트워크 오류: ${API} 에 연결하지 못했어요.`, 'err')
  } finally {
    $('submit').disabled = false
  }
})

$('logout').addEventListener('click', async () => {
  // 서버의 refresh 토큰도 지운다. 저장소만 비우면 토큰은 서버에 14일 동안 살아 있다(유출됐다면 계속 쓸 수 있다)
  const { refreshToken } = await chrome.storage.local.get('refreshToken')
  if (refreshToken) {
    try {
      await fetch(`${API}/auth/logout`, {
        method: 'POST',
        headers: { 'X-Refresh-Token': refreshToken, 'X-Client': 'extension' },
      })
    } catch {
      // 네트워크가 안 돼도 이 기기에서는 로그아웃한다. 서버 토큰은 만료(14일)로 사라진다
    }
  }
  await chrome.storage.local.remove(['token', 'refreshToken', 'nickname'])
  setMsg('로그아웃됐어요.')
  render()
})

render()
