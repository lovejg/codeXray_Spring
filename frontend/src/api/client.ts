import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { clearAccessToken, getAccessToken, setAccessToken } from '../lib/tokens'

const client = axios.create({
  baseURL: '/api',
  withCredentials: true, // refresh httpOnly 쿠키 전송
})

client.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 동시에 여러 요청이 401 을 받아도 refresh 는 한 번만 (thundering herd 방지)
let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  // refresh 토큰은 httpOnly 쿠키에 있으므로 body 없이 호출. 인터셉터 재귀 방지로 raw axios 사용.
  const { data } = await axios.post<{ accessToken: string }>(
    '/api/auth/refresh',
    {},
    { withCredentials: true },
  )
  setAccessToken(data.accessToken)
  return data.accessToken
}

// 로그인 전(세션 없음) 호출들 — 이들의 401/403 은 "토큰 만료"가 아니라
// "자격 증명/인증 상태" 자체의 실패이므로 refresh 재시도를 하면 안 되고,
// 백엔드가 준 구체 메시지(LOGIN_FAILED 등)를 그대로 화면에 흘려보내야 한다.
const NO_REFRESH_PATHS = ['/auth/login', '/auth/register', '/auth/oauth', '/auth/verify', '/auth/resend']

client.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined
    const isRefreshCall = original?.url?.includes('/auth/refresh')
    const isPublicAuthCall = NO_REFRESH_PATHS.some((p) => original?.url?.includes(p))

    // access 만료(401) → refresh 로테이션 후 원 요청 1회 재시도
    // 단, refresh 호출 자신과 공개 인증 엔드포인트는 제외.
    if (error.response?.status === 401 && original && !original._retry && !isRefreshCall && !isPublicAuthCall) {
      original._retry = true
      try {
        refreshPromise = refreshPromise ?? refreshAccessToken()
        const newToken = await refreshPromise
        refreshPromise = null
        original.headers.Authorization = `Bearer ${newToken}`
        return client(original)
      } catch (refreshErr) {
        refreshPromise = null
        clearAccessToken()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshErr)
      }
    }
    return Promise.reject(error)
  },
)

export default client
