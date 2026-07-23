import { STATUS_COLOR, STATUS_LABEL, type SuggestionStatus } from '../../types'

export default function StatusBadge({ status }: { status?: SuggestionStatus | null }) {
  if (!status) {
    return <span className="inline-flex items-center rounded bg-slate-800 px-1.5 py-0.5 text-[11px] text-slate-400">접수</span>
  }
  const c = STATUS_COLOR[status]
  return (
    <span className="inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium" style={{ color: c.text, backgroundColor: c.bg }}>
      {STATUS_LABEL[status]}
    </span>
  )
}
