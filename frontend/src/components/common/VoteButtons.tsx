import { useState } from 'react'
import { ChevronUp, ChevronDown } from 'lucide-react'
import { communityApi } from '../../api/community'
import type { VoteSummary } from '../../types'
import { useAuthStore } from '../../store/authStore'
import { apiErrorMessage } from '../../lib/apiError'

// 추천/비추천. 같은 값을 다시 누르면 철회.
export default function VoteButtons({ postId, initial }: { postId: number; initial: VoteSummary }) {
  const user = useAuthStore((s) => s.user)
  const [v, setV] = useState<VoteSummary>(initial)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function cast(value: 1 | -1) {
    if (!user || busy) return
    setBusy(true)
    setError('')
    try {
      // 이미 같은 방향이면 철회
      const next = v.myVote === value ? await communityApi.removeVote(postId) : await communityApi.vote(postId, value)
      setV(next)
    } catch (err) {
      setError(apiErrorMessage(err, '투표에 실패했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col items-center">
      <button
        onClick={() => cast(1)}
        disabled={!user}
        className={`rounded p-1 transition ${v.myVote === 1 ? 'text-emerald-400' : 'text-slate-500 hover:text-slate-300'} disabled:opacity-40`}
        aria-label="추천"
      >
        <ChevronUp size={22} />
      </button>
      <span className={`text-sm font-bold ${v.score > 0 ? 'text-emerald-400' : v.score < 0 ? 'text-rose-400' : 'text-slate-400'}`}>
        {v.score}
      </span>
      <button
        onClick={() => cast(-1)}
        disabled={!user}
        className={`rounded p-1 transition ${v.myVote === -1 ? 'text-rose-400' : 'text-slate-500 hover:text-slate-300'} disabled:opacity-40`}
        aria-label="비추천"
      >
        <ChevronDown size={22} />
      </button>
      {error && <span className="mt-1 w-16 text-center text-[10px] text-rose-400">{error}</span>}
    </div>
  )
}
