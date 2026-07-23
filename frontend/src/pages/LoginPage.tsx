import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { apiErrorMessage } from '../lib/apiError'

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
        <Link to="/problems" className="mb-8 block text-center text-2xl font-bold tracking-tight">
          <span className="text-sky-400">code</span>
          <span className="text-white">Xray</span>
        </Link>
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <h1 className="mb-5 text-lg font-semibold text-white">{title}</h1>
          {children}
        </div>
      </div>
    </div>
  )
}

export function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm text-slate-400">{label}</span>
      {children}
    </label>
  )
}

export const inputCls =
  'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-600 focus:border-sky-500'
export const primaryBtn =
  'w-full rounded-lg bg-sky-500 py-2 text-sm font-medium text-white transition hover:bg-sky-400 disabled:opacity-60'
