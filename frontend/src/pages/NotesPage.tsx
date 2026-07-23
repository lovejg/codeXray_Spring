import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { notesApi, type NoteBody } from '../api/notes'
import { NOTE_TYPE_LABEL, type Note, type NoteType } from '../types'
import { apiErrorMessage } from '../lib/apiError'
import Spinner from '../components/common/Spinner'
import Modal from '../components/common/Modal'
import NoteTypeBadge from '../components/common/NoteTypeBadge'
import TagBadge from '../components/common/TagBadge'
import Markdown from '../components/common/Markdown'

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
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <h1 className="text-xl font-bold text-white">노트</h1>

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
            className="w-48 rounded-l-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white outline-none focus:border-sky-500"
          />
          <button onClick={() => setSearch(keyword.trim())} className="rounded-r-lg border border-l-0 border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 hover:bg-slate-700">검색</button>
        </div>

        <button onClick={() => setCreating(true)} className="ml-auto inline-flex items-center gap-1.5 rounded-lg bg-sky-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-sky-400">
          <Plus size={15} /> 노트 작성
        </button>
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <p className="py-16 text-center text-sm text-slate-500">노트가 없습니다.</p>}

      <div className="grid gap-3 sm:grid-cols-2">
        {data?.map((n) => (
          <div key={n.id} className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
            <div className="flex items-center gap-2">
              <NoteTypeBadge type={n.type} />
              <h3 className="font-medium text-slate-100">{n.title}</h3>
              <div className="ml-auto flex gap-1.5">
                <button onClick={() => setEditing(n)} className="text-slate-500 hover:text-sky-400"><Pencil size={15} /></button>
                <button onClick={() => { if (confirm('이 노트를 삭제할까요?')) remove.mutate(n.id) }} className="text-slate-500 hover:text-rose-400"><Trash2 size={15} /></button>
              </div>
            </div>
            <div className="mt-2 max-h-48 overflow-hidden">
              <Markdown>{n.body}</Markdown>
            </div>
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

function NoteFormModal({ note, onClose, onSaved }: { note: Note | null; onClose: () => void; onSaved: () => void }) {
  const isEdit = note != null
  const [type, setType] = useState<NoteType>(note?.type ?? 'CODE')
  const [title, setTitle] = useState(note?.title ?? '')
  const [body, setBody] = useState(note?.body ?? '')
  const [language, setLanguage] = useState(note?.language ?? '')
  const [tagsInput, setTagsInput] = useState((note?.tags ?? []).join(', '))
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function save() {
    if (!title.trim() || !body.trim()) { setError('제목과 본문을 입력해 주세요.'); return }
    const tags = tagsInput.split(',').map((t) => t.trim()).filter(Boolean).slice(0, 20)
    const payload: NoteBody = { type, title: title.trim(), body, language: language || undefined, tags }
    setSaving(true)
    setError('')
    try {
      if (isEdit) await notesApi.update(note.id, payload)
      else await notesApi.create(payload)
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal open onClose={onClose} title={isEdit ? '노트 수정' : '노트 작성'}>
      <div className="space-y-3">
        <div className="flex gap-2">
          <select value={type} onChange={(e) => setType(e.target.value as NoteType)} className={selectCls}>
            {NOTE_TYPES.map((t) => <option key={t} value={t}>{NOTE_TYPE_LABEL[t]}</option>)}
          </select>
          <input value={language} onChange={(e) => setLanguage(e.target.value)} placeholder="언어(선택)" className="w-32 rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white outline-none focus:border-sky-500" />
        </div>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목" className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500" />
        <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={10} placeholder="본문 (마크다운 지원)" className="w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-[13px] text-slate-100 outline-none focus:border-sky-500" />
        <input value={tagsInput} onChange={(e) => setTagsInput(e.target.value)} placeholder="태그 (쉼표로 구분)" className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500" />

        {error && <p className="text-sm text-rose-400">{error}</p>}

        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className="rounded-lg border border-slate-700 px-4 py-1.5 text-sm text-slate-300 hover:bg-slate-800">취소</button>
          <button onClick={save} disabled={saving} className="rounded-lg bg-sky-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sky-400 disabled:opacity-60">
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

const selectCls = 'rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500'
