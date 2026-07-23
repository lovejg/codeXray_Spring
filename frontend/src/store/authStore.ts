import { create } from 'zustand'
import type { User } from '../types'
import { clearAccessToken, setAccessToken } from '../lib/tokens'
import client from '../api/client'

interface AuthState {
  user: User | null
  token: string | null
  setAuth: (user: User, token: string) => void
  setUser: (user: User) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: localStorage.getItem('token'),
  setAuth: (user, token) => {
    setAccessToken(token)
    set({ user, token })
  },
  setUser: (user) => set({ user }),
  logout: () => {
    // 서버의 refresh 세션도 폐기(쿠키). 실패해도 로컬 로그아웃은 진행.
    void client.post('/auth/logout').catch(() => {})
    clearAccessToken()
    set({ user: null, token: null })
  },
}))

export const useIsAdmin = () => useAuthStore((s) => s.user?.role === 'ADMIN')
