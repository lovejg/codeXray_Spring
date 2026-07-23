import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Lock, Pencil, Trash2, Flag, EyeOff } from 'lucide-react'
import { communityApi } from '../api/community'
import { useAuthStore, useIsAdmin } from '../store/authStore'
import { timeAgo } from '../lib/date'
import { VOTABLE_POST_TYPES } from '../types'
import Spinner from '../components/common/Spinner'
import Markdown from '../components/common/Markdown'
import PostTypeBadge from '../components/common/PostTypeBadge'
import AuthorName from '../components/common/AuthorName'
import VoteButtons from '../components/common/VoteButtons'
import ReportModal from '../components/common/ReportModal'
import { apiErrorMessage } from '../lib/apiError'

export default function CommunityPostPage() {
  const { id } = useParams()
  const postId = Number(id)
  const user = useAuthStore((s) => s.user)
  const isAdmin = useIsAdmin()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [reporting, setReporting] = useState(false)

  const { data: post, isLoading, isError } = useQuery({
    queryKey: ['community', 'post', postId],
    queryFn: () => communityApi.getPost(postId),
    enabled: Number.isFinite(postId),
  })

  const del = useMutation({
    mutationFn: () => communityApi.deletePost(postId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['community'] }); navigate('/community') },
  })
  const hide = useMutation({
    mutationFn: (hidden: boolean) => communityApi.hidePost(postId, hidden),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['community', 'post', postId] }),
  })

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !post) return <p className="py-10 text-center text-sm text-rose-400">게시글을 볼 수 없습니다.</p>

  const isOwner = user && post.author.id === user.id
  const votable = VOTABLE_POST_TYPES.includes(post.type)

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/community" className="text-sm text-slate-500 hover:text-slate-300">← 커뮤니티</Link>

      <div className="mt-3 flex gap-4 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
        {votable && <VoteButtons postId={post.id} initial={post.votes} />}

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <PostTypeBadge type={post.type} />
            {post.isPrivate && <span className="inline-flex items-center gap-1 text-xs text-slate-500"><Lock size={12} /> 비공개</span>}
            {post.hidden && <span className="inline-flex items-center gap-1 text-xs text-rose-400"><EyeOff size={12} /> 숨김</span>}
          </div>
          <h1 className="mt-2 text-2xl font-bold text-white">{post.title}</h1>
          <div className="mt-1.5 flex items-center gap-2 text-xs text-slate-500">
            <AuthorName author={post.author} />
            <span>·</span>
            <span>{timeAgo(post.createdAt)}</span>
            {post.problem && <><span>·</span><span className="text-slate-600">{post.problem.title}</span></>}
          </div>

          <div className="mt-4">
            <Markdown>{post.content}</Markdown>
          </div>

          {/* 액션 */}
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
            {isAdmin && (
              <button onClick={() => hide.mutate(!post.hidden)} className="inline-flex items-center gap-1 text-slate-400 hover:text-amber-400">
                <EyeOff size={13} /> {post.hidden ? '숨김 해제' : '숨김'}
              </button>
            )}
            {user && !isOwner && (
              <button onClick={() => setReporting(true)} className="inline-flex items-center gap-1 text-slate-400 hover:text-rose-400"><Flag size={13} /> 신고</button>
            )}
          </div>
        </div>
      </div>

      {/* 댓글 */}
      <CommentSection postId={post.id} comments={post.comments} canComment={!!user && !post.hidden} />

      {reporting && <ReportModal postId={post.id} onClose={() => setReporting(false)} />}
    </div>
  )
}

function CommentSection({ postId, comments, canComment }: { postId: number; comments: import('../types').Comment[]; canComment: boolean }) {
  const user = useAuthStore((s) => s.user)
  const isAdmin = useIsAdmin()
  const qc = useQueryClient()
  const [content, setContent] = useState('')
  const [error, setError] = useState('')

  const add = useMutation({
    mutationFn: () => communityApi.addComment(postId, content.trim()),
    onSuccess: () => { setContent(''); setError(''); qc.invalidateQueries({ queryKey: ['community', 'post', postId] }) },
    onError: (err) => setError(apiErrorMessage(err, '댓글 등록에 실패했습니다.')),
  })
  const del = useMutation({
    mutationFn: (id: number) => communityApi.deleteComment(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['community', 'post', postId] }),
  })

  return (
    <section className="mt-4 rounded-2xl border border-slate-800 bg-slate-900/40 p-6">
      <h2 className="mb-4 text-sm font-semibold text-slate-300">댓글 {comments.length}</h2>

      <div className="space-y-3">
        {comments.map((c) => {
          const owner = user && c.author.id === user.id
          return (
            <div key={c.id} className="rounded-lg border border-slate-800 bg-slate-950/40 p-3">
              <div className="flex items-center gap-2 text-xs text-slate-500">
                <AuthorName author={c.author} />
                <span>·</span>
                <span>{timeAgo(c.createdAt)}</span>
                {(owner || isAdmin) && (
                  <button onClick={() => { if (confirm('댓글을 삭제할까요?')) del.mutate(c.id) }} className="ml-auto text-slate-500 hover:text-rose-400"><Trash2 size={13} /></button>
                )}
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm text-slate-200">{c.content}</p>
            </div>
          )
        })}
        {comments.length === 0 && <p className="text-sm text-slate-500">첫 댓글을 남겨보세요.</p>}
      </div>

      {canComment ? (
        <div className="mt-4">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={2}
            maxLength={2000}
            placeholder="댓글 작성"
            className="w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500"
          />
          {error && <p className="mt-1 text-xs text-rose-400">{error}</p>}
          <div className="mt-2 flex justify-end">
            <button onClick={() => content.trim() && add.mutate()} disabled={add.isPending || !content.trim()} className="rounded-lg bg-sky-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-400 disabled:opacity-50">
              등록
            </button>
          </div>
        </div>
      ) : (
        <p className="mt-4 text-sm text-slate-500">댓글을 작성하려면 로그인이 필요합니다.</p>
      )}
    </section>
  )
}
