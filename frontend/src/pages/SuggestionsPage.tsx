import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { communityApi } from '../api/community'
import { SUGGESTION_POST_TYPES, STATUS_LABEL, type SuggestionStatus } from '../types'
import { useAuthStore } from '../store/authStore'
import { timeAgo } from '../lib/date'
import PageHeader from '../components/common/PageHeader'
import Spinner from '../components/common/Spinner'
import EmptyState from '../components/common/EmptyState'
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
      <PageHeader title="건의사항" subtitle="개선 아이디어나 버그를 제보하면 관리자가 확인합니다.">
        <select value={status} onChange={(e) => setStatus(e.target.value as SuggestionStatus | '')} className="select-field">
          <option value="">전체 상태</option>
          <option value="IN_PROGRESS">{STATUS_LABEL.IN_PROGRESS}</option>
          <option value="RESOLVED">{STATUS_LABEL.RESOLVED}</option>
        </select>
        {user && (
          <Link to="/suggestions/new" className="btn-primary">
            <Plus size={16} /> 건의하기
          </Link>
        )}
      </PageHeader>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <EmptyState message="건의사항이 없습니다" />}

      <div className="glass-card divide-y divide-white/5 overflow-hidden">
        {data?.map((p) => (
          <Link key={p.id} to={`/suggestions/${p.id}`} className="flex items-center gap-3 px-5 py-3.5 transition hover:bg-white/5">
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
