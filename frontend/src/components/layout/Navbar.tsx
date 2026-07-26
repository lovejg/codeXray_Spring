import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore, useIsAdmin } from '../../store/authStore'
import NotificationBell from './NotificationBell'

const navItems = [
  { to: '/problems', label: '문제' },
  { to: '/solutions', label: '내 풀이' },
  { to: '/notes', label: '노트' },
  { to: '/community', label: '커뮤니티' },
  { to: '/suggestions', label: '건의사항' },
]

export default function Navbar() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const isAdmin = useIsAdmin()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-20 border-b border-slate-800 bg-[#0a0d13]/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-7 px-6">
        <Link to="/problems" className="flex items-center gap-2 font-mono text-lg font-bold">
          <span className="text-teal-400">❯</span>
          <span className="text-white">code<span className="text-teal-400">Xray</span></span>
          <span className="cursor-blink -ml-1 text-teal-400">▊</span>
        </Link>

        <nav className="flex items-center gap-0.5 font-mono text-sm">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 transition ${
                  isActive
                    ? 'bg-teal-400/10 text-teal-300'
                    : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink
              to="/admin/reports"
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 transition ${
                  isActive ? 'bg-amber-500/15 text-amber-300' : 'text-amber-400/70 hover:text-amber-300'
                }`
              }
            >
              신고함
            </NavLink>
          )}
        </nav>

        <div className="ml-auto flex items-center gap-1 font-mono">
          {user ? (
            <>
              <NotificationBell />
              <Link
                to="/profile"
                className="rounded-md px-3 py-1.5 text-sm text-slate-300 transition hover:bg-white/5 hover:text-white"
              >
                <span className="text-teal-400/70">@</span>{user.nickname}
              </Link>
              <button
                onClick={handleLogout}
                className="rounded-md px-3 py-1.5 text-sm text-slate-500 transition hover:bg-white/5 hover:text-white"
              >
                logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-md px-3 py-1.5 text-sm text-slate-300 transition hover:bg-white/5 hover:text-white"
              >
                로그인
              </Link>
              <Link to="/register" className="btn-primary">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
