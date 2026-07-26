export default function TagBadge({ name }: { name: string }) {
  return (
    <span className="inline-flex items-center rounded border border-slate-700/70 bg-slate-800/40 px-1.5 py-0.5 font-mono text-xs text-teal-300/90">
      #{name}
    </span>
  )
}
