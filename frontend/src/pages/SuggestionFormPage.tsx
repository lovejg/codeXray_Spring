import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { communityApi } from '../api/community'
import { SUGGESTION_POST_TYPES, POST_TYPE_LABEL, type PostType } from '../types'
import { apiErrorMessage } from '../lib/apiError'

export default function SuggestionFormPage() {
  const navigate = useNavigate()
  const [type, setType] = useState<PostType>('FEEDBACK')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function save() {
    if (!title.trim() || !content.trim()) { setError('제목과 내용을 입력해 주세요.'); return }
    setSaving(true)
    setError('')
    try {
      const created = await communityApi.createPost({ type, title: title.trim(), content })
      navigate(`/suggestions/${created.id}`)
    } catch (err) {
      setError(apiErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold text-white">건의하기</h1>

      <div className="mb-3">
        <label className="mb-1.5 block text-sm text-slate-400">유형</label>
        <select value={type} onChange={(e) => setType(e.target.value as PostType)} className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500">
          {SUGGESTION_POST_TYPES.map((t) => <option key={t} value={t}>{POST_TYPE_LABEL[t]}</option>)}
        </select>
      </div>

      <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목" className="mb-3 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500" />
      <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={12} placeholder="내용 (마크다운 지원)" className="mb-3 w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-[13px] text-slate-100 outline-none focus:border-sky-500" />

      {error && <p className="mb-3 text-sm text-rose-400">{error}</p>}

      <div className="flex gap-2">
        <button onClick={save} disabled={saving} className="rounded-lg bg-sky-500 px-5 py-2 text-sm font-medium text-white hover:bg-sky-400 disabled:opacity-60">
          {saving ? '저장 중…' : '등록'}
        </button>
        <button onClick={() => navigate(-1)} className="rounded-lg border border-slate-700 px-5 py-2 text-sm text-slate-300 hover:bg-slate-800">취소</button>
      </div>
    </div>
  )
}
