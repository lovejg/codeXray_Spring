import client from './client'
import type { User } from '../types'

interface LoginResult {
  accessToken: string
}

export const authApi = {
  register: (email: string, password: string, nickname: string) =>
    client.post('/auth/register', { email, password, nickname }),

  verifyEmail: (token: string) => client.post('/auth/verify-email', { token }),

  resendVerification: (email: string) =>
    client.post('/auth/resend-verification', { email }),

  login: (email: string, password: string) =>
    client.post<LoginResult>('/auth/login', { email, password }).then((r) => r.data),

  oauthGoogle: (code: string) =>
    client.post<LoginResult>('/auth/oauth/google', { code }).then((r) => r.data),

  oauthNaver: (code: string, state: string) =>
    client.post<LoginResult>('/auth/oauth/naver', { code, state }).then((r) => r.data),

  logout: () => client.post('/auth/logout'),

  // 로그인/OAuth 후 access token 으로 내 정보 조회
  me: () => client.get<User>('/users/me').then((r) => r.data),
}
