// 팝업: codeXray 로그인 → accessToken을 chrome.storage에 저장.
// 확장 페이지도 host_permissions 덕에 localhost API를 CORS 없이 호출 가능.
const API = 'http://localhost:8080/api'

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
    setMsg('네트워크 오류: 백엔드(localhost:8080)가 실행 중인지 확인하세요.', 'err')
  } finally {
    $('submit').disabled = false
  }
})

$('logout').addEventListener('click', async () => {
  await chrome.storage.local.remove(['token', 'refreshToken', 'nickname'])
  setMsg('로그아웃됐어요.')
  render()
})

render()
