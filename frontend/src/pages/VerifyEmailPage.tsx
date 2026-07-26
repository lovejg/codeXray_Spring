import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { authApi } from '../api/auth'
import { apiErrorMessage } from '../lib/apiError'
import { AuthShell, inputCls, primaryBtn } from './LoginPage'

type Status = 'idle' | 'verifying' | 'success' | 'error'

// 같은 브라우저의 탭들끼리 "인증 완료" 신호를 주고받는 채널 이름
const VERIFY_CHANNEL = 'codexray-email-verify'

// 이메일 인증: URL 에 ?token= 이 있으면 자동 인증, 없으면 재발송 안내.
export default function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const location = useLocation()
  const navigate = useNavigate()
  const emailFromState = (location.state as { email?: string } | null)?.email ?? ''

  const [status, setStatus] = useState<Status>(token ? 'verifying' : 'idle')
  const [message, setMessage] = useState('')
  const [email, setEmail] = useState(emailFromState)
  const [resent, setResent] = useState(false)
  // 다른 탭에서의 인증 완료로 성공 처리된 경우 → 로그인으로 자동 이동시킴
  const [autoRedirect, setAutoRedirect] = useState(false)

  // (새 탭) 토큰이 있으면 인증 실행. 성공하면 다른 탭들에 완료 신호를 방송.
  useEffect(() => {
    if (!token) return
    authApi
      .verifyEmail(token)
      .then(() => {
        setStatus('success')
        try {
          const ch = new BroadcastChannel(VERIFY_CHANNEL)
          ch.postMessage({ type: 'verified' })
          ch.close()
        } catch {
          // BroadcastChannel 미지원 브라우저는 그냥 무시(핵심 인증은 이미 성공)
        }
      })
      .catch((err) => {
        setStatus('error')
        setMessage(apiErrorMessage(err, '인증에 실패했습니다.'))
      })
  }, [token])

  // (대기 탭) 토큰이 없으면 다른 탭의 완료 신호를 구독 → 성공 화면으로 전환 후 로그인 이동
  useEffect(() => {
    if (token) return
    let ch: BroadcastChannel
    try {
      ch = new BroadcastChannel(VERIFY_CHANNEL)
    } catch {
      return
    }
    ch.onmessage = (e: MessageEvent) => {
      if (e.data?.type === 'verified') {
        setStatus('success')
        setAutoRedirect(true)
      }
    }
    return () => ch.close()
  }, [token])

  // 자동 이동 타이머: 대기 탭에서 인증이 확인되면 1.5초 뒤 로그인으로
  useEffect(() => {
    if (!autoRedirect) return
    const t = setTimeout(() => navigate('/login'), 1500)
    return () => clearTimeout(t)
  }, [autoRedirect, navigate])

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
        {autoRedirect && (
          <p className="mt-2 text-xs text-slate-400">잠시 후 로그인 화면으로 이동합니다…</p>
        )}
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
        메일의 “이메일 인증하기” 버튼을 누르면 이 화면이 자동으로 넘어갑니다. (스팸함도 확인해 주세요)
      </p>

      <div className="mt-5 space-y-2">
        {emailFromState ? (
          // 가입 직후 흐름: 이미 이메일을 알고 있으므로 수정 불가한 안내 텍스트로만 표시
          <p className="text-sm text-slate-300">
            <span className="text-slate-500">받는 이메일: </span>
            {email}
          </p>
        ) : (
          // /verify-email 로 직접 들어온 경우엔 이메일 입력이 필요
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputCls}
            placeholder="인증 메일을 받을 이메일"
          />
        )}
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
