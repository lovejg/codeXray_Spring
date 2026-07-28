import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Star, Pencil, Trash2, Plus, ChevronRight } from 'lucide-react'
import { solutionsApi } from '../api/solutions'
import type { Solution } from '../types'
import { languageLabel } from '../lib/languages'
import PageHeader from '../components/common/PageHeader'
import Spinner from '../components/common/Spinner'
import EmptyState from '../components/common/EmptyState'
import TierBadge from '../components/common/TierBadge'

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
      <PageHeader title="내 풀이" subtitle="등록한 풀이와 메모, 별표한 문제를 모아봅니다.">
        <label className="flex cursor-pointer items-center gap-1.5 text-sm text-slate-400">
          <input type="checkbox" checked={starredOnly} onChange={(e) => setStarredOnly(e.target.checked)} className="accent-teal-500" />
          별표만
        </label>
        <Link to="/solutions/new" className="btn-primary">
          <Plus size={16} /> 풀이 등록
        </Link>
      </PageHeader>

      {isLoading && <Spinner label="불러오는 중…" />}

      {data && data.length === 0 && (
        <EmptyState message="아직 등록한 풀이가 없습니다" hint="문제를 풀고 풀이를 등록해 보세요." />
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
  const hasMemo = s.memo && (s.memo.wrongReason || s.memo.logic || s.memo.keyFunctions || s.memo.freeNote)

  return (
    <div className="glass-card flex items-center gap-3 p-4 transition hover:-translate-y-0.5 hover:border-teal-400/30 hover:bg-white/[0.07]">
      <button onClick={onStar} className={s.starred ? 'text-amber-400' : 'text-slate-600 hover:text-slate-400'} aria-label="별표">
        <Star size={18} fill={s.starred ? 'currentColor' : 'none'} />
      </button>

      {/* 카드 대부분 영역을 클릭하면 상세 페이지로 이동 */}
      <Link to={`/solutions/${s.id}`} className="flex min-w-0 flex-1 items-center gap-2">
        <span className="truncate font-medium text-slate-100">{s.problem.title}</span>
        <TierBadge tier={s.problem.tier} />
        {hasMemo && <span className="rounded-md bg-teal-500/15 px-1.5 py-0.5 text-[10px] font-medium text-teal-300">메모</span>}
      </Link>

      <span className="text-xs text-slate-500">{languageLabel(s.language)}</span>
      <Link to={`/solutions/${s.id}/edit`} className="text-slate-500 transition hover:text-teal-300" aria-label="수정"><Pencil size={16} /></Link>
      <button onClick={onDelete} className="text-slate-500 transition hover:text-rose-400" aria-label="삭제"><Trash2 size={16} /></button>
      <Link to={`/solutions/${s.id}`} className="text-slate-600 transition hover:text-slate-300" aria-label="상세"><ChevronRight size={18} /></Link>
    </div>
  )
}
