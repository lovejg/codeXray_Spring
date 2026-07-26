import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ratingsApi } from '../../api/ratings'
import { solutionsApi } from '../../api/solutions'
import { useAuthStore } from '../../store/authStore'
import { apiErrorMessage } from '../../lib/apiError'
import { useState } from 'react'

// 난이도 체감 피드백(0~5). "이 문제의 풀이를 등록한 사람"만 남길 수 있다.
// 제출 시 문제 티어가 재계산되므로 problem 쿼리도 무효화.
export default function FeedbackWidget({ problemId }: { problemId: number }) {
  const user = useAuthStore((s) => s.user)
  const qc = useQueryClient()
  const [error, setError] = useState('')

  // 내 풀이 목록에서 이 문제를 푼 적이 있는지 확인(게이팅용)
  const { data: mySolutions, isLoading: loadingSolutions } = useQuery({
    queryKey: ['solutions', 'mine-all'],
    queryFn: () => solutionsApi.list(),
    enabled: !!user,
  })
  const hasSolution = !!mySolutions?.some((s) => s.problem.id === problemId)

  const { data: mine } = useQuery({
    queryKey: ['feedback', problemId],
    queryFn: () => ratingsApi.getMyFeedback(problemId),
    enabled: !!user && hasSolution,
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

  if (loadingSolutions) {
    return <p className="text-sm text-slate-500">확인 중…</p>
  }

  // 풀이를 등록하지 않았으면 피드백 대신 안내만(등록 버튼은 위쪽 단일 액션 버튼이 담당)
  if (!hasSolution) {
    return (
      <p className="text-sm text-slate-400">
        위의 <span className="text-slate-200">‘이 문제 풀이 작성’</span>으로 풀이를 등록하면 체감 난이도를 남길 수 있어요.
      </p>
    )
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
              className={`h-10 w-10 rounded-xl border text-sm font-semibold transition ${
                active
                  ? 'border-teal-500 bg-teal-500/20 text-teal-300'
                  : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/25'
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
