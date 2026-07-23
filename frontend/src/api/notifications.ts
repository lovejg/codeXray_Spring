import client from './client'
import type { NotificationList } from '../types'

export const notificationsApi = {
  list: (params: { onlyUnread?: boolean; cursor?: number; limit?: number }) =>
    client.get<NotificationList>('/notifications', { params }).then((r) => r.data),

  unreadCount: () =>
    client.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count),

  markRead: (ids: number[]) => client.patch('/notifications/read', { ids }),

  markAllRead: () => client.patch('/notifications/read-all'),

  remove: (id: number) => client.delete(`/notifications/${id}`),
}
