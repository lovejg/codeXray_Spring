// 소셜 로그인 시작점: 프로바이더의 authorize 페이지로 브라우저를 보낸다.
// 콜백은 /oauth/callback/{provider} 로 돌아오고, OAuthCallbackPage 가 code 를 백엔드로 넘긴다.

export type OAuthProvider = 'google' | 'naver'

const STATE_KEY = 'oauth_state'

// CSRF 방지용 state: authorize 로 넘겼다가 콜백에서 되돌아온 값과 비교한다.
function newState(): string {
  return crypto.randomUUID()
}

export function startOAuth(provider: OAuthProvider): void {
  const state = newState()
  sessionStorage.setItem(STATE_KEY, state)

  const redirectUri = `${window.location.origin}/oauth/callback/${provider}`

  let authorizeUrl: string
  if (provider === 'google') {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
    if (!clientId) {
      alert('구글 클라이언트 ID(VITE_GOOGLE_CLIENT_ID)가 설정되지 않았습니다. frontend/.env 를 확인하세요.')
      return
    }
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      redirect_uri: redirectUri,
      scope: 'openid email profile',
      state,
    })
    authorizeUrl = `https://accounts.google.com/o/oauth2/v2/auth?${params}`
  } else {
    const clientId = import.meta.env.VITE_NAVER_CLIENT_ID as string | undefined
    if (!clientId) {
      alert('네이버 클라이언트 ID(VITE_NAVER_CLIENT_ID)가 설정되지 않았습니다. frontend/.env 를 확인하세요.')
      return
    }
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      redirect_uri: redirectUri,
      state,
    })
    authorizeUrl = `https://nid.naver.com/oauth2.0/authorize?${params}`
  }

  window.location.href = authorizeUrl
}

// 콜백에서 저장해둔 state 를 꺼내고 지운다(1회용).
export function consumeSavedState(): string | null {
  const saved = sessionStorage.getItem(STATE_KEY)
  sessionStorage.removeItem(STATE_KEY)
  return saved
}
