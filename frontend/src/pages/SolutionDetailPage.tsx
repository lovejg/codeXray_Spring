import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Star, Pencil, Trash2, ArrowLeft } from 'lucide-react'
import { solutionsApi } from '../api/solutions'
import { languageLabel } from '../lib/languages'
import Spinner from '../components/common/Spinner'
import EmptyState from '../components/common/EmptyState'
import TierBadge from '../components/common/TierBadge'
import CodeBlock from '../components/common/CodeBlock'
import AiAnalyzePanel from '../components/common/AiAnalyzePanel'

// 풀이 상세: 코드/메모/AI 분석을 전용 페이지에서 온전히 표시(카드 안에 우겨넣지 않음).
export default function SolutionDetailPage() {
  const { id } = useParams()
  const solutionId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: s, isLoading, isError } = useQuery({
    queryKey: ['solution', solutionId],
    queryFn: () => solutionsApi.get(solutionId),
    enabled: Number.isFinite(solutionId),
  })

  const star = useMutation({
    mutationFn: () => solutionsApi.toggleStar(solutionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['solution', solutionId] })
      qc.invalidateQueries({ queryKey: ['solutions'] })
    },
  })
  const remove = useMutation({
    mutationFn: () => solutionsApi.remove(solutionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['solutions'] })
      navigate('/solutions')
    },
  })

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !s) return <EmptyState tone="error" message="풀이를 찾을 수 없습니다." />

  const memo = s.memo
  const hasMemo = memo && (memo.wrongReason || memo.logic || memo.keyFunctions || memo.freeNote)

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/solutions" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-300">
        <ArrowLeft size={14} /> 내 풀이
      </Link>

      <div className="mt-4 flex items-center gap-2">
        <button onClick={() => star.mutate()} className={s.starred ? 'text-amber-400' : 'text-slate-600 hover:text-slate-400'} aria-label="별표">
          <Star size={20} fill={s.starred ? 'currentColor' : 'none'} />
        </button>
        <Link to={`/problems/${s.problem.id}`} className="text-xl font-bold text-slate-100 transition hover:text-teal-300">
          {s.problem.title}
        </Link>
        <TierBadge tier={s.problem.tier} />
        <span className="ml-auto text-xs text-slate-500">{languageLabel(s.language)}</span>
        <Link to={`/solutions/${s.id}/edit`} className="text-slate-500 transition hover:text-teal-300" aria-label="수정"><Pencil size={16} /></Link>
        <button onClick={() => { if (confirm('이 풀이를 삭제할까요?')) remove.mutate() }} className="text-slate-500 transition hover:text-rose-400" aria-label="삭제"><Trash2 size={16} /></button>
      </div>

      <div className="mt-5 space-y-4">
        <CodeBlock code={s.code} language={s.language} />
        <AiAnalyzePanel code={s.code} language={s.language} problemTitle={s.problem.title} />

        {hasMemo && (
          <div className="glass-card p-5 text-sm">
            <p className="mb-2 text-xs font-semibold text-slate-400">메모</p>
            <div className="space-y-1.5">
              {memo?.wrongReason && <MemoRow label="틀린 이유" value={memo.wrongReason} />}
              {memo?.logic && <MemoRow label="풀이 로직" value={memo.logic} />}
              {memo?.keyFunctions && <MemoRow label="핵심 함수" value={memo.keyFunctions} />}
              {memo?.freeNote && <MemoRow label="자유 메모" value={memo.freeNote} />}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function MemoRow({ label, value }: { label: string; value: string }) {
  return (
    <p className="text-slate-300">
      <span className="text-slate-500">{label}: </span>
      {value}
    </p>
  )
}
