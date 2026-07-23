import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { problemsApi } from '../../api/problems'
import type { Problem } from '../../types'
import LevelBadge from './LevelBadge'

// 문제 검색 후 하나 선택. 새 풀이 작성 시 문제 지정용.
export default function ProblemPicker({ onPick }: { onPick: (p: Problem) => void }) {
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState('')

  const { data } = useQuery({
    queryKey: ['problems', 'pick', search],
    queryFn: () => problemsApi.list({ keyword: search, size: 8, page: 0 }),
    enabled: search.length > 0,
  })

  return (
    <div>
      <div className="flex">
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && setSearch(keyword.trim())}
          placeholder="문제 제목 검색"
          className="w-full rounded-l-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-sky-500"
        />
        <button
          onClick={() => setSearch(keyword.trim())}
          className="rounded-r-lg border border-l-0 border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 hover:bg-slate-700"
        >
          검색
        </button>
      </div>

      {data && data.items.length > 0 && (
        <ul className="mt-2 divide-y divide-slate-800 overflow-hidden rounded-lg border border-slate-800">
          {data.items.map((p) => (
            <li key={p.id}>
              <button
                onClick={() => onPick(p)}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-slate-800/60"
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
