import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ratingsApi } from '../../api/ratings'
import { useAuthStore } from '../../store/authStore'
import { apiErrorMessage } from '../../lib/apiError'
import { useState } from 'react'

// 난이도 체감 피드백(0~5). 제출 시 문제 티어가 재계산되므로 problem 쿼리도 무효화.
export default function FeedbackWidget({ problemId }: { problemId: number }) {
  const user = useAuthStore((s) => s.user)
  const qc = useQueryClient()
  const [error, setError] = useState('')

  const { data: mine } = useQuery({
    queryKey: ['feedback', problemId],
    queryFn: () => ratingsApi.getMyFeedback(problemId),
    enabled: !!user,
  })

  const mutation = useMutation({
    mutationFn: (level: number) => ratingsApi.submitFeedback(problemId, level),
    onSuccess: () => {
      setError('')
      qc.invalidateQueries({ queryKey: ['feedback', problemId] })
      qc.invalidateQueries({ queryKey: ['problem', problemId] })
    },
    onError: (err) => setError(apiErrorMessage(err, '피드백 제출에 실패했습니다.')),
  })

  if (!user) {
    return <p className="text-sm text-slate-500">난이도 피드백은 로그인 후 남길 수 있어요.</p>
  }

  return (
    <div>
      <div className="flex items-center gap-2">
        {[0, 1, 2, 3, 4, 5].map((lv) => {
          const active = mine?.level === lv
          return (
            <button
              key={lv}
              onClick={() => mutation.mutate(lv)}
              disabled={mutation.isPending}
              className={`h-9 w-9 rounded-lg border text-sm font-semibold transition ${
                active
                  ? 'border-sky-500 bg-sky-500/20 text-sky-300'
                  : 'border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-500'
              }`}
            >
              {lv}
            </button>
          )
        })}
      </div>
      <p className="mt-2 text-xs text-slate-500">
        0(매우 쉬움) ~ 5(매우 어려움){mine ? ` · 내 평가: ${mine.level}` : ''}
      </p>
      {error && <p className="mt-1 text-xs text-rose-400">{error}</p>}
    </div>
  )
}
