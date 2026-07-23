import client from './client'
import type { User } from '../types'

export const usersApi = {
  me: () => client.get<User>('/users/me').then((r) => r.data),

  updateNickname: (nickname: string) =>
    client.patch('/users/me/nickname', { nickname }),

  updatePassword: (currentPassword: string, newPassword: string) =>
    client.patch('/users/me/password', { currentPassword, newPassword }),

  deleteAccount: () => client.delete('/users/me'),
}
