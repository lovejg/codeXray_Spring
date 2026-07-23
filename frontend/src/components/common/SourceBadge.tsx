import { SOURCE_LABEL, type ProblemSource } from '../../types'

export default function SourceBadge({ source }: { source: ProblemSource }) {
  return (
    <span className="inline-flex items-center rounded bg-slate-800 px-1.5 py-0.5 text-[11px] text-slate-400">
      {SOURCE_LABEL[source]}
    </span>
  )
}
