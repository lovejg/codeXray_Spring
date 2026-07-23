import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { apiErrorMessage } from '../lib/apiError'
import { AuthShell, Field, inputCls, primaryBtn } from './LoginPage'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await authApi.register(email, password, nickname)
      // 가입 후 이메일 인증 안내 화면으로
      navigate('/verify-email', { state: { email } })
    } catch (err) {
      setError(apiErrorMessage(err, '회원가입에 실패했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="회원가입">
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="이메일">
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={inputCls} placeholder="you@example.com" />
        </Field>
        <Field label="닉네임">
          <input type="text" required minLength={2} maxLength={20} value={nickname} onChange={(e) => setNickname(e.target.value)} className={inputCls} placeholder="2~20자" />
        </Field>
        <Field label="비밀번호">
          <input type="password" required minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} className={inputCls} placeholder="8자 이상" />
        </Field>

        {error && <p className="text-sm text-rose-400">{error}</p>}

        <button type="submit" disabled={loading} className={primaryBtn}>
          {loading ? '가입 중…' : '회원가입'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-400">
        이미 계정이 있나요?{' '}
        <Link to="/login" className="text-sky-400 hover:underline">
          로그인
        </Link>
      </p>
    </AuthShell>
  )
}
