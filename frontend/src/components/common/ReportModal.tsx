import { useState } from 'react'
import { communityApi } from '../../api/community'
import { apiErrorMessage } from '../../lib/apiError'
import Modal from './Modal'

export default function ReportModal({ postId, onClose }: { postId: number; onClose: () => void }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit() {
    if (reason.trim().length < 2) { setError('신고 사유를 입력해 주세요.'); return }
    setBusy(true)
    setError('')
    try {
      await communityApi.report(postId, reason.trim())
      setDone(true)
    } catch (err) {
      setError(apiErrorMessage(err, '신고에 실패했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open onClose={onClose} title="게시글 신고">
      {done ? (
        <div>
          <p className="text-sm text-emerald-400">신고가 접수되었습니다.</p>
          <button onClick={onClose} className="mt-4 rounded-lg bg-sky-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-400">확인</button>
        </div>
      ) : (
        <div className="space-y-3">
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            maxLength={500}
            placeholder="신고 사유 (2~500자)"
            className="w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500"
          />
          {error && <p className="text-sm text-rose-400">{error}</p>}
          <div className="flex justify-end gap-2">
            <button onClick={onClose} className="rounded-lg border border-slate-700 px-4 py-1.5 text-sm text-slate-300 hover:bg-slate-800">취소</button>
            <button onClick={submit} disabled={busy} className="rounded-lg bg-rose-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-rose-400 disabled:opacity-60">
              {busy ? '접수 중…' : '신고'}
            </button>
          </div>
        </div>
      )}
    </Modal>
  )
}
