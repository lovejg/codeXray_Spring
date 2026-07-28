import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { problemsApi, type ProblemQuery } from '../api/problems'
import { SOURCE_LABEL, type ProblemSource } from '../types'
import { TIER_ORDER, tierLabel } from '../lib/tier'
import { useDebouncedValue } from '../lib/useDebouncedValue'
import PageHeader from '../components/common/PageHeader'
import Spinner from '../components/common/Spinner'
import TierBadge from '../components/common/TierBadge'
import LevelBadge from '../components/common/LevelBadge'
import SourceBadge from '../components/common/SourceBadge'
import TagBadge from '../components/common/TagBadge'

const PAGE_SIZE = 20
const SORTS = [
  { value: 'createdAt,desc', label: '기본' },
  { value: 'level,desc', label: '레벨 높은순' },
  { value: 'level,asc', label: '레벨 낮은순' },
  { value: 'acceptanceRate,asc', label: '정답률 낮은순' },
]

export default function ProblemsPage() {
  const [keyword, setKeyword] = useState('')
  const [source, setSource] = useState<ProblemSource | ''>('')
  const [tierMin, setTierMin] = useState<number | ''>('')
  const [tierMax, setTierMax] = useState<number | ''>('')
  const [sort, setSort] = useState('createdAt,desc')
  const [page, setPage] = useState(0)

  // 타이핑이 멈추면 300ms 뒤 검색어가 반영됨(실시간 검색)
  const search = useDebouncedValue(keyword.trim(), 300)

  // 검색어가 바뀌면 항상 1페이지부터 다시 보여줌
  useEffect(() => {
    setPage(0)
  }, [search])

  // '/' 키로 검색창 포커스 (GitHub/vim 스타일). 이미 입력 중이면 무시.
  const searchRef = useRef<HTMLInputElement>(null)
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key !== '/') return
      const el = document.activeElement as HTMLElement | null
      const tag = el?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el?.isContentEditable) return
      e.preventDefault()
      searchRef.current?.focus()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const query: ProblemQuery = {
    keyword: search || undefined,
    source: source || undefined,
    tierMin: tierMin === '' ? undefined : tierMin,
    tierMax: tierMax === '' ? undefined : tierMax,
    sort,
    page,
    size: PAGE_SIZE,
  }

  const { data, isLoading, isError } = useQuery({
    queryKey: ['problems', query],
    queryFn: () => problemsApi.list(query),
    placeholderData: keepPreviousData,
  })

  return (
    <div>
      <PageHeader title="문제" subtitle="티어·출처·태그로 원하는 문제를 찾아보세요.">
        {data && <span className="text-sm text-slate-400">총 {data.total.toLocaleString()}문제</span>}
      </PageHeader>

      {/* 필터 바 */}
      <div className="mb-5 flex flex-wrap items-center gap-2">
        <div className="relative">
          <svg className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z" />
          </svg>
          <input
            ref={searchRef}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="제목 검색"
            className={`${selectCls} w-72 pl-10 pr-9`}
          />
          {!keyword && <kbd className="kbd pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2">/</kbd>}
        </div>

        <select value={source} onChange={(e) => { setSource(e.target.value as ProblemSource | ''); setPage(0) }} className={selectCls}>
          <option value="">전체 출처</option>
          {(Object.keys(SOURCE_LABEL) as ProblemSource[]).map((s) => (
            <option key={s} value={s}>{SOURCE_LABEL[s]}</option>
          ))}
        </select>

        <select value={tierMin} onChange={(e) => { setTierMin(e.target.value === '' ? '' : Number(e.target.value)); setPage(0) }} className={selectCls}>
          <option value="">최소 티어</option>
          {TIER_ORDER.map((t, i) => <option key={t} value={i}>{tierLabel(t)}</option>)}
        </select>
        <select value={tierMax} onChange={(e) => { setTierMax(e.target.value === '' ? '' : Number(e.target.value)); setPage(0) }} className={selectCls}>
          <option value="">최대 티어</option>
          {TIER_ORDER.map((t, i) => <option key={t} value={i}>{tierLabel(t)}</option>)}
        </select>

        <select value={sort} onChange={(e) => { setSort(e.target.value); setPage(0) }} className={`${selectCls} ml-auto`}>
          {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}
      {isError && <p className="py-10 text-center text-sm text-rose-400">문제를 불러오지 못했습니다.</p>}

      {data && (
        <>
          <div className="glass-card overflow-hidden">
            <table className="w-full text-[15px]">
              <thead className="border-b border-slate-800 bg-slate-950/60 text-left font-mono text-xs uppercase tracking-widest text-slate-500">
                <tr>
                  <th className="px-5 py-3.5 font-semibold">제목</th>
                  <th className="w-24 px-3 py-3.5 font-semibold">레벨</th>
                  <th className="w-32 px-3 py-3.5 font-semibold">티어</th>
                  <th className="px-3 py-3.5 font-semibold">태그</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {data.items.map((p) => (
                  <tr key={p.id} className="group transition hover:bg-white/5">
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono text-teal-400 opacity-0 transition group-hover:opacity-100">&gt;</span>
                        <Link to={`/problems/${p.id}`} className="font-semibold text-slate-100 transition hover:text-teal-300">
                          {p.title}
                        </Link>
                      </div>
                      <div className="mt-1.5 pl-4"><SourceBadge source={p.source} /></div>
                    </td>
                    <td className="px-3 py-4"><LevelBadge level={p.level} /></td>
                    <td className="px-3 py-4"><TierBadge tier={p.tier} size="md" /></td>
                    <td className="px-3 py-4">
                      <div className="flex flex-wrap gap-1.5">
                        {p.tags.slice(0, 4).map((t) => <TagBadge key={t.id} name={t.name} />)}
                      </div>
                    </td>
                  </tr>
                ))}
                {data.items.length === 0 && (
                  <tr><td colSpan={4} className="px-4 py-14 text-center font-mono text-sm text-slate-500"><span className="text-slate-600">// </span>조건에 맞는 문제가 없습니다</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {/* 페이지네이션 */}
          <div className="mt-5 flex items-center justify-center gap-2 text-sm">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className={pageBtn}>이전</button>
            <span className="px-1 text-slate-400">
              {data.totalPages === 0 ? 0 : page + 1} / {data.totalPages}
            </span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)} className={pageBtn}>다음</button>
          </div>
        </>
      )}
    </div>
  )
}

const selectCls =
  'rounded-md border border-slate-800 bg-slate-950 px-3 py-2 font-mono text-sm text-slate-200 outline-none transition focus:border-teal-400/60'
const pageBtn =
  'rounded-md border border-slate-800 bg-slate-900/50 px-4 py-2 font-mono text-sm text-slate-300 transition hover:border-teal-400/40 hover:text-teal-300 disabled:cursor-not-allowed disabled:opacity-40'
