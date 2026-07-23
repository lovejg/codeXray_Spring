import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { notificationsApi } from '../../api/notifications'

// 미읽음 카운트를 30초마다 폴링해 배지로 표시.
export default function NotificationBell() {
  const { data: count = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 30_000,
  })

  return (
    <Link
      to="/notifications"
      className="relative inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white"
      aria-label="알림"
    >
      <Bell size={18} />
      {count > 0 && (
        <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
