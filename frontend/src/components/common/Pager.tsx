// 이전/다음 페이지 버튼. 백엔드 PageResponse(page 는 0부터)를 그대로 받는다.
// 페이지가 하나뿐이면 아무것도 그리지 않는다
const pageBtn =
  'rounded-md border border-slate-800 bg-slate-900/50 px-4 py-2 font-mono text-sm text-slate-300 transition hover:border-teal-400/40 hover:text-teal-300 disabled:cursor-not-allowed disabled:opacity-40'

export default function Pager({
  page,
  totalPages,
  onChange,
  alwaysShow = false,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
  alwaysShow?: boolean
}) {
  if (!alwaysShow && totalPages <= 1) return null

  return (
    <div className="mt-5 flex items-center justify-center gap-2 text-sm">
      <button disabled={page === 0} onClick={() => onChange(page - 1)} className={pageBtn}>이전</button>
      <span className="px-1 text-slate-400">
        {totalPages === 0 ? 0 : page + 1} / {totalPages}
      </span>
      <button disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)} className={pageBtn}>다음</button>
    </div>
  )
}
