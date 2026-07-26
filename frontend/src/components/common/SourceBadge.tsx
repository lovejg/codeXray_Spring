import { SOURCE_LABEL, type ProblemSource } from '../../types'

export default function SourceBadge({ source }: { source: ProblemSource }) {
  return (
    <span className="inline-flex items-center rounded border border-slate-800 bg-slate-900/60 px-1.5 py-0.5 font-mono text-xs text-slate-400">
      {SOURCE_LABEL[source]}
    </span>
  )
}
