import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usersApi } from '../api/users'
import { useAuthStore } from '../store/authStore'
import { apiErrorMessage } from '../lib/apiError'
import Modal from '../components/common/Modal'
import DashboardStats from '../components/common/DashboardStats'
import Avatar from '../components/common/Avatar'

export default function ProfilePage() {
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const [nickname, setNickname] = useState(user?.nickname ?? '')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [pwOpen, setPwOpen] = useState(false)
  const [delOpen, setDelOpen] = useState(false)

  if (!user) return null

  async function saveNickname() {
    setMsg(''); setErr('')
    const next = nickname.trim()
    if (next.length < 2) { setErr('닉네임은 2자에서 20자 사이여야 합니다.'); return }
    try {
      await usersApi.updateNickname(next)
      setUser({ ...user!, nickname: next })
      setNickname(next)
      setMsg('닉네임이 변경되었습니다.')
    } catch (e) {
      setErr(apiErrorMessage(e, '닉네임 변경에 실패했습니다.'))
    }
  }

  return (
    <div>
      <h1 className="page-title mb-5">프로필</h1>

      <div className="glass-card max-w-xl p-6">
        <div className="mb-5 flex items-center gap-3 border-b border-slate-800 pb-5">
          <Avatar name={user.nickname} size={52} />
          <div className="min-w-0">
            <div className="truncate font-mono text-lg font-bold text-white">{user.nickname}</div>
            <div className="truncate text-sm text-slate-500">{user.email}</div>
          </div>
        </div>

        <Row label="이메일"><span className="text-slate-200">{user.email}</span></Row>
        <Row label="가입 방식"><span className="text-slate-400">{PROVIDER_LABEL[user.provider] ?? user.provider}</span></Row>
        <Row label="권한"><span className="text-slate-400">{user.role === 'ADMIN' ? '관리자' : '일반 회원'}</span></Row>

        <div className="mt-4">
          <label className="mb-1.5 block text-sm font-medium text-slate-400">닉네임</label>
          <div className="flex gap-2">
            <input
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && saveNickname()}
              minLength={2}
              maxLength={20}
              className="input-field flex-1"
            />
            <button onClick={saveNickname} disabled={nickname.trim() === user.nickname} className="btn-primary px-4">변경</button>
          </div>
          {msg && <p className="mt-2 text-sm text-emerald-400">{msg}</p>}
          {err && <p className="mt-2 text-sm text-rose-400">{err}</p>}
        </div>
      </div>

      <div className="mt-4 flex max-w-xl gap-2">
        {/* 소셜 가입 계정은 비밀번호가 없다 */}
        {user.provider === 'LOCAL' && <button onClick={() => setPwOpen(true)} className="btn-ghost">비밀번호 변경</button>}
        <button onClick={() => setDelOpen(true)} className="ml-auto rounded-md border border-rose-800/60 px-5 py-2.5 font-mono text-sm text-rose-400 transition hover:bg-rose-950/40">회원 탈퇴</button>
      </div>

      {/* 내 활동 통계 */}
      <p className="section-title mb-4 mt-12">// 내 활동</p>
      <DashboardStats />

      {pwOpen && <PasswordModal onClose={() => setPwOpen(false)} />}
      {delOpen && <WithdrawModal onClose={() => setDelOpen(false)} />}
    </div>
  )
}

const PROVIDER_LABEL: Record<string, string> = { LOCAL: '이메일', GOOGLE: 'Google', NAVER: '네이버' }

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-slate-800 py-2.5 last:border-0">
      <span className="text-sm text-slate-500">{label}</span>
      {children}
    </div>
  )
}

function PasswordModal({ onClose }: { onClose: () => void }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirmNext, setConfirmNext] = useState('')
  const [err, setErr] = useState('')
  const [done, setDone] = useState(false)

  const [saving, setSaving] = useState(false)

  async function submit() {
    if (!current) { setErr('현재 비밀번호를 입력해 주세요.'); return }
    if (next.length < 8) { setErr('새 비밀번호는 8자 이상이어야 합니다.'); return }
    if (next !== confirmNext) { setErr('새 비밀번호가 서로 다릅니다.'); return }
    setErr('')
    setSaving(true)
    try {
      await usersApi.updatePassword(current, next)
      setDone(true)
    } catch (e) {
      setErr(apiErrorMessage(e, '비밀번호 변경에 실패했습니다.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="비밀번호 변경">
      {done ? (
        <div>
          <p className="text-sm text-emerald-400">비밀번호가 변경되었습니다.</p>
          <button onClick={onClose} className="btn-primary mt-4 px-4 py-2">확인</button>
        </div>
      ) : (
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); void submit() }}>
          <input type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} placeholder="현재 비밀번호" className="input-field" />
          <input type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} placeholder="새 비밀번호 (8자 이상)" className="input-field" />
          <input type="password" autoComplete="new-password" value={confirmNext} onChange={(e) => setConfirmNext(e.target.value)} placeholder="새 비밀번호 확인" className="input-field" />
          {err && <p className="text-sm text-rose-400">{err}</p>}
          <div className="flex justify-end gap-2">
            <button type="button" onClick={onClose} className="btn-ghost px-4 py-2">취소</button>
            <button type="submit" disabled={saving} className="btn-primary px-4 py-2">{saving ? '변경 중…' : '변경'}</button>
          </div>
        </form>
      )}
    </Modal>
  )
}

function WithdrawModal({ onClose }: { onClose: () => void }) {
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  async function confirmDelete() {
    setErr('')
    setBusy(true)
    try {
      await usersApi.deleteAccount()
      // logout 이 서버의 로그인 유지 쿠키를 지우고, 브라우저의 토큰과 유저 상태도 비운다
      logout()
      navigate('/', { replace: true })
    } catch (e) {
      setErr(apiErrorMessage(e, '탈퇴에 실패했습니다.'))
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="회원 탈퇴">
      <p className="text-sm text-slate-300">정말 탈퇴하시겠어요? 되돌릴 수 없습니다.</p>
      <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-slate-400">
        <li>풀이와 메모, 노트, 체감 난이도 평가, 추천, 알림은 바로 지워집니다.</li>
        <li>커뮤니티 글과 댓글은 남고 작성자만 비워집니다. 남기기 싫은 글은 먼저 지워 주세요.</li>
      </ul>
      {err && <p className="mt-2 text-sm text-rose-400">{err}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button onClick={onClose} className="btn-ghost px-4 py-2">취소</button>
        <button onClick={confirmDelete} disabled={busy} className="rounded-md bg-rose-500 px-4 py-2 font-mono text-sm font-semibold text-white transition hover:bg-rose-400 disabled:opacity-60">
          {busy ? '탈퇴 중…' : '탈퇴'}
        </button>
      </div>
    </Modal>
  )
}
