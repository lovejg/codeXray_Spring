import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { communityApi } from '../api/community'
import { REPORT_STATUS_LABEL, REPORT_STATUS_COLOR, type Report, type ReportStatus } from '../types'
import { timeAgo } from '../lib/date'
import Spinner from '../components/common/Spinner'
import PostTypeBadge from '../components/common/PostTypeBadge'
import AuthorName from '../components/common/AuthorName'

export default function AdminReportsPage() {
  const [status, setStatus] = useState<ReportStatus | ''>('OPEN')

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'reports', status],
    queryFn: () => communityApi.listReports(status || undefined),
  })

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-bold text-white">신고함</h1>
        <select value={status} onChange={(e) => setStatus(e.target.value as ReportStatus | '')} className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1.5 text-sm text-slate-200 outline-none focus:border-sky-500">
          <option value="">전체</option>
          <option value="OPEN">{REPORT_STATUS_LABEL.OPEN}</option>
          <option value="HANDLED">{REPORT_STATUS_LABEL.HANDLED}</option>
          <option value="DISMISSED">{REPORT_STATUS_LABEL.DISMISSED}</option>
        </select>
      </div>

      {isLoading && <Spinner label="불러오는 중…" />}
      {data && data.length === 0 && <p className="py-16 text-center text-sm text-slate-500">신고가 없습니다.</p>}

      <div className="space-y-3">
        {data?.map((r) => <ReportCard key={r.id} report={r} />)}
      </div>
    </div>
  )
}

function ReportCard({ report }: { report: Report }) {
  const qc = useQueryClient()
  const [note, setNote] = useState(report.adminNote ?? '')
  const c = REPORT_STATUS_COLOR[report.status]

  const resolve = useMutation({
    mutationFn: (s: ReportStatus) => communityApi.updateReport(report.id, s, note || undefined),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reports'] }),
  })
  const hide = useMutation({
    mutationFn: (hidden: boolean) => communityApi.hidePost(report.post.id, hidden),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reports'] }),
  })

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
      <div className="flex items-center gap-2">
        <span className="inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium" style={{ color: c.text, backgroundColor: c.bg }}>
          {REPORT_STATUS_LABEL[report.status]}
        </span>
        <PostTypeBadge type={report.post.type} />
        <Link to={`/community/${report.post.id}`} className="truncate font-medium text-slate-100 hover:text-sky-400">
          {report.post.title}
        </Link>
        {report.post.hidden && <span className="text-xs text-rose-400">숨김됨</span>}
        <span className="ml-auto text-xs text-slate-500">{timeAgo(report.createdAt)}</span>
      </div>

      <div className="mt-2 text-sm text-slate-300">
        <span className="text-slate-500">사유: </span>{report.reason}
      </div>
      <div className="mt-1 text-xs text-slate-500">
        신고자 <AuthorName author={report.reporter} /> · 글쓴이 <AuthorName author={report.post.author} />
      </div>

      {report.status === 'OPEN' && (
        <div className="mt-3 space-y-2">
          <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="관리자 메모(선택)" className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-1.5 text-sm text-white outline-none focus:border-sky-500" />
          <div className="flex flex-wrap gap-2">
            <button onClick={() => hide.mutate(true)} className="rounded-lg border border-amber-700 bg-amber-500/10 px-3 py-1.5 text-sm text-amber-300 hover:bg-amber-500/20">
              게시글 숨김
            </button>
            <button onClick={() => resolve.mutate('HANDLED')} className="rounded-lg border border-emerald-700 bg-emerald-500/10 px-3 py-1.5 text-sm text-emerald-300 hover:bg-emerald-500/20">
              처리 완료
            </button>
            <button onClick={() => resolve.mutate('DISMISSED')} className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800">
              기각
            </button>
          </div>
        </div>
      )}

      {report.adminNote && report.status !== 'OPEN' && (
        <p className="mt-2 text-xs text-slate-500">메모: {report.adminNote}</p>
      )}
    </div>
  )
}
