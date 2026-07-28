// 터미널풍 로딩 표시: `$ 불러오는 중… ▊` (깜빡이는 커서)
export default function Spinner({ label = '불러오는 중…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center py-16 font-mono text-sm text-slate-400">
      <span className="text-teal-400">$</span>
      <span className="ml-2">{label}</span>
      <span className="cursor-blink ml-0.5 text-teal-400">▊</span>
    </div>
  )
}
