import { isValidElement, useState, type ReactNode } from 'react'
import { Copy, Check } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'

// React 자식 트리(hljs span 들)에서 원본 코드 텍스트만 추출 → 복사용
function extractText(node: ReactNode): string {
  if (typeof node === 'string') return node
  if (typeof node === 'number') return String(node)
  if (Array.isArray(node)) return node.map(extractText).join('')
  if (isValidElement(node)) return extractText((node.props as { children?: ReactNode }).children)
  return ''
}

// 코드펜스 래퍼: 우상단 복사 버튼 + hljs 코드. (모듈 레벨 컴포넌트라 hook 상태 안정적)
function CodeFence({ children }: { children?: ReactNode }) {
  const [copied, setCopied] = useState(false)
  async function copy() {
    try {
      await navigator.clipboard.writeText(extractText(children))
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      /* 클립보드 접근 불가 시 무시 */
    }
  }
  return (
    <div className="relative my-3 overflow-hidden rounded-lg border border-slate-800">
      <button
        onClick={copy}
        className="absolute right-2 top-2 z-10 inline-flex items-center gap-1 rounded-md border border-white/10 bg-slate-900/80 px-1.5 py-0.5 text-[11px] text-slate-300 backdrop-blur transition hover:bg-slate-800"
        aria-label="코드 복사"
      >
        {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
        {copied ? '복사됨' : '복사'}
      </button>
      <pre className="m-0">{children}</pre>
    </div>
  )
}

// 마크다운 렌더 (노트 본문, 커뮤니티 글, AI 결과 공용). 다크 테마 prose 스타일.
// rehypeHighlight 가 ```js 같은 코드펜스를 파싱해 hljs 토큰 span 으로 바꿔줌(velog 스타일 강조).
export default function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown text-sm leading-relaxed text-slate-200">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[[rehypeHighlight, { detect: true, ignoreMissing: true }]]}
        components={{
          code({ className, children, ...props }) {
            // 인라인 코드는 언어 className 이 없음 → 작은 뱃지형으로
            const inline = !className
            if (inline) {
              return (
                <code className="rounded bg-slate-800 px-1 py-0.5 font-mono text-[12px] text-sky-300" {...props}>
                  {children}
                </code>
              )
            }
            // 블록 코드: rehypeHighlight 가 넣어준 className(hljs language-xxx)과
            // 자식 span 을 그대로 통과 → 테마의 pre code.hljs 규칙이 색/패딩/스크롤 담당.
            return (
              <code className={`${className ?? ''} font-mono text-[13px] leading-relaxed`} {...props}>
                {children}
              </code>
            )
          },
          // 코드펜스 → 복사 버튼 달린 래퍼(패딩/배경/스크롤은 안쪽 code.hljs 담당)
          pre: CodeFence,
          a({ children, ...props }) {
            return <a className="text-sky-400 hover:underline" target="_blank" rel="noreferrer" {...props}>{children}</a>
          },
          ul({ children }) {
            return <ul className="my-2 list-disc space-y-1 pl-5">{children}</ul>
          },
          ol({ children }) {
            return <ol className="my-2 list-decimal space-y-1 pl-5">{children}</ol>
          },
          h1({ children }) {
            return <h1 className="mt-4 mb-2 text-lg font-bold text-white">{children}</h1>
          },
          h2({ children }) {
            return <h2 className="mt-4 mb-2 text-base font-bold text-white">{children}</h2>
          },
          h3({ children }) {
            return <h3 className="mt-3 mb-1.5 text-sm font-bold text-slate-100">{children}</h3>
          },
          p({ children }) {
            return <p className="my-2">{children}</p>
          },
          blockquote({ children }) {
            return <blockquote className="my-2 border-l-2 border-slate-600 pl-3 text-slate-400">{children}</blockquote>
          },
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  )
}
