import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { solutionsApi, type UpsertMemoBody } from '../api/solutions'
import { problemsApi } from '../api/problems'
import type { Problem } from '../types'
import { LANGUAGES, languageLabel } from '../lib/languages'
import { detectLanguage } from '../lib/detectLanguage'
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
  const [autoDetected, setAutoDetected] = useState<string | null>(null) // 자동 감지 안내용
  const [memo, setMemo] = useState<UpsertMemoBody>({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // 사용자가 언어를 직접 고르면 자동 감지 중단. 코드 길이 변화로 붙여넣기 판별.
  const langTouched = useRef(false)
  const prevLen = useRef(0)
  // 확장에서 ?focus=memo 로 들어오면 메모 섹션을 자동으로 열고 스크롤
  const memoRef = useRef<HTMLDetailsElement>(null)

  // 코드 변경 처리: 큰 폭으로 늘었으면(=붙여넣기) 언어 자동 감지
  function handleCodeChange(v: string) {
    const delta = v.length - prevLen.current
    prevLen.current = v.length
    setCode(v)
    if (!langTouched.current && delta >= 15) {
      const detected = detectLanguage(v)
      if (detected) {
        setLanguage(detected)
        setAutoDetected(detected)
      }
    }
  }

  // 편집 데이터 반영
  useEffect(() => {
    if (editing) {
      setProblem(editing.problem)
      setCode(editing.code)
      setLanguage(editing.language)
      langTouched.current = true // 저장된 언어를 자동 감지가 덮어쓰지 않도록
      prevLen.current = editing.code.length
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

  // 확장에서 넘어온 경우(?focus=memo): 메모 섹션 펼치고 스크롤
  useEffect(() => {
    if (params.get('focus') !== 'memo') return
    const el = memoRef.current
    if (el) {
      el.open = true
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }, [params, editing])

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
          <div className="flex items-center gap-2 rounded-md border border-slate-800 bg-slate-900/50 px-3 py-2.5 text-sm text-slate-200">
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
        <div className="flex items-center gap-2">
          <select
            value={language}
            onChange={(e) => { setLanguage(e.target.value); langTouched.current = true; setAutoDetected(null) }}
            className="select-field"
          >
            {LANGUAGES.map((l) => <option key={l} value={l}>{languageLabel(l)}</option>)}
          </select>
          {autoDetected && (
            <span className="font-mono text-xs text-teal-400">↳ 자동 감지됨 · 필요하면 바꿀 수 있어요</span>
          )}
        </div>
      </div>

      <div className="mb-4">
        <label className="mb-1.5 block text-sm font-medium text-slate-400">코드</label>
        <CodeEditor value={code} onChange={handleCodeChange} language={language} placeholder="풀이 코드를 입력하거나 붙여넣으세요" />
      </div>

      {/* 메모 (선택) */}
      <details ref={memoRef} className="glass-card mb-4 p-4">
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
