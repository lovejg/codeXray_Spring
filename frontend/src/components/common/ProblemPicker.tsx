import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { problemsApi } from '../../api/problems'
import type { Problem } from '../../types'
import { useDebouncedValue } from '../../lib/useDebouncedValue'
import LevelBadge from './LevelBadge'

// 붙여넣은 값이 프로그래머스 문제 URL인지 판별.
function looksLikeProblemUrl(v: string) {
  return /programmers\.co\.kr/.test(v) && /lessons\/\d+/.test(v)
}

// 문제 검색 후 하나 선택. 새 풀이 작성 시 문제 지정용.
// 제목 검색뿐 아니라 "프로그래머스 문제 URL을 붙여넣으면" 자동으로 매칭·선택된다.
export default function ProblemPicker({ onPick }: { onPick: (p: Problem) => void }) {
  const [keyword, setKeyword] = useState('')
  // 타이핑이 멈추면 300ms 뒤 검색 실행(실시간).
  const search = useDebouncedValue(keyword.trim(), 300)
  const isUrl = looksLikeProblemUrl(search)

  // 제목 검색 (URL이 아닐 때만)
  const listQuery = useQuery({
    queryKey: ['problems', 'pick', search],
    queryFn: () => problemsApi.list({ keyword: search, size: 8, page: 0 }),
    enabled: search.length > 0 && !isUrl,
  })

  // URL 붙여넣기 → 문제 매칭 (URL일 때만)
  const urlQuery = useQuery({
    queryKey: ['problems', 'lookup', search],
    queryFn: () => problemsApi.lookupByUrl(search),
    enabled: isUrl,
    retry: false, // 404(매칭 실패)는 재시도 의미 없음
  })

  // URL 매칭 성공 시 자동 선택
  useEffect(() => {
    if (urlQuery.data) onPick(urlQuery.data)
  }, [urlQuery.data, onPick])

  return (
    <div>
      <div className="relative">
        <svg className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z" />
        </svg>
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="문제 제목 검색 또는 프로그래머스 URL 붙여넣기"
          className="input-field py-2.5 pl-10 pr-3"
        />
      </div>

      {/* URL 붙여넣기 상태 표시 */}
      {isUrl && urlQuery.isFetching && (
        <p className="mt-2 font-mono text-xs text-teal-400">↳ URL로 문제 찾는 중…</p>
      )}
      {isUrl && urlQuery.isError && (
        <p className="mt-2 text-xs text-rose-400">이 URL에 해당하는 문제를 찾지 못했어요. 제목으로 검색해 보세요.</p>
      )}

      {/* 제목 검색 상태/결과 */}
      {!isUrl && search.length > 0 && listQuery.isFetching && (
        <p className="mt-2 text-xs text-slate-500">검색 중…</p>
      )}

      {!isUrl && listQuery.data && listQuery.data.items.length > 0 && (
        <ul className="mt-2 divide-y divide-slate-800 overflow-hidden rounded-md border border-slate-800">
          {listQuery.data.items.map((p) => (
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
      {!isUrl && listQuery.data && listQuery.data.items.length === 0 && (
        <p className="mt-2 text-xs text-slate-500">검색 결과가 없습니다.</p>
      )}
    </div>
  )
}
