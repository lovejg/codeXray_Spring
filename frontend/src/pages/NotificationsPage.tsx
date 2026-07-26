import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Check, Trash2 } from 'lucide-react'
import { notificationsApi } from '../api/notifications'
import type { AppNotification } from '../types'
import { formatNotification } from '../lib/formatNotification'
import { timeAgo } from '../lib/date'
import Spinner from '../components/common/Spinner'

export default function NotificationsPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [items, setItems] = useState<AppNotification[]>([])
  const [cursor, setCursor] = useState<number | null>(null)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(true)

  async function load(next: number | null) {
    setLoading(true)
    const res = await notificationsApi.list({ cursor: next ?? undefined, limit: 20 })
    setItems((prev) => (next == null ? res.items : [...prev, ...res.items]))
    setCursor(res.nextCursor)
    setHasMore(res.nextCursor != null)
    setLoading(false)
  }

  useEffect(() => {
    void load(null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function refreshBadge() {
    qc.invalidateQueries({ queryKey: ['notifications', 'unread-count'] })
  }

  async function markAllRead() {
    await notificationsApi.markAllRead()
    setItems((prev) => prev.map((n) => ({ ...n, isRead: true })))
    refreshBadge()
  }

  async function onClickItem(n: AppNotification) {
    if (!n.isRead) {
      await notificationsApi.markRead([n.id])
      setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, isRead: true } : x)))
      refreshBadge()
    }
    const { link } = formatNotification(n)
    if (link) navigate(link)
  }

  async function remove(id: number) {
    await notificationsApi.remove(id)
    setItems((prev) => prev.filter((n) => n.id !== id))
    refreshBadge()
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-5 flex items-center gap-3">
        <h1 className="page-title">알림</h1>
        <button onClick={markAllRead} className="btn-ghost ml-auto px-4 py-2">
          <Check size={14} /> 모두 읽음
        </button>
      </div>

      {loading && items.length === 0 && <Spinner label="불러오는 중…" />}
      {!loading && items.length === 0 && <p className="py-16 text-center text-sm text-slate-500">알림이 없습니다.</p>}

      <div className="space-y-2">
        {items.map((n) => {
          const { text } = formatNotification(n)
          return (
            <div
              key={n.id}
              className={`flex items-start gap-3 rounded-xl border p-3.5 transition ${n.isRead ? 'border-white/10 bg-white/[0.03]' : 'border-teal-500/40 bg-teal-950/20'}`}
            >
              <button onClick={() => onClickItem(n)} className="min-w-0 flex-1 text-left">
                <p className={`text-sm ${n.isRead ? 'text-slate-400' : 'text-slate-100'}`}>{text}</p>
                <p className="mt-1 text-xs text-slate-600">{timeAgo(n.createdAt)}</p>
              </button>
              {!n.isRead && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-teal-400" />}
              <button onClick={() => remove(n.id)} className="text-slate-600 hover:text-rose-400" aria-label="삭제"><Trash2 size={15} /></button>
            </div>
          )
        })}
      </div>

      {hasMore && items.length > 0 && (
        <div className="mt-4 flex justify-center">
          <button onClick={() => load(cursor)} disabled={loading} className="btn-ghost px-4 py-2 disabled:opacity-50">
            {loading ? '불러오는 중…' : '더 보기'}
          </button>
        </div>
      )}
    </div>
  )
}
