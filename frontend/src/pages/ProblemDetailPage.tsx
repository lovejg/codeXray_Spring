import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ExternalLink, Pencil, FileText } from 'lucide-react'
import { problemsApi } from '../api/problems'
import { solutionsApi } from '../api/solutions'
import { useAuthStore } from '../store/authStore'
import Spinner from '../components/common/Spinner'
import EmptyState from '../components/common/EmptyState'
import TierBadge from '../components/common/TierBadge'
import LevelBadge from '../components/common/LevelBadge'
import SourceBadge from '../components/common/SourceBadge'
import TagBadge from '../components/common/TagBadge'
import FeedbackWidget from '../components/common/FeedbackWidget'
import AiHintPanel from '../components/common/AiHintPanel'

export default function ProblemDetailPage() {
  const { id } = useParams()
  const problemId = Number(id)
  const user = useAuthStore((s) => s.user)

  const { data: problem, isLoading, isError } = useQuery({
    queryKey: ['problem', problemId],
    queryFn: () => problemsApi.get(problemId),
    enabled: Number.isFinite(problemId),
  })

  // 내 풀이 목록(FeedbackWidget과 같은 캐시키 → 중복 요청 없음). 이 문제 풀이 존재 여부로 CTA 결정.
  const { data: mySolutions } = useQuery({
    queryKey: ['solutions', 'mine-all'],
    queryFn: () => solutionsApi.list(),
    enabled: !!user,
  })
  const mySolution = mySolutions?.find((s) => s.problem.id === problemId) ?? null

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !problem) return <EmptyState tone="error" message="문제를 찾을 수 없습니다." />

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/problems" className="text-sm text-slate-500 hover:text-slate-300">← 문제 목록</Link>

      <div className="glass-card mt-3 p-6">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-2xl font-bold text-white">{problem.title}</h1>
          <a
            href={problem.link}
            target="_blank"
            rel="noreferrer"
            className="btn-ghost shrink-0"
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

      {/* 풀이 액션 — 상황에 따라 하나만 노출(작성 or 보기) */}
      {user && (
        <div className="mt-4">
          {mySolution ? (
            <Link to={`/solutions/${mySolution.id}`} className="btn-primary">
              <FileText size={16} /> 내 풀이 보기
            </Link>
          ) : (
            <Link to={`/solutions/new?problemId=${problem.id}`} className="btn-primary">
              <Pencil size={16} /> 이 문제 풀이 작성
            </Link>
          )}
        </div>
      )}

      {/* AI 힌트 — 안 풀릴 때 정답 없이 단계별 힌트 */}
      {user && (
        <div className="mt-4">
          <AiHintPanel problemId={problem.id} />
        </div>
      )}

      {/* 난이도 피드백 */}
      <section className="glass-card mt-4 p-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-300">체감 난이도 피드백</h2>
        <FeedbackWidget problemId={problem.id} />
      </section>
    </div>
  )
}
