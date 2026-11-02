import { useEffect, useState } from 'react'
import { authApi } from '../api/auth'
import { refreshAccessToken } from '../api/client'
import { useAuthStore } from '../store/authStore'
import { clearAccessToken, hasSessionHint } from '../lib/tokens'

// 앱 시작 시 세션 복구. access token 은 메모리에만 있어 새로고침하면 없으므로,
// 로그인한 적이 있는 브라우저면 refresh 쿠키로 새 access token 을 받고 /users/me 로 유저를 채운다.
// refresh 는 인터셉터를 거치지 않고 바로 부른다: 실패해도 /login 으로 보내지 않고 비로그인 상태로 둔다(공개 화면이 많다)
export function useBootstrapAuth(): boolean {
  const setAuth = useAuthStore((s) => s.setAuth)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let alive = true
    async function run() {
      if (!hasSessionHint()) {
        setReady(true)
        return
      }
      try {
        const token = await refreshAccessToken()
        const user = await authApi.me()
        if (alive) setAuth(user, token)
      } catch {
        // 세션 없음/만료 → 비로그인 상태로 진행
        clearAccessToken()
      } finally {
        if (alive) setReady(true)
      }
    }
    void run()
    return () => {
      alive = false
    }
  }, [setAuth])

  return ready
}
