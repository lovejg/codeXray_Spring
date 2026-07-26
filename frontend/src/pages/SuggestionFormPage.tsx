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
      <h1 className="page-title mb-5">건의하기</h1>

      <div className="mb-3">
        <label className="mb-1.5 block text-sm font-medium text-slate-400">유형</label>
        <select value={type} onChange={(e) => setType(e.target.value as PostType)} className="rounded-xl border border-white/10 bg-slate-950/60 px-3 py-2 text-sm text-slate-200 outline-none transition focus:border-teal-400/70 focus:ring-2 focus:ring-teal-500/20">
          {SUGGESTION_POST_TYPES.map((t) => <option key={t} value={t}>{POST_TYPE_LABEL[t]}</option>)}
        </select>
      </div>

      <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목" className="input-field mb-3" />
      <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={12} placeholder={SUGGESTION_CONTENT_PLACEHOLDER} className="input-field mb-3 resize-y font-mono text-[13px] leading-relaxed" />

      {error && <p className="mb-3 text-sm text-rose-400">{error}</p>}

      <div className="flex gap-2">
        <button onClick={save} disabled={saving} className="btn-primary">
          {saving ? '저장 중…' : '등록'}
        </button>
        <button onClick={() => navigate(-1)} className="btn-ghost">취소</button>
      </div>
    </div>
  )
}

const SUGGESTION_CONTENT_PLACEHOLDER = `개선 아이디어나 불편했던 점을 편하게 적어주세요. 예)

- OOO 기능이 있으면 좋겠어요.
- △△ 화면에서 □□가 불편했어요.
- 이런 문제 출처도 추가되면 좋겠어요.`
