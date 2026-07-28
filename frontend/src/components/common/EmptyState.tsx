// 빈 목록/에러용 공용 표시. 개발자 감성으로 주석(`//`, `!`) 프리픽스.
export default function EmptyState({
  message,
  hint,
  tone = 'muted',
}: {
  message: string
  hint?: string
  tone?: 'muted' | 'error'
}) {
  const isError = tone === 'error'
  return (
    <div className="py-16 text-center font-mono">
      <p className={`text-sm ${isError ? 'text-rose-400/90' : 'text-slate-500'}`}>
        <span className={isError ? 'text-rose-500' : 'text-slate-600'}>{isError ? '! ' : '// '}</span>
        {message}
      </p>
      {hint && <p className="mt-2 text-xs text-slate-600">{hint}</p>}
    </div>
  )
}
