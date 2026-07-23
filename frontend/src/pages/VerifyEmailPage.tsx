import { useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { apiErrorMessage } from '../lib/apiError'
import { AuthShell, inputCls, primaryBtn } from './LoginPage'

type Status = 'idle' | 'verifying' | 'success' | 'error'

// 이메일 인증: URL 에 ?token= 이 있으면 자동 인증, 없으면 재발송 안내.
export default function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const location = useLocation()
  const emailFromState = (location.state as { email?: string } | null)?.email ?? ''

  const [status, setStatus] = useState<Status>(token ? 'verifying' : 'idle')
  const [message, setMessage] = useState('')
  const [email, setEmail] = useState(emailFromState)
  const [resent, setResent] = useState(false)

  useEffect(() => {
    if (!token) return
    authApi
      .verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((err) => {
        setStatus('error')
        setMessage(apiErrorMessage(err, '인증에 실패했습니다.'))
      })
  }, [token])

  async function resend() {
    if (!email) return
    setResent(false)
    try {
      await authApi.resendVerification(email)
      setResent(true)
    } catch (err) {
      setMessage(apiErrorMessage(err, '재발송에 실패했습니다.'))
    }
  }

  if (status === 'verifying') {
    return (
      <AuthShell title="이메일 인증">
        <p className="text-sm text-slate-400">인증 처리 중…</p>
      </AuthShell>
    )
  }

  if (status === 'success') {
    return (
      <AuthShell title="인증 완료">
        <p className="text-sm text-emerald-400">이메일 인증이 완료되었습니다. 이제 로그인할 수 있어요.</p>
        <Link to="/login" className={`mt-5 block text-center ${primaryBtn}`}>
          로그인하러 가기
        </Link>
      </AuthShell>
    )
  }

  // idle(가입 직후) or error
  return (
    <AuthShell title="이메일 인증 필요">
      <p className="text-sm text-slate-400">
        {status === 'error'
          ? message
          : '가입한 이메일로 인증 링크를 보냈어요. 메일함을 확인해 주세요.'}
      </p>
      <p className="mt-2 text-xs text-slate-500">
        (개발 환경에서는 백엔드 콘솔 로그에 인증 링크가 출력됩니다.)
      </p>

      <div className="mt-5 space-y-2">
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={inputCls}
          placeholder="인증 메일을 받을 이메일"
        />
        <button onClick={resend} className={primaryBtn}>
          인증 메일 재발송
        </button>
        {resent && <p className="text-sm text-emerald-400">재발송했습니다. 메일함을 확인해 주세요.</p>}
      </div>

      <p className="mt-6 text-center text-sm text-slate-400">
        <Link to="/login" className="text-sky-400 hover:underline">
          로그인으로 돌아가기
        </Link>
      </p>
    </AuthShell>
  )
}
