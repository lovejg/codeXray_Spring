import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { notesApi } from '../api/notes'
import { NOTE_TYPE_LABEL, type Note, type NoteType } from '../types'
import { languageLabel } from '../lib/languages'
import { toPlainPreview } from '../lib/markdownPreview'
import PageHeader from '../components/common/PageHeader'
import Spinner from '../components/common/Spinner'
import NoteTypeBadge from '../components/common/NoteTypeBadge'
import TagBadge from '../components/common/TagBadge'
import NoteFormModal from './NoteFormModal'

const NOTE_TYPES = Object.keys(NOTE_TYPE_LABEL) as NoteType[]

export default function NotesPage() {
  const qc = useQueryClient()
  const [typeFilter, setTypeFilter] = useState<NoteType | ''>('')
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState<Note | null>(null)
  const [creating, setCreating] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['notes', { typeFilter, search }],
    queryFn: () => notesApi.list(typeFilter || undefined, search || undefined),
  })

  const remove = useMutation({
    mutationFn: (id: number) => notesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notes'] }),
  })

  return (
    <div>
      <PageHeader title="노트" subtitle="배운 개념과 실수를 기록해 두고 다시 꺼내보세요.">
        <button onClick={() => setCreating(true)} className="btn-primary">
          <Plus size={16} /> 노트 작성
        </button>
      </PageHeader>

      <div className="mb-5 flex flex-wrap items-center gap-2">
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value as NoteType | '')} className={selectCls}>
          <option value="">전체 유형</option>
          {NOTE_TYPES.map((t) => <option key={t} value={t}>{NOTE_TYPE_LABEL[t]}</option>)}
        </select>

        <div className="flex">
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && setSearch(keyword.trim())}
            placeholder="제목/본문 검색"
            className="w-52 rounded-l-md border border-slate-800 bg-slate-950 px-3 py-2 font-mono text-sm text-white outline-none transition focus:border-teal-400/60"
          />
          <button onClick={() => setSearch(keyword.trim())} className="rounded-r-md border border-l-0 border-slate-800 bg-slate-900/50 px-3 font-mono text-sm text-slate-300 transition hover:text-teal-300">검색</button>
        </div>
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <p className="py-16 text-center text-sm text-slate-500">노트가 없습니다.</p>}

      <div className="grid gap-3 sm:grid-cols-2">
        {data?.map((n) => (
          <div key={n.id} className="glass-card p-5 transition hover:bg-white/[0.07]">
            <div className="flex items-center gap-2">
              <NoteTypeBadge type={n.type} />
              {/* 제목 클릭 → 상세 페이지 */}
              <Link to={`/notes/${n.id}`} className="min-w-0 flex-1 truncate font-medium text-slate-100 transition hover:text-teal-300">{n.title}</Link>
              {n.language && (
                <span className="rounded-md bg-white/5 px-1.5 py-0.5 text-[11px] text-slate-400">{languageLabel(n.language)}</span>
              )}
              <button onClick={() => setEditing(n)} className="text-slate-500 transition hover:text-teal-300" aria-label="수정"><Pencil size={15} /></button>
              <button onClick={() => { if (confirm('이 노트를 삭제할까요?')) remove.mutate(n.id) }} className="text-slate-500 transition hover:text-rose-400" aria-label="삭제"><Trash2 size={15} /></button>
            </div>

            {/* 본문 미리보기: 마크다운 기호를 벗긴 평문 3줄 말줄임(전체는 상세에서) */}
            <Link to={`/notes/${n.id}`} className="mt-2 block">
              <p className="line-clamp-3 whitespace-pre-wrap text-sm text-slate-400">{toPlainPreview(n.body)}</p>
            </Link>

            {n.tags.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {n.tags.map((t) => <TagBadge key={t} name={t} />)}
              </div>
            )}
          </div>
        ))}
      </div>

      {(creating || editing) && (
        <NoteFormModal
          note={editing}
          onClose={() => { setCreating(false); setEditing(null) }}
          onSaved={() => { setCreating(false); setEditing(null); qc.invalidateQueries({ queryKey: ['notes'] }) }}
        />
      )}
    </div>
  )
}

const selectCls = 'rounded-md border border-slate-800 bg-slate-950 px-3 py-2 font-mono text-sm text-slate-200 outline-none transition focus:border-teal-400/60'
