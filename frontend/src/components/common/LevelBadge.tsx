import { LEVEL_COLOR } from '../../types'

export default function LevelBadge({ level }: { level: number }) {
  const color = LEVEL_COLOR[level] ?? '#9ca3af'
  return (
    <span
      className="inline-flex h-5 min-w-5 items-center justify-center rounded px-1 text-[11px] font-bold"
      style={{ color, backgroundColor: `${color}1f` }}
      title={`레벨 ${level}`}
    >
      Lv.{level}
    </span>
  )
}
