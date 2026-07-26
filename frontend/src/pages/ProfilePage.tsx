import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usersApi } from '../api/users'
import { useAuthStore } from '../store/authStore'
import { apiErrorMessage } from '../lib/apiError'
import Modal from '../components/common/Modal'

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
    try {
      await usersApi.updateNickname(nickname.trim())
      setUser({ ...user!, nickname: nickname.trim() })
      setMsg('닉네임이 변경되었습니다.')
    } catch (e) {
      setErr(apiErrorMessage(e, '닉네임 변경에 실패했습니다.'))
    }
  }

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="page-title mb-5">프로필</h1>

      <div className="glass-card p-6">
        <Row label="이메일"><span className="text-slate-200">{user.email}</span></Row>
        <Row label="권한"><span className="text-slate-400">{user.role === 'ADMIN' ? '관리자' : '일반 회원'}</span></Row>

        <div className="mt-4">
          <label className="mb-1.5 block text-sm font-medium text-slate-400">닉네임</label>
          <div className="flex gap-2">
            <input value={nickname} onChange={(e) => setNickname(e.target.value)} minLength={2} maxLength={20} className="input-field flex-1" />
            <button onClick={saveNickname} className="btn-primary px-4">변경</button>
          </div>
          {msg && <p className="mt-2 text-sm text-emerald-400">{msg}</p>}
          {err && <p className="mt-2 text-sm text-rose-400">{err}</p>}
        </div>
      </div>

      <div className="mt-4 flex gap-2">
        <button onClick={() => setPwOpen(true)} className="btn-ghost">비밀번호 변경</button>
        <button onClick={() => setDelOpen(true)} className="ml-auto rounded-xl border border-rose-800/60 px-5 py-2.5 text-sm text-rose-400 transition hover:bg-rose-950/40">회원 탈퇴</button>
      </div>

      {pwOpen && <PasswordModal onClose={() => setPwOpen(false)} />}
      {delOpen && <WithdrawModal onClose={() => setDelOpen(false)} />}
    </div>
  )
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-white/10 py-2.5 last:border-0">
      <span className="text-sm text-slate-500">{label}</span>
      {children}
    </div>
  )
}

function PasswordModal({ onClose }: { onClose: () => void }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [err, setErr] = useState('')
  const [done, setDone] = useState(false)

  async function submit() {
    if (next.length < 8) { setErr('새 비밀번호는 8자 이상이어야 합니다.'); return }
    setErr('')
    try {
      await usersApi.updatePassword(current, next)
      setDone(true)
    } catch (e) {
      setErr(apiErrorMessage(e, '비밀번호 변경에 실패했습니다.'))
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
        <div className="space-y-3">
          <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} placeholder="현재 비밀번호" className={pwInput} />
          <input type="password" value={next} onChange={(e) => setNext(e.target.value)} placeholder="새 비밀번호 (8자 이상)" className={pwInput} />
          {err && <p className="text-sm text-rose-400">{err}</p>}
          <div className="flex justify-end gap-2">
            <button onClick={onClose} className="btn-ghost px-4 py-2">취소</button>
            <button onClick={submit} className="btn-primary px-4 py-2">변경</button>
          </div>
        </div>
      )}
    </Modal>
  )
}

function WithdrawModal({ onClose }: { onClose: () => void }) {
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()
  const [err, setErr] = useState('')

  async function confirmDelete() {
    setErr('')
    try {
      await usersApi.deleteAccount()
      // 계정 삭제 후 로컬 상태 정리 + 로그인 화면으로
      useAuthStore.setState({ user: null, token: null })
      localStorage.removeItem('token')
      logout()
      navigate('/login')
    } catch (e) {
      setErr(apiErrorMessage(e, '탈퇴에 실패했습니다.'))
    }
  }

  return (
    <Modal open onClose={onClose} title="회원 탈퇴">
      <p className="text-sm text-slate-300">정말 탈퇴하시겠어요? 내 풀이·노트 등 개인 데이터가 삭제되며 되돌릴 수 없습니다.</p>
      {err && <p className="mt-2 text-sm text-rose-400">{err}</p>}
      <div className="mt-4 flex justify-end gap-2">
        <button onClick={onClose} className="btn-ghost px-4 py-2">취소</button>
        <button onClick={confirmDelete} className="rounded-xl bg-rose-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-rose-400">탈퇴</button>
      </div>
    </Modal>
  )
}

const pwInput = 'input-field'
