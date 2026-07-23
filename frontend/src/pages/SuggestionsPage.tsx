import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { communityApi } from '../api/community'
import { SUGGESTION_POST_TYPES, STATUS_LABEL, type SuggestionStatus } from '../types'
import { useAuthStore } from '../store/authStore'
import { timeAgo } from '../lib/date'
import Spinner from '../components/common/Spinner'
import PostTypeBadge from '../components/common/PostTypeBadge'
import StatusBadge from '../components/common/StatusBadge'
import AuthorName from '../components/common/AuthorName'

export default function SuggestionsPage() {
  const user = useAuthStore((s) => s.user)
  const [status, setStatus] = useState<SuggestionStatus | ''>('')

  const { data, isLoading } = useQuery({
    queryKey: ['suggestions', 'list', status],
    queryFn: () => communityApi.listPosts({
      types: SUGGESTION_POST_TYPES.join(','),
      status: status || undefined,
      sort: 'recent',
    }),
  })

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-bold text-white">건의사항</h1>
        <select value={status} onChange={(e) => setStatus(e.target.value as SuggestionStatus | '')} className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500">
          <option value="">전체 상태</option>
          <option value="IN_PROGRESS">{STATUS_LABEL.IN_PROGRESS}</option>
          <option value="RESOLVED">{STATUS_LABEL.RESOLVED}</option>
        </select>
        {user && (
          <Link to="/suggestions/new" className="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-sky-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-sky-400">
            <Plus size={15} /> 건의하기
          </Link>
        )}
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <p className="py-16 text-center text-sm text-slate-500">건의사항이 없습니다.</p>}

      <div className="divide-y divide-slate-800 overflow-hidden rounded-xl border border-slate-800">
        {data?.map((p) => (
          <Link key={p.id} to={`/suggestions/${p.id}`} className="flex items-center gap-3 px-4 py-3 hover:bg-slate-900/40">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <PostTypeBadge type={p.type} />
                <StatusBadge status={p.status} />
                <span className="truncate font-medium text-slate-100">{p.title}</span>
              </div>
              <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
                <AuthorName author={p.author} />
                <span>·</span>
                <span>{timeAgo(p.createdAt)}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
