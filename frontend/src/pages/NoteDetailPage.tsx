import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Pencil, Trash2, ArrowLeft } from 'lucide-react'
import { notesApi } from '../api/notes'
import { languageLabel } from '../lib/languages'
import Spinner from '../components/common/Spinner'
import NoteTypeBadge from '../components/common/NoteTypeBadge'
import TagBadge from '../components/common/TagBadge'
import Markdown from '../components/common/Markdown'
import NoteFormModal from './NoteFormModal'

// 노트 상세: 본문 전체를 마크다운으로 온전히 표시(카드 프리뷰처럼 잘리지 않음).
export default function NoteDetailPage() {
  const { id } = useParams()
  const noteId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)

  const { data: n, isLoading, isError } = useQuery({
    queryKey: ['note', noteId],
    queryFn: () => notesApi.get(noteId),
    enabled: Number.isFinite(noteId),
  })

  const remove = useMutation({
    mutationFn: () => notesApi.remove(noteId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notes'] })
      navigate('/notes')
    },
  })

  if (isLoading) return <Spinner label="불러오는 중…" />
  if (isError || !n) return <p className="py-10 text-center text-sm text-rose-400">노트를 찾을 수 없습니다.</p>

  return (
    <div className="mx-auto max-w-3xl">
      <Link to="/notes" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-300">
        <ArrowLeft size={14} /> 노트
      </Link>

      <div className="mt-4 flex items-center gap-2">
        <NoteTypeBadge type={n.type} />
        <h1 className="min-w-0 flex-1 truncate text-xl font-bold text-slate-100">{n.title}</h1>
        {n.language && (
          <span className="rounded-md bg-white/5 px-1.5 py-0.5 text-[11px] text-slate-400">{languageLabel(n.language)}</span>
        )}
        <button onClick={() => setEditing(true)} className="text-slate-500 transition hover:text-teal-300" aria-label="수정"><Pencil size={16} /></button>
        <button onClick={() => { if (confirm('이 노트를 삭제할까요?')) remove.mutate() }} className="text-slate-500 transition hover:text-rose-400" aria-label="삭제"><Trash2 size={16} /></button>
      </div>

      {n.tags.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1">
          {n.tags.map((t) => <TagBadge key={t} name={t} />)}
        </div>
      )}

      <div className="glass-card mt-5 p-6">
        <Markdown>{n.body}</Markdown>
      </div>

      {editing && (
        <NoteFormModal
          note={n}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false)
            qc.invalidateQueries({ queryKey: ['note', noteId] })
            qc.invalidateQueries({ queryKey: ['notes'] })
          }}
        />
      )}
    </div>
  )
}
