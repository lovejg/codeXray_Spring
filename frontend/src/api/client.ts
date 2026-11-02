import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { clearAccessToken, getAccessToken, setAccessToken } from '../lib/tokens'

// 기본은 '/api'(dev: Vite 프록시, prod: 백엔드와 same-origin).
// 다른 오리진 배포 시 VITE_API_BASE 로 백엔드 절대주소 지정(예: https://api.example.com/api).
const API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? '/api'

const client = axios.create({
  baseURL: API_BASE,
  withCredentials: true, // refresh httpOnly 쿠키 전송
})

client.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 이 탭에서 동시에 여러 요청이 401 을 받아도 refresh 는 한 번만 (thundering herd 방지)
let refreshPromise: Promise<string> | null = null

async function callRefresh(): Promise<string> {
  // refresh 토큰은 httpOnly 쿠키에 있으므로 body 없이 호출. 인터셉터 재귀 방지로 raw axios 사용.
  const { data } = await axios.post<{ accessToken: string }>(
    `${API_BASE}/auth/refresh`,
    {},
    { withCredentials: true },
  )
  setAccessToken(data.accessToken)
  return data.accessToken
}

// 서버는 refresh 토큰을 쓸 때마다 새것으로 바꾸고 헌것은 GETDEL 로 지운다(회전).
// access token 을 메모리에 두면 탭마다 시작할 때 refresh 를 하는데, 탭 여러 개가 같은 쿠키로 동시에 보내면
// 하나만 성공하고 나머지는 401 로 로그아웃된다(브라우저를 다시 열어 탭이 한꺼번에 복원될 때).
// 브라우저 전체에 걸리는 잠금(Web Locks)으로 줄을 세우면, 뒤 탭은 앞 탭이 받아 둔 새 쿠키로 요청하게 된다
export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const run = 'locks' in navigator ? navigator.locks.request('codexray-refresh', callRefresh) : callRefresh()
    refreshPromise = run.finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
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
        const newToken = await refreshAccessToken()
        original.headers.Authorization = `Bearer ${newToken}`
        return client(original)
      } catch (refreshErr) {
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
