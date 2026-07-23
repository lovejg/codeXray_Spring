import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { communityApi } from '../api/community'
import { COMMUNITY_POST_TYPES, POST_TYPE_LABEL, type PostType, type Problem } from '../types'
import { apiErrorMessage } from '../lib/apiError'
import Spinner from '../components/common/Spinner'
import ProblemPicker from '../components/common/ProblemPicker'

export default function CommunityFormPage() {
  const { id } = useParams()
  const editId = id ? Number(id) : null
  const isEdit = editId != null
  const navigate = useNavigate()

  const { data: editing, isLoading } = useQuery({
    queryKey: ['community', 'post', editId],
    queryFn: () => communityApi.getPost(editId!),
    enabled: isEdit,
  })

  const [type, setType] = useState<PostType>('QUESTION')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [isPrivate, setIsPrivate] = useState(false)
  const [problem, setProblem] = useState<Problem | null>(null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (editing) {
      setType(editing.type)
      setTitle(editing.title)
      setContent(editing.content)
      setIsPrivate(editing.isPrivate)
    }
  }, [editing])

  async function save() {
    if (!title.trim() || !content.trim()) { setError('제목과 내용을 입력해 주세요.'); return }
    setSaving(true)
    setError('')
    try {
      if (isEdit) {
        await communityApi.updatePost(editId!, { title: title.trim(), content, isPrivate })
        navigate(`/community/${editId}`)
      } else {
        const created = await communityApi.createPost({
          type,
          title: title.trim(),
          content,
          isPrivate,
          problemId: problem?.id,
        })
        navigate(`/community/${created.id}`)
      }
    } catch (err) {
      setError(apiErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSaving(false)
    }
  }

  if (isEdit && isLoading) return <Spinner label="불러오는 중…" />

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-4 text-xl font-bold text-white">{isEdit ? '글 수정' : '글쓰기'}</h1>

      {!isEdit && (
        <div className="mb-3">
          <label className="mb-1.5 block text-sm text-slate-400">유형</label>
          <select value={type} onChange={(e) => setType(e.target.value as PostType)} className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500">
            {COMMUNITY_POST_TYPES.map((t) => <option key={t} value={t}>{POST_TYPE_LABEL[t]}</option>)}
          </select>
        </div>
      )}

      {!isEdit && (
        <div className="mb-3">
          <label className="mb-1.5 block text-sm text-slate-400">연결할 문제 (선택)</label>
          {problem ? (
            <div className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200">
              {problem.title}
              <button onClick={() => setProblem(null)} className="ml-auto text-xs text-slate-500 hover:text-slate-300">해제</button>
            </div>
          ) : (
            <ProblemPicker onPick={setProblem} />
          )}
        </div>
      )}

      <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목" className="mb-3 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500" />
      <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={14} placeholder="내용 (마크다운 지원)" className="mb-3 w-full resize-y rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-[13px] text-slate-100 outline-none focus:border-sky-500" />

      <label className="mb-4 flex items-center gap-2 text-sm text-slate-400">
        <input type="checkbox" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)} className="accent-sky-500" />
        비공개 (나와 관리자만 볼 수 있음)
      </label>

      {error && <p className="mb-3 text-sm text-rose-400">{error}</p>}

      <div className="flex gap-2">
        <button onClick={save} disabled={saving} className="rounded-lg bg-sky-500 px-5 py-2 text-sm font-medium text-white hover:bg-sky-400 disabled:opacity-60">
          {saving ? '저장 중…' : '저장'}
        </button>
        <button onClick={() => navigate(-1)} className="rounded-lg border border-slate-700 px-5 py-2 text-sm text-slate-300 hover:bg-slate-800">취소</button>
      </div>
    </div>
  )
}
