import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ExternalLink, Pencil } from 'lucide-react'
import { problemsApi } from '../api/problems'
import { useAuthStore } from '../store/authStore'
import Spinner from '../components/common/Spinner'
import TierBadge from '../components/common/TierBadge'
import LevelBadge from '../components/common/LevelBadge'
import SourceBadge from '../components/common/SourceBadge'
import TagBadge from '../components/common/TagBadge'
import FeedbackWidget from '../components/common/FeedbackWidget'

export default function ProblemDetailPage() {
  const { id } = useParams()
  const problemId = Number(id)
  const user = useAuthStore((s) => s.user)

  const { data: problem, isLoading, isError } = useQuery({
    queryKey: ['problem', problemId],
    queryFn: () => problemsApi.get(problemId),
    enabled: Number.isFinite(problemId),
  })

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !problem) return <p className="py-10 text-center text-sm text-rose-400">문제를 찾을 수 없습니다.</p>

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/problems" className="text-sm text-slate-500 hover:text-slate-300">← 문제 목록</Link>

      <div className="mt-3 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-2xl font-bold text-white">{problem.title}</h1>
          <a
            href={problem.link}
            target="_blank"
            rel="noreferrer"
            className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800"
          >
            문제 풀러가기 <ExternalLink size={14} />
          </a>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <SourceBadge source={problem.source} />
          <LevelBadge level={problem.level} />
          <TierBadge tier={problem.tier} size="md" />
          {problem.acceptanceRate != null && (
            <span className="text-xs text-slate-500">정답률 {problem.acceptanceRate.toFixed(1)}%</span>
          )}
        </div>

        {problem.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {problem.tags.map((t) => <TagBadge key={t.id} name={t.name} />)}
          </div>
        )}
      </div>

      {/* 난이도 피드백 */}
      <section className="mt-4 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-300">체감 난이도 피드백</h2>
        <FeedbackWidget problemId={problem.id} />
      </section>

      {/* 내 풀이 작성 */}
      {user && (
        <div className="mt-4">
          <Link
            to={`/solutions/new?problemId=${problem.id}`}
            className="inline-flex items-center gap-1.5 rounded-lg bg-sky-500 px-4 py-2 text-sm font-medium text-white hover:bg-sky-400"
          >
            <Pencil size={15} /> 이 문제 풀이 작성
          </Link>
        </div>
      )}
    </div>
  )
}
