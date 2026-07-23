import { useEffect, useState } from 'react'
import { authApi } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { getAccessToken } from '../lib/tokens'

// 앱 시작 시 세션 복구: access token 이 있으면 /users/me 로 유저를 채운다.
// (만료됐어도 인터셉터가 refresh 쿠키로 자동 재발급 후 재시도)
export function useBootstrapAuth(): boolean {
  const setUser = useAuthStore((s) => s.setUser)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let alive = true
    async function run() {
      if (!getAccessToken()) {
        setReady(true)
        return
      }
      try {
        const user = await authApi.me()
        if (alive) setUser(user)
      } catch {
        // 세션 없음/만료 → 비로그인 상태로 진행
      } finally {
        if (alive) setReady(true)
      }
    }
    void run()
    return () => {
      alive = false
    }
  }, [setUser])

  return ready
}
