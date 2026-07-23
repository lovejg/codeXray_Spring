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

client.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined
    const isRefreshCall = original?.url?.includes('/auth/refresh')

    // access 만료(401) → refresh 로테이션 후 원 요청 1회 재시도
    if (error.response?.status === 401 && original && !original._retry && !isRefreshCall) {
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
