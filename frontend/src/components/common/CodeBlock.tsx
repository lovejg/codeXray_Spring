import { useState } from 'react'
import { Copy, Check } from 'lucide-react'
import hljs from 'highlight.js/lib/common'
import { languageLabel } from '../../lib/languages'

// 읽기 전용 코드 표시 블록. language 로 문법 강조 + 우상단 복사 버튼.
export default function CodeBlock({ code, language }: { code: string; language?: string | null }) {
  const [copied, setCopied] = useState(false)

  const highlighted =
    language && hljs.getLanguage(language)
      ? hljs.highlight(code, { language }).value
      : hljs.highlightAuto(code).value

  async function copy() {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // 클립보드 접근 불가(비-https 등) 시 조용히 무시
    }
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-800">
      <div className="flex items-center justify-between border-b border-slate-800 bg-slate-900/60 px-3 py-1.5">
        <span className="text-[11px] text-slate-500">{language ? languageLabel(language) : 'code'}</span>
        <button
          onClick={copy}
          className="inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[11px] text-slate-400 transition hover:bg-white/10 hover:text-slate-200"
          aria-label="코드 복사"
        >
          {copied ? <Check size={13} className="text-emerald-400" /> : <Copy size={13} />}
          {copied ? '복사됨' : '복사'}
        </button>
      </div>
      <pre className="m-0 font-mono text-[13px] leading-relaxed">
        <code
          className="hljs"
          style={{ maxHeight: '75vh', overflowY: 'auto' }}
          dangerouslySetInnerHTML={{ __html: highlighted }}
        />
      </pre>
    </div>
  )
}
