import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { recommendApi } from '../../api/recommend'
import { useAuthStore } from '../../store/authStore'
import TierBadge from './TierBadge'

// 문제 목록 상단에 뜨는 개인 추천(약점 보강 + 적정 난이도). 비로그인/추천 없으면 렌더 안 함.
export default function RecommendationStrip() {
  const user = useAuthStore((s) => s.user)
  const { data } = useQuery({
    queryKey: ['recommendations'],
    queryFn: recommendApi.list,
    enabled: !!user,
    staleTime: 1000 * 60 * 5,
  })

  if (!user || !data || data.length === 0) return null

  return (
    <section className="mb-6">
      <p className="section-title mb-3">
        // 추천 문제 <span className="text-slate-600">약점 보강 + 적정 난이도</span>
      </p>
      <div className="grid gap-2 sm:grid-cols-2">
        {data.map((r) => (
          <Link
            key={r.problem.id}
            to={`/problems/${r.problem.id}`}
            className="glass-card flex items-center gap-2 p-3 transition hover:-translate-y-0.5 hover:border-teal-400/30 hover:bg-white/[0.04]"
          >
            <span className="font-mono text-teal-400">❯</span>
            <span className="truncate text-sm font-medium text-slate-100">{r.problem.title}</span>
            <TierBadge tier={r.problem.tier} />
            <span
              className={`ml-auto shrink-0 rounded border px-1.5 py-0.5 font-mono text-[11px] ${
                r.reason === '약점 보강'
                  ? 'border-rose-500/30 text-rose-300'
                  : 'border-teal-500/30 text-teal-300'
              }`}
            >
              {r.reason}
              {r.tag ? ` · #${r.tag}` : ''}
            </span>
          </Link>
        ))}
      </div>
    </section>
  )
}
