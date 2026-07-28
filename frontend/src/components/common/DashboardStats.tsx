import { useQuery } from '@tanstack/react-query'
import { statsApi, type Dashboard } from '../../api/stats'
import { TIER_ORDER, tierLabel } from '../../lib/tier'
import { languageLabel } from '../../lib/languages'
import Spinner from './Spinner'
import EmptyState from './EmptyState'

// 프로필 페이지에 얹는 개인 활동 통계 (요약 타일 + 잔디밭 + 분포).
export default function DashboardStats() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['stats', 'me'],
    queryFn: statsApi.dashboard,
  })

  if (isLoading) return <Spinner label="통계 집계 중…" />
  if (isError || !data) return <EmptyState tone="error" message="통계를 불러오지 못했습니다." />

  const s = data.summary
  if (s.totalSolved === 0) {
    return <EmptyState message="아직 집계할 활동이 없습니다" hint="문제를 풀고 풀이를 등록하면 통계가 쌓여요." />
  }

  return (
    <div className="space-y-6">
      {/* 요약 타일 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <StatTile label="해결" value={s.totalSolved} />
        <StatTile label="현재 스트릭" value={s.currentStreak} suffix="일" accent />
        <StatTile label="최장 스트릭" value={s.longestStreak} suffix="일" />
        <StatTile label="다시 풀 문제" value={s.starred} />
        <StatTile label="노트" value={s.notes} />
      </div>

      {/* 잔디밭 */}
      <section className="glass-card p-5">
        <p className="section-title mb-4">// 최근 활동</p>
        <Heatmap cells={data.heatmap} />
      </section>

      {/* 분포들 */}
      <div className="grid gap-6 lg:grid-cols-2">
        <section className="glass-card p-5">
          <p className="section-title mb-4">// 티어 분포</p>
          <TierBars tiers={data.tiers} />
        </section>

        <section className="glass-card p-5">
          <p className="section-title mb-4">// 언어 분포</p>
          <Bars items={data.languages.map((l) => ({ label: languageLabel(l.label), count: l.count }))} />
        </section>

        <section className="glass-card p-5">
          <p className="section-title mb-4">// 많이 푼 태그</p>
          {data.topTags.length ? (
            <Bars items={data.topTags} />
          ) : (
            <p className="font-mono text-sm text-slate-600">// 태그 데이터 없음</p>
          )}
        </section>

        <section className="glass-card p-5">
          <p className="section-title mb-4">// 약점 태그 <span className="text-slate-600">(체감 난이도 높음)</span></p>
          {data.weakTags.length ? (
            <div className="flex flex-wrap gap-2">
              {data.weakTags.map((w) => (
                <span key={w.tag} className="rounded-md border border-rose-500/30 bg-rose-500/5 px-2.5 py-1 font-mono text-sm text-rose-300">
                  #{w.tag} <span className="text-rose-400/60">avg {w.avgLevel}</span>
                </span>
              ))}
            </div>
          ) : (
            <p className="font-mono text-sm text-slate-600">// 체감 난이도 피드백을 남기면 여기에 약점이 뜹니다</p>
          )}
        </section>
      </div>
    </div>
  )
}

// ── 요약 타일 ──
function StatTile({ label, value, suffix, accent }: { label: string; value: number; suffix?: string; accent?: boolean }) {
  return (
    <div className="glass-card p-4">
      <div className="font-mono text-xs uppercase tracking-widest text-slate-500">{label}</div>
      <div className={`mt-1 font-mono text-2xl font-bold ${accent ? 'text-teal-300' : 'text-white'}`}>
        {value.toLocaleString()}
        {suffix && <span className="ml-0.5 text-sm font-normal text-slate-500">{suffix}</span>}
      </div>
    </div>
  )
}

// ── 가로 막대 ──
function Bars({ items }: { items: { label: string; count: number }[] }) {
  const max = Math.max(1, ...items.map((i) => i.count))
  return (
    <div className="space-y-2.5">
      {items.map((it) => (
        <div key={it.label} className="flex items-center gap-3 font-mono text-sm">
          <span className="w-24 shrink-0 truncate text-slate-300">{it.label}</span>
          <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-800/60">
            <div className="h-full rounded-full bg-teal-400/70" style={{ width: `${(it.count / max) * 100}%` }} />
          </div>
          <span className="w-8 shrink-0 text-right text-slate-500">{it.count}</span>
        </div>
      ))}
    </div>
  )
}

// ── 티어 분포(티어 순서대로 정렬) ──
function TierBars({ tiers }: { tiers: Dashboard['tiers'] }) {
  const order = TIER_ORDER as readonly string[]
  const items = [...tiers]
    .sort((a, b) => order.indexOf(a.tier) - order.indexOf(b.tier))
    .map((t) => ({ label: t.tier === 'UNRATED' ? '미분류' : tierLabel(t.tier as never), count: t.count }))
  return <Bars items={items} />
}

// ── 잔디밭(GitHub식 히트맵) ──
function Heatmap({ cells }: { cells: Dashboard['heatmap'] }) {
  const map = new Map(cells.map((c) => [c.date, c.count]))
  const weeks = buildWeeks(map)
  return (
    <div className="overflow-x-auto">
      <div className="flex gap-1">
        {weeks.map((week, wi) => (
          <div key={wi} className="flex flex-col gap-1">
            {week.map((cell, di) => (
              <div
                key={di}
                title={cell ? `${cell.date} · ${cell.count}회` : ''}
                className="h-3 w-3 rounded-sm"
                style={{ backgroundColor: cell ? cellColor(cell.count) : 'transparent' }}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-end gap-1 font-mono text-[11px] text-slate-500">
        <span className="mr-1">less</span>
        {[0, 1, 3, 5].map((n) => (
          <span key={n} className="h-3 w-3 rounded-sm" style={{ backgroundColor: cellColor(n) }} />
        ))}
        <span className="ml-1">more</span>
      </div>
    </div>
  )
}

function cellColor(count: number) {
  if (count <= 0) return 'rgba(45,212,191,0.07)'
  if (count <= 2) return 'rgba(45,212,191,0.3)'
  if (count <= 4) return 'rgba(45,212,191,0.55)'
  return 'rgba(45,212,191,0.9)'
}

// 최근 ~180일을 주 단위 열(일~토)로 배치. 오늘 이후 칸은 null.
function buildWeeks(map: Map<string, number>) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(today.getDate() - 179)
  const gridStart = new Date(start)
  gridStart.setDate(start.getDate() - start.getDay())

  const weeks: ({ date: string; count: number } | null)[][] = []
  const cur = new Date(gridStart)
  while (cur <= today) {
    const week: ({ date: string; count: number } | null)[] = []
    for (let i = 0; i < 7; i++) {
      if (cur > today) {
        week.push(null)
      } else {
        const iso = isoLocal(cur)
        week.push({ date: iso, count: map.get(iso) ?? 0 })
      }
      cur.setDate(cur.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
}

function isoLocal(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}
