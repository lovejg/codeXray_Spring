import { useState, useEffect } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Pencil, Trash2 } from 'lucide-react'
import { communityApi } from '../api/community'
import { useAuthStore, useIsAdmin } from '../store/authStore'
import { timeAgo } from '../lib/date'
import { STATUS_LABEL, type SuggestionStatus } from '../types'
import Spinner from '../components/common/Spinner'
import Markdown from '../components/common/Markdown'
import PostTypeBadge from '../components/common/PostTypeBadge'
import StatusBadge from '../components/common/StatusBadge'
import AuthorName from '../components/common/AuthorName'

export default function SuggestionPostPage() {
  const { id } = useParams()
  const postId = Number(id)
  const user = useAuthStore((s) => s.user)
  const isAdmin = useIsAdmin()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: post, isLoading, isError } = useQuery({
    queryKey: ['community', 'post', postId],
    queryFn: () => communityApi.getPost(postId),
    enabled: Number.isFinite(postId),
  })

  const del = useMutation({
    mutationFn: () => communityApi.deletePost(postId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['suggestions'] }); navigate('/suggestions') },
  })

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !post) return <p className="py-10 text-center text-sm text-rose-400">게시글을 볼 수 없습니다.</p>

  const isOwner = user && post.author.id === user.id

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/suggestions" className="text-sm text-slate-500 hover:text-slate-300">← 건의사항</Link>

      <div className="mt-3 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
        <div className="flex flex-wrap items-center gap-2">
          <PostTypeBadge type={post.type} />
          <StatusBadge status={post.status} />
        </div>
        <h1 className="mt-2 text-2xl font-bold text-white">{post.title}</h1>
        <div className="mt-1.5 flex items-center gap-2 text-xs text-slate-500">
          <AuthorName author={post.author} />
          <span>·</span>
          <span>{timeAgo(post.createdAt)}</span>
        </div>

        <div className="mt-4">
          <Markdown>{post.content}</Markdown>
        </div>

        {(isOwner || isAdmin) && (
          <div className="mt-5 flex items-center gap-3 text-xs">
            {isOwner && (
              <>
                <Link to={`/community/${post.id}/edit`} className="inline-flex items-center gap-1 text-slate-400 hover:text-sky-400"><Pencil size={13} /> 수정</Link>
                <button onClick={() => { if (confirm('삭제할까요?')) del.mutate() }} className="inline-flex items-center gap-1 text-slate-400 hover:text-rose-400"><Trash2 size={13} /> 삭제</button>
              </>
            )}
            {isAdmin && !isOwner && (
              <button onClick={() => { if (confirm('삭제할까요?')) del.mutate() }} className="inline-flex items-center gap-1 text-slate-400 hover:text-rose-400"><Trash2 size={13} /> 삭제(관리자)</button>
            )}
          </div>
        )}
      </div>

      {/* 관리자 공식 답변 */}
      {post.adminReply && (
        <div className="mt-4 rounded-2xl border border-sky-800/50 bg-sky-950/30 p-6">
          <p className="mb-2 text-sm font-semibold text-sky-300">관리자 답변</p>
          <Markdown>{post.adminReply}</Markdown>
          {post.adminReplyAt && <p className="mt-2 text-xs text-slate-500">{timeAgo(post.adminReplyAt)}</p>}
        </div>
      )}

      {/* 관리자 컨트롤 */}
      {isAdmin && <AdminControls postId={post.id} status={post.status} adminReply={post.adminReply} />}
    </div>
  )
}

function AdminControls({ postId, status, adminReply }: { postId: number; status?: SuggestionStatus | null; adminReply?: string | null }) {
  const qc = useQueryClient()
  const [reply, setReply] = useState(adminReply ?? '')
  useEffect(() => setReply(adminReply ?? ''), [adminReply])

  const setStatus = useMutation({
    mutationFn: (s: SuggestionStatus) => communityApi.updateStatus(postId, s),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['community', 'post', postId] }),
  })
  const saveReply = useMutation({
    mutationFn: () => communityApi.updateAdminReply(postId, reply),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['community', 'post', postId] }),
  })

  return (
    <div className="mt-4 rounded-2xl border border-amber-800/40 bg-amber-950/20 p-6">
      <p className="mb-3 text-sm font-semibold text-amber-300">관리자 도구</p>

      <div className="mb-4 flex items-center gap-2">
        <span className="text-sm text-slate-400">상태:</span>
        {(['IN_PROGRESS', 'RESOLVED'] as SuggestionStatus[]).map((s) => (
          <button
            key={s}
            onClick={() => setStatus.mutate(s)}
            className={`rounded-lg border px-3 py-1.5 text-sm ${status === s ? 'border-sky-500 bg-sky-500/20 text-sky-300' : 'border-slate-700 text-slate-300 hover:bg-slate-800'}`}
          >
            {STATUS_LABEL[s]}
          </button>
        ))}
      </div>

      <label className="mb-1.5 block text-sm text-slate-400">공식 답변 (빈 값 저장 시 삭제)</label>
      <textarea value={reply} onChange={(e) => setReply(e.target.value)} rows={4} className="w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500" />
      <div className="mt-2 flex justify-end">
        <button onClick={() => saveReply.mutate()} disabled={saveReply.isPending} className="rounded-lg bg-sky-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-400 disabled:opacity-60">
          답변 저장
        </button>
      </div>
    </div>
  )
}
