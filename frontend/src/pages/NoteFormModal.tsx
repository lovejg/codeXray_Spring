import { useState } from 'react'
import { notesApi, type NoteBody } from '../api/notes'
import { NOTE_TYPE_LABEL, type Note, type NoteType } from '../types'
import { LANGUAGES, languageLabel } from '../lib/languages'
import { apiErrorMessage } from '../lib/apiError'
import Modal from '../components/common/Modal'

const NOTE_TYPES = Object.keys(NOTE_TYPE_LABEL) as NoteType[]

// 노트 작성/수정 모달. 목록(생성)과 상세(수정) 양쪽에서 재사용.
export default function NoteFormModal({ note, onClose, onSaved }: { note: Note | null; onClose: () => void; onSaved: () => void }) {
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
    <Modal open onClose={onClose} title={isEdit ? '노트 수정' : '노트 작성'} size="xl">
      <div className="space-y-3">
        <div className="flex gap-2">
          {/* 다른 화면과 같은 공용 클래스(teal 테마). 예전에는 이 모달만 하늘색 버튼이었다 */}
          <select value={type} onChange={(e) => setType(e.target.value as NoteType)} className="select-field">
            {NOTE_TYPES.map((t) => <option key={t} value={t}>{NOTE_TYPE_LABEL[t]}</option>)}
          </select>
          <select value={language} onChange={(e) => setLanguage(e.target.value)} className="select-field">
            <option value="">언어 없음</option>
            {LANGUAGES.map((l) => <option key={l} value={l}>{languageLabel(l)}</option>)}
          </select>
        </div>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목" className="input-field" />
        <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={16} placeholder={NOTE_BODY_PLACEHOLDER} className="input-field resize-y text-[13px] leading-relaxed" />
        <input value={tagsInput} onChange={(e) => setTagsInput(e.target.value)} placeholder="태그 (쉼표로 구분)" className="input-field" />

        {error && <p className="text-sm text-rose-400">{error}</p>}

        <div className="flex justify-end gap-2 pt-1">
          <button onClick={onClose} className="btn-ghost">취소</button>
          <button onClick={save} disabled={saving} className="btn-primary">
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

// 마크다운 사용법을 겸한 작성 예시. ```언어 코드펜스를 쓰면 문법 강조가 적용된다.
const NOTE_BODY_PLACEHOLDER = `마크다운으로 자유롭게 작성하세요. 예)

## 접근 방법
- 이분 탐색으로 범위를 좁혀 O(log n)

## 핵심 코드
\`\`\`python
def solve(nums, target):
    lo, hi = 0, len(nums) - 1
    ...
\`\`\`

## 배운 점 / 막힌 부분
- lower_bound 처리에서 실수했음`
