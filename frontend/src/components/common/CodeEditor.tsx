// 가벼운 코드 입력기(모노스페이스 textarea). 탭 입력 지원.
// 필요 시 나중에 CodeMirror 로 교체할 수 있도록 단순 인터페이스로 감쌈.
import { type ChangeEvent, type KeyboardEvent } from 'react'

interface Props {
  value: string
  onChange: (v: string) => void
  placeholder?: string
  rows?: number
}

export default function CodeEditor({ value, onChange, placeholder, rows = 16 }: Props) {
  function handleKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Tab') {
      e.preventDefault()
      const el = e.currentTarget
      const start = el.selectionStart
      const end = el.selectionEnd
      const next = value.slice(0, start) + '  ' + value.slice(end)
      onChange(next)
      // 커서를 삽입한 공백 뒤로
      requestAnimationFrame(() => {
        el.selectionStart = el.selectionEnd = start + 2
      })
    }
  }

  return (
    <textarea
      value={value}
      onChange={(e: ChangeEvent<HTMLTextAreaElement>) => onChange(e.target.value)}
      onKeyDown={handleKey}
      placeholder={placeholder}
      rows={rows}
      spellCheck={false}
      className="w-full resize-y rounded-lg border border-slate-700 bg-[#0d1117] px-3 py-2.5 font-mono text-[13px] leading-relaxed text-slate-100 outline-none focus:border-sky-500"
    />
  )
}
