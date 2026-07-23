import { useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { problemsApi, type ProblemQuery } from '../api/problems'
import { SOURCE_LABEL, type ProblemSource } from '../types'
import { TIER_ORDER, tierLabel } from '../lib/tier'
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
  const [search, setSearch] = useState('') // 실제 적용된 검색어
  const [source, setSource] = useState<ProblemSource | ''>('')
  const [tierMin, setTierMin] = useState<number | ''>('')
  const [tierMax, setTierMax] = useState<number | ''>('')
  const [sort, setSort] = useState('createdAt,desc')
  const [page, setPage] = useState(0)

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

  function applySearch() {
    setPage(0)
    setSearch(keyword.trim())
  }

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold text-white">문제</h1>

      {/* 필터 바 */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="flex">
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && applySearch()}
            placeholder="제목 검색"
            className="w-56 rounded-l-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white outline-none focus:border-sky-500"
          />
          <button onClick={applySearch} className="rounded-r-lg border border-l-0 border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 hover:bg-slate-700">
            검색
          </button>
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
          <div className="overflow-hidden rounded-xl border border-slate-800">
            <table className="w-full text-sm">
              <thead className="bg-slate-900/60 text-left text-xs text-slate-400">
                <tr>
                  <th className="px-4 py-2.5 font-medium">제목</th>
                  <th className="w-24 px-2 py-2.5 font-medium">레벨</th>
                  <th className="w-28 px-2 py-2.5 font-medium">티어</th>
                  <th className="px-2 py-2.5 font-medium">태그</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {data.items.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-900/40">
                    <td className="px-4 py-2.5">
                      <Link to={`/problems/${p.id}`} className="font-medium text-slate-100 hover:text-sky-400">
                        {p.title}
                      </Link>
                      <div className="mt-1"><SourceBadge source={p.source} /></div>
                    </td>
                    <td className="px-2 py-2.5"><LevelBadge level={p.level} /></td>
                    <td className="px-2 py-2.5"><TierBadge tier={p.tier} /></td>
                    <td className="px-2 py-2.5">
                      <div className="flex flex-wrap gap-1">
                        {p.tags.slice(0, 4).map((t) => <TagBadge key={t.id} name={t.name} />)}
                      </div>
                    </td>
                  </tr>
                ))}
                {data.items.length === 0 && (
                  <tr><td colSpan={4} className="px-4 py-10 text-center text-slate-500">조건에 맞는 문제가 없습니다.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {/* 페이지네이션 */}
          <div className="mt-4 flex items-center justify-center gap-2 text-sm">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className={pageBtn}>이전</button>
            <span className="text-slate-400">
              {data.totalPages === 0 ? 0 : page + 1} / {data.totalPages}
            </span>
            <button disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)} className={pageBtn}>다음</button>
            <span className="ml-3 text-xs text-slate-600">총 {data.total}문제</span>
          </div>
        </>
      )}
    </div>
  )
}

const selectCls =
  'rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500'
const pageBtn =
  'rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-slate-300 hover:bg-slate-800 disabled:opacity-40'
