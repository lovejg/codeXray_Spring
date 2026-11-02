// access token 은 메모리(이 모듈의 변수)에만 둔다. localStorage 에 두면 XSS 한 번으로 스크립트가 꺼내 갈 수 있다.
// 새로고침하면 사라지므로 앱이 시작될 때 refresh 쿠키(httpOnly)로 다시 받는다(useBootstrapAuth).
// refresh token 은 백엔드가 httpOnly 쿠키로 관리하므로 JS 에서 저장하지 않는다.
let accessToken: string | null = null

// "이 브라우저에서 로그인한 적이 있다"는 표시만 남긴다(토큰이 아니다).
// 한 번도 로그인하지 않은 방문자에게까지 시작할 때마다 refresh 를 보내 401 을 받지 않게 하려는 것
const SESSION_HINT_KEY = 'has-session'
const LEGACY_TOKEN_KEY = 'token' // 예전 버전이 access token 을 넣어 두던 자리

function storage(): Storage | null {
  try {
    return window.localStorage
  } catch {
    return null // 사생활 보호 모드 등에서 막혀 있으면 표시 없이 동작한다
  }
}

// 예전 버전으로 로그인해 둔 사용자는 표시가 없고 토큰만 있다. 이 배포로 로그아웃되지 않게 표시로 바꿔 두고 토큰은 지운다
const legacy = storage()
if (legacy?.getItem(LEGACY_TOKEN_KEY)) {
  legacy.setItem(SESSION_HINT_KEY, '1')
  legacy.removeItem(LEGACY_TOKEN_KEY)
}

export const getAccessToken = () => accessToken

export const setAccessToken = (token: string) => {
  accessToken = token
  storage()?.setItem(SESSION_HINT_KEY, '1')
}

export const clearAccessToken = () => {
  accessToken = null
  storage()?.removeItem(SESSION_HINT_KEY)
}

export const hasSessionHint = () => storage()?.getItem(SESSION_HINT_KEY) === '1'
