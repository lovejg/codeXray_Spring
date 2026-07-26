import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

// 화면 하단 IDE 스타일 상태바. 실시간 시계 + 접속 상태 + 현재 경로 + 버전.
const APP_VERSION = 'v0.1.0'

export default function StatusBar() {
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(t)
  }, [])

  const clock = now.toLocaleTimeString('en-GB') // HH:MM:SS

  return (
    <footer className="fixed inset-x-0 bottom-0 z-20 h-7 border-t border-slate-800 bg-[#0a0d13]/95 backdrop-blur">
      <div className="mx-auto flex h-full max-w-6xl items-center gap-4 px-6 font-mono text-[11px] text-slate-500">
        <span className="flex items-center gap-1.5 text-teal-400">
          <span className="h-1.5 w-1.5 rounded-full bg-teal-400 shadow-[0_0_6px_#2dd4bf]" />
          ready
        </span>
        <span className="text-slate-600">{location.pathname}</span>
        {user && <span className="text-slate-500">@{user.nickname}</span>}
        <span className="ml-auto tabular-nums text-slate-500">{clock}</span>
        <span className="text-slate-600">codeXray {APP_VERSION}</span>
      </div>
    </footer>
  )
}
