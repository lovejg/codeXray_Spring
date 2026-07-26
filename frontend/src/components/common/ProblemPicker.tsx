import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { problemsApi } from '../../api/problems'
import type { Problem } from '../../types'
import { useDebouncedValue } from '../../lib/useDebouncedValue'
import LevelBadge from './LevelBadge'

// 문제 검색 후 하나 선택. 새 풀이 작성 시 문제 지정용.
export default function ProblemPicker({ onPick }: { onPick: (p: Problem) => void }) {
  const [keyword, setKeyword] = useState('')
  // 타이핑이 멈추면 300ms 뒤 검색 실행(실시간). 별도 검색 버튼이 없어 레이아웃도 단순해짐.
  const search = useDebouncedValue(keyword.trim(), 300)

  const { data, isFetching } = useQuery({
    queryKey: ['problems', 'pick', search],
    queryFn: () => problemsApi.list({ keyword: search, size: 8, page: 0 }),
    enabled: search.length > 0,
  })

  return (
    <div>
      <div className="relative">
        <svg className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z" />
        </svg>
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="문제 제목 검색"
          className="w-full rounded-xl border border-white/10 bg-slate-950/60 py-2.5 pl-10 pr-3 text-sm text-white outline-none transition focus:border-teal-400/70 focus:ring-2 focus:ring-teal-500/20"
        />
      </div>

      {search.length > 0 && isFetching && (
        <p className="mt-2 text-xs text-slate-500">검색 중…</p>
      )}

      {data && data.items.length > 0 && (
        <ul className="mt-2 divide-y divide-white/5 overflow-hidden rounded-xl border border-white/10">
          {data.items.map((p) => (
            <li key={p.id}>
              <button
                onClick={() => onPick(p)}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm transition hover:bg-white/5"
              >
                <LevelBadge level={p.level} />
                <span className="text-slate-200">{p.title}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {data && data.items.length === 0 && (
        <p className="mt-2 text-xs text-slate-500">검색 결과가 없습니다.</p>
      )}
    </div>
  )
}
