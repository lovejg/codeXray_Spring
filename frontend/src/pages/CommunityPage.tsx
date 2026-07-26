import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { MessageSquare, Plus, Lock } from 'lucide-react'
import { communityApi } from '../api/community'
import { COMMUNITY_POST_TYPES } from '../types'
import { useAuthStore } from '../store/authStore'
import { timeAgo } from '../lib/date'
import PageHeader from '../components/common/PageHeader'
import Spinner from '../components/common/Spinner'
import PostTypeBadge from '../components/common/PostTypeBadge'
import AuthorName from '../components/common/AuthorName'

export default function CommunityPage() {
  const user = useAuthStore((s) => s.user)
  const [sort, setSort] = useState<'recent' | 'votes'>('recent')

  const { data, isLoading } = useQuery({
    queryKey: ['community', 'list', sort],
    queryFn: () => communityApi.listPosts({ types: COMMUNITY_POST_TYPES.join(','), sort }),
  })

  return (
    <div>
      <PageHeader title="커뮤니티" subtitle="문제 풀이를 공유하고 서로 질문에 답해보세요.">
        <div className="flex overflow-hidden rounded-xl border border-white/10 text-sm">
          {(['recent', 'votes'] as const).map((s) => (
            <button
              key={s}
              onClick={() => setSort(s)}
              className={`px-3.5 py-2 transition ${sort === s ? 'bg-white/10 text-white' : 'text-slate-400 hover:bg-white/5'}`}
            >
              {s === 'recent' ? '최신순' : '추천순'}
            </button>
          ))}
        </div>
        {user && (
          <Link to="/community/new" className="btn-primary">
            <Plus size={16} /> 글쓰기
          </Link>
        )}
      </PageHeader>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <p className="py-16 text-center text-sm text-slate-500">아직 게시글이 없습니다.</p>}

      <div className="glass-card divide-y divide-white/5 overflow-hidden">
        {data?.map((p) => (
          <Link key={p.id} to={`/community/${p.id}`} className="flex items-center gap-3 px-5 py-3.5 transition hover:bg-white/5">
            <div className="flex w-10 shrink-0 flex-col items-center">
              <span className={`text-sm font-bold ${p.votes.score > 0 ? 'text-emerald-400' : p.votes.score < 0 ? 'text-rose-400' : 'text-slate-500'}`}>
                {p.votes.score}
              </span>
              <span className="text-[10px] text-slate-600">추천</span>
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <PostTypeBadge type={p.type} />
                {p.isPrivate && <Lock size={12} className="text-slate-500" />}
                <span className="truncate font-medium text-slate-100">{p.title}</span>
              </div>
              <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
                <AuthorName author={p.author} />
                <span>·</span>
                <span>{timeAgo(p.createdAt)}</span>
                {p.problem && <><span>·</span><span className="text-slate-600">{p.problem.title}</span></>}
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-1 text-xs text-slate-500">
              <MessageSquare size={14} /> {p.commentCount}
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
