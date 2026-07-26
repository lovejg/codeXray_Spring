import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { solutionsApi, type UpsertMemoBody } from '../api/solutions'
import { problemsApi } from '../api/problems'
import type { Problem } from '../types'
import { LANGUAGES, languageLabel } from '../lib/languages'
import { apiErrorMessage } from '../lib/apiError'
import Spinner from '../components/common/Spinner'
import CodeEditor from '../components/common/CodeEditor'
import ProblemPicker from '../components/common/ProblemPicker'

export default function SolutionFormPage() {
  const { id } = useParams()
  const editId = id ? Number(id) : null
  const isEdit = editId != null
  const [params] = useSearchParams()
  const presetProblemId = params.get('problemId') ? Number(params.get('problemId')) : null
  const navigate = useNavigate()
  const qc = useQueryClient()

  // 편집 대상 로드
  const { data: editing, isLoading: loadingEdit } = useQuery({
    queryKey: ['solution', editId],
    queryFn: () => solutionsApi.get(editId!),
    enabled: isEdit,
  })
  // 새 작성 시 preset 문제 로드
  const { data: presetProblem } = useQuery({
    queryKey: ['problem', presetProblemId],
    queryFn: () => problemsApi.get(presetProblemId!),
    enabled: !isEdit && presetProblemId != null,
  })

  const [problem, setProblem] = useState<Problem | null>(null)
  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('python')
  const [memo, setMemo] = useState<UpsertMemoBody>({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // 편집 데이터 반영
  useEffect(() => {
    if (editing) {
      setProblem(editing.problem)
      setCode(editing.code)
      setLanguage(editing.language)
      setMemo({
        wrongReason: editing.memo?.wrongReason ?? '',
        logic: editing.memo?.logic ?? '',
        keyFunctions: editing.memo?.keyFunctions ?? '',
        freeNote: editing.memo?.freeNote ?? '',
      })
    }
  }, [editing])
  // preset 문제 반영
  useEffect(() => {
    if (presetProblem) setProblem(presetProblem)
  }, [presetProblem])

  async function onSubmit() {
    if (!problem) { setError('문제를 선택해 주세요.'); return }
    if (!code.trim()) { setError('코드를 입력해 주세요.'); return }
    setError('')
    setSaving(true)
    try {
      // 풀이 저장(upsert). 편집이면 update, 아니면 create.
      const saved = isEdit
        ? await solutionsApi.update(editId!, code, language)
        : await solutionsApi.create(problem.id, code, language)

      // 메모에 내용이 있으면 함께 저장
      const hasMemo = memo.wrongReason || memo.logic || memo.keyFunctions || memo.freeNote
      if (hasMemo) {
        await solutionsApi.upsertMemo(saved.id, memo)
      }
      // 목록/상세 캐시를 무효화 → 이동한 화면에서 새로고침 없이 최신 반영
      qc.invalidateQueries({ queryKey: ['solutions'] })
      qc.invalidateQueries({ queryKey: ['solution', saved.id] })
      navigate('/solutions')
    } catch (err) {
      setError(apiErrorMessage(err, '저장에 실패했습니다.'))
    } finally {
      setSaving(false)
    }
  }

  if (isEdit && loadingEdit) return <Spinner label="불러오는 중…" />

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="page-title mb-5">{isEdit ? '풀이 수정' : '풀이 등록'}</h1>

      {/* 문제 선택 (편집/preset 이면 고정) */}
      <div className="mb-4">
        <label className="mb-1.5 block text-sm font-medium text-slate-400">문제</label>
        {problem ? (
          <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-slate-200">
            {problem.title}
            {!isEdit && (
              <button onClick={() => setProblem(null)} className="ml-auto text-xs text-slate-500 hover:text-slate-300">변경</button>
            )}
          </div>
        ) : (
          <ProblemPicker onPick={setProblem} />
        )}
      </div>

      <div className="mb-4">
        <label className="mb-1.5 block text-sm font-medium text-slate-400">언어</label>
        <select value={language} onChange={(e) => setLanguage(e.target.value)} className="rounded-xl border border-white/10 bg-slate-950/60 px-3 py-2 text-sm text-slate-200 outline-none transition focus:border-teal-400/70 focus:ring-2 focus:ring-teal-500/20">
          {LANGUAGES.map((l) => <option key={l} value={l}>{languageLabel(l)}</option>)}
        </select>
      </div>

      <div className="mb-4">
        <label className="mb-1.5 block text-sm font-medium text-slate-400">코드</label>
        <CodeEditor value={code} onChange={setCode} language={language} placeholder="풀이 코드를 입력하거나 붙여넣으세요" />
      </div>

      {/* 메모 (선택) */}
      <details className="glass-card mb-4 p-4">
        <summary className="cursor-pointer text-sm text-slate-300">메모 (선택)</summary>
        <div className="mt-3 space-y-3">
          <MemoField label="틀린 이유" value={memo.wrongReason} onChange={(v) => setMemo((m) => ({ ...m, wrongReason: v }))} />
          <MemoField label="풀이 로직" value={memo.logic} onChange={(v) => setMemo((m) => ({ ...m, logic: v }))} />
          <MemoField label="핵심 함수/개념" value={memo.keyFunctions} onChange={(v) => setMemo((m) => ({ ...m, keyFunctions: v }))} />
          <MemoField label="자유 메모" value={memo.freeNote} onChange={(v) => setMemo((m) => ({ ...m, freeNote: v }))} />
        </div>
      </details>

      {error && <p className="mb-3 text-sm text-rose-400">{error}</p>}

      <div className="flex gap-2">
        <button onClick={onSubmit} disabled={saving} className="btn-primary">
          {saving ? '저장 중…' : '저장'}
        </button>
        <button onClick={() => navigate(-1)} className="btn-ghost">
          취소
        </button>
      </div>
    </div>
  )
}

function MemoField({ label, value, onChange }: { label: string; value?: string; onChange: (v: string) => void }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs text-slate-500">{label}</span>
      <textarea
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
        rows={2}
        className="input-field resize-y"
      />
    </label>
  )
}
