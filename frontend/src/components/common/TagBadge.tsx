export default function TagBadge({ name }: { name: string }) {
  return (
    <span className="inline-flex items-center rounded-full bg-slate-800/70 px-2 py-0.5 text-[11px] text-slate-300">
      #{name}
    </span>
  )
}
