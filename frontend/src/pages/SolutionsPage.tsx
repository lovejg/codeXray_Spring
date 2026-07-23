import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Star, Pencil, Trash2, Plus } from 'lucide-react'
import { solutionsApi } from '../api/solutions'
import type { Solution } from '../types'
import { languageLabel } from '../lib/languages'
import Spinner from '../components/common/Spinner'
import TierBadge from '../components/common/TierBadge'
import CodeBlock from '../components/common/CodeBlock'
import AiAnalyzePanel from '../components/common/AiAnalyzePanel'

export default function SolutionsPage() {
  const [starredOnly, setStarredOnly] = useState(false)
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['solutions', { starredOnly }],
    queryFn: () => solutionsApi.list(starredOnly ? true : undefined),
  })

  const star = useMutation({
    mutationFn: (id: number) => solutionsApi.toggleStar(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['solutions'] }),
  })
  const remove = useMutation({
    mutationFn: (id: number) => solutionsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['solutions'] }),
  })

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-bold text-white">내 풀이</h1>
        <label className="ml-auto flex cursor-pointer items-center gap-1.5 text-sm text-slate-400">
          <input type="checkbox" checked={starredOnly} onChange={(e) => setStarredOnly(e.target.checked)} className="accent-sky-500" />
          별표만
        </label>
        <Link to="/solutions/new" className="inline-flex items-center gap-1.5 rounded-lg bg-sky-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-sky-400">
          <Plus size={15} /> 풀이 등록
        </Link>
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}

      {data && data.length === 0 && (
        <p className="py-16 text-center text-sm text-slate-500">아직 등록한 풀이가 없습니다.</p>
      )}

      <div className="space-y-4">
        {data?.map((s) => (
          <SolutionCard
            key={s.id}
            s={s}
            onStar={() => star.mutate(s.id)}
            onDelete={() => { if (confirm('이 풀이를 삭제할까요?')) remove.mutate(s.id) }}
          />
        ))}
      </div>
    </div>
  )
}

function SolutionCard({ s, onStar, onDelete }: { s: Solution; onStar: () => void; onDelete: () => void }) {
  const [open, setOpen] = useState(false)
  const memo = s.memo
  const hasMemo = memo && (memo.wrongReason || memo.logic || memo.keyFunctions || memo.freeNote)

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <div className="flex items-center gap-2">
        <button onClick={onStar} className={s.starred ? 'text-amber-400' : 'text-slate-600 hover:text-slate-400'} aria-label="별표">
          <Star size={18} fill={s.starred ? 'currentColor' : 'none'} />
        </button>
        <Link to={`/problems/${s.problem.id}`} className="font-medium text-slate-100 hover:text-sky-400">
          {s.problem.title}
        </Link>
        <TierBadge tier={s.problem.tier} />
        <span className="ml-auto text-xs text-slate-500">{languageLabel(s.language)}</span>
        <Link to={`/solutions/${s.id}/edit`} className="text-slate-500 hover:text-sky-400" aria-label="수정"><Pencil size={16} /></Link>
        <button onClick={onDelete} className="text-slate-500 hover:text-rose-400" aria-label="삭제"><Trash2 size={16} /></button>
      </div>

      <button onClick={() => setOpen((o) => !o)} className="mt-2 text-xs text-slate-500 hover:text-slate-300">
        {open ? '코드 접기' : '코드 보기'}
      </button>

      {open && (
        <div className="mt-2 space-y-3">
          <CodeBlock code={s.code} language={s.language} />
          <AiAnalyzePanel code={s.code} language={s.language} problemTitle={s.problem.title} />
          {hasMemo && (
            <div className="rounded-lg border border-slate-800 bg-slate-950/40 p-3 text-sm">
              <p className="mb-1 text-xs font-semibold text-slate-400">메모</p>
              {memo?.wrongReason && <MemoRow label="틀린 이유" value={memo.wrongReason} />}
              {memo?.logic && <MemoRow label="풀이 로직" value={memo.logic} />}
              {memo?.keyFunctions && <MemoRow label="핵심 함수" value={memo.keyFunctions} />}
              {memo?.freeNote && <MemoRow label="자유 메모" value={memo.freeNote} />}
            </div>
          )}
        </div>
      )}
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
