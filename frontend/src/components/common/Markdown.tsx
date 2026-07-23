import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

// 마크다운 렌더 (노트 본문, 커뮤니티 글, AI 결과 공용). 다크 테마 prose 스타일.
export default function Markdown({ children }: { children: string }) {
  return (
    <div className="markdown text-sm leading-relaxed text-slate-200">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ className, children, ...props }) {
            const inline = !className
            if (inline) {
              return (
                <code className="rounded bg-slate-800 px-1 py-0.5 font-mono text-[12px] text-sky-300" {...props}>
                  {children}
                </code>
              )
            }
            return (
              <pre className="my-3 overflow-x-auto rounded-lg border border-slate-800 bg-[#0d1117] p-3">
                <code className="font-mono text-[13px] leading-relaxed text-slate-100">{children}</code>
              </pre>
            )
          },
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
