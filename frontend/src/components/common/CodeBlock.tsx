// 읽기 전용 코드 표시 블록.
export default function CodeBlock({ code, language }: { code: string; language?: string | null }) {
  return (
    <div className="overflow-hidden rounded-lg border border-slate-800">
      {language && (
        <div className="border-b border-slate-800 bg-slate-900/60 px-3 py-1 text-[11px] text-slate-500">
          {language}
        </div>
      )}
      <pre className="overflow-x-auto bg-[#0d1117] px-3 py-2.5 font-mono text-[13px] leading-relaxed text-slate-100">
        <code>{code}</code>
      </pre>
    </div>
  )
}
