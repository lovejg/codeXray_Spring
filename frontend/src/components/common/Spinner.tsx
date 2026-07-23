export default function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-slate-400">
      <span className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-slate-600 border-t-sky-400" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  )
}
