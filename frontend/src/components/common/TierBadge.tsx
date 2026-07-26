import type { Tier } from '../../lib/tier'
import { tierColor, tierLabel } from '../../lib/tier'

export default function TierBadge({ tier, size = 'sm' }: { tier?: Tier | null; size?: 'sm' | 'md' }) {
  if (!tier) {
    return <span className="text-xs text-slate-600">-</span>
  }
  const color = tierColor(tier)
  const px = size === 'md' ? 'px-3 py-1 text-sm' : 'px-2.5 py-0.5 text-xs'
  return (
    <span
      className={`inline-flex items-center rounded font-mono font-semibold ${px}`}
      style={{ color, backgroundColor: `${color}22`, border: `1px solid ${color}44` }}
    >
      {tierLabel(tier)}
    </span>
  )
}
