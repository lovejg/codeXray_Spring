import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { apiErrorMessage } from '../lib/apiError'
import { consumeSavedState, type OAuthProvider } from '../lib/oauth'
import { setAccessToken } from '../lib/tokens'
import { AuthShell } from './LoginPage'

// 소셜 OAuth 리다이렉트 콜백. /oauth/callback/{provider} 로 돌아온다.
// ?code= 를 백엔드로 넘겨 우리 토큰으로 교환. state 는 CSRF 방지용으로 검증.
export default function OAuthCallbackPage() {
  const { provider } = useParams<{ provider: OAuthProvider }>()
  const [params] = useSearchParams()
  const code = params.get('code')
  const returnedState = params.get('state')
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [error, setError] = useState('')
  const done = useRef(false)

  useEffect(() => {
    if (done.current) return
    done.current = true

    const savedState = consumeSavedState()

    // 사용자가 동의 화면에서 취소하면 code 대신 error 가 돌아온다
    if (params.get('error')) {
      setError('소셜 로그인이 취소되었습니다.')
      return
    }
    if (!code) {
      setError('인증 코드가 없습니다.')
      return
    }
    // authorize 때 저장한 state 가 없거나 되돌아온 값과 다르면 위조 요청으로 간주(저장값이 없을 때 통과시키면 검사가 무의미해진다)
    if (!savedState || returnedState !== savedState) {
      setError('보안 검증(state)에 실패했습니다. 다시 시도해 주세요.')
      return
    }

    const exchange =
      provider === 'naver'
        ? authApi.oauthNaver(code, returnedState ?? '')
        : authApi.oauthGoogle(code)

    exchange
      .then(async ({ accessToken }) => {
        setAccessToken(accessToken)
        const user = await authApi.me()
        setAuth(user, accessToken)
        navigate('/problems', { replace: true })
      })
      .catch((err) => setError(apiErrorMessage(err, '소셜 로그인에 실패했습니다.')))
  }, [code, returnedState, provider, params, navigate, setAuth])

  return (
    <AuthShell title="소셜 로그인">
      {error ? (
        <p className="text-sm text-rose-400">{error}</p>
      ) : (
        <p className="text-sm text-slate-400">로그인 처리 중…</p>
      )}
    </AuthShell>
  )
}
