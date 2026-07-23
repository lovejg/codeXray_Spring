import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { apiErrorMessage } from '../lib/apiError'
import { AuthShell } from './LoginPage'

// 구글 OAuth 리다이렉트 콜백. ?code= 를 백엔드로 넘겨 우리 토큰으로 교환.
export default function OAuthCallbackPage() {
  const [params] = useSearchParams()
  const code = params.get('code')
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [error, setError] = useState('')
  const done = useRef(false)

  useEffect(() => {
    if (done.current) return
    done.current = true

    if (!code) {
      setError('인증 코드가 없습니다.')
      return
    }
    authApi
      .oauthGoogle(code)
      .then(async ({ accessToken }) => {
        localStorage.setItem('token', accessToken)
        const user = await authApi.me()
        setAuth(user, accessToken)
        navigate('/problems', { replace: true })
      })
      .catch((err) => setError(apiErrorMessage(err, '소셜 로그인에 실패했습니다.')))
  }, [code, navigate, setAuth])

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
