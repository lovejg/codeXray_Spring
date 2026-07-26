import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { apiErrorCode, apiErrorMessage } from '../lib/apiError'
import { startOAuth } from '../lib/oauth'

export default function LoginPage() {
  const setAuth = useAuthStore((s) => s.setAuth)
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/problems'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { accessToken } = await authApi.login(email, password)
      // 유저 정보를 위해 access token 을 먼저 저장한 뒤 me 조회
      useAuthStore.setState({ token: accessToken })
      localStorage.setItem('token', accessToken)
      const user = await authApi.me()
      setAuth(user, accessToken)
      navigate(from, { replace: true })
    } catch (err) {
      // 미인증 계정이면 에러만 띄우지 말고 인증 안내 화면으로 유도(이메일 채워서)
      if (apiErrorCode(err) === 'EMAIL_NOT_VERIFIED') {
        navigate('/verify-email', { state: { email } })
        return
      }
      setError(apiErrorMessage(err, '로그인에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="로그인">
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="이메일">
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputCls}
            placeholder="you@example.com"
          />
        </Field>
        <Field label="비밀번호">
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputCls}
            placeholder="••••••••"
          />
        </Field>

        {error && <p className="text-sm text-rose-400">{error}</p>}

        <button type="submit" disabled={loading} className={primaryBtn}>
          {loading ? '로그인 중…' : '로그인'}
        </button>
      </form>

      {/* 소셜 로그인 */}
      <div className="mt-6">
        <div className="mb-4 flex items-center gap-3 text-xs text-slate-500">
          <span className="h-px flex-1 bg-slate-800" />
          또는
          <span className="h-px flex-1 bg-slate-800" />
        </div>
        <div className="space-y-2">
          <button
            type="button"
            onClick={() => startOAuth('google')}
            className="flex w-full items-center justify-center gap-2 rounded-lg border border-slate-700 bg-white py-2 text-sm font-medium text-slate-800 transition hover:bg-slate-100"
          >
            <GoogleIcon />
            Google로 로그인
          </button>
          <button
            type="button"
            onClick={() => startOAuth('naver')}
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-[#03C75A] py-2 text-sm font-medium text-white transition hover:brightness-95"
          >
            <span className="text-base font-bold">N</span>
            네이버로 로그인
          </button>
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-slate-400">
        아직 계정이 없나요?{' '}
        <Link to="/register" className="text-sky-400 hover:underline">
          회원가입
        </Link>
      </p>
    </AuthShell>
  )
}

// ── 인증 페이지 공용 UI ──
export function AuthShell({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <Link to="/problems" className="mb-8 flex items-center justify-center gap-2 font-mono text-3xl font-bold tracking-tight">
          <span className="text-teal-400">❯</span>
          <span className="text-white">code<span className="text-teal-400">Xray</span></span>
          <span className="cursor-blink -ml-1 text-teal-400">▊</span>
        </Link>
        <div className="glass-card p-7">
          <h1 className="mb-6 font-mono text-xl font-semibold text-white">{title}</h1>
          {children}
        </div>
      </div>
    </div>
  )
}

export function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-slate-400">{label}</span>
      {children}
    </label>
  )
}

function GoogleIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 48 48" aria-hidden>
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
  )
}

export const inputCls = 'input-field'
export const primaryBtn = 'btn-primary w-full py-2.5'
