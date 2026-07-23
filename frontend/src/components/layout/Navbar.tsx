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
    <header className="sticky top-0 z-20 border-b border-slate-800 bg-slate-950/80 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
        <Link to="/problems" className="flex items-center gap-1.5 font-bold tracking-tight">
          <span className="text-sky-400">code</span>
          <span className="text-white">Xray</span>
        </Link>

        <nav className="flex items-center gap-1 text-sm">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `rounded-lg px-3 py-1.5 transition ${
                  isActive
                    ? 'bg-slate-800 text-white'
                    : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
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
                `rounded-lg px-3 py-1.5 transition ${
                  isActive ? 'bg-amber-500/20 text-amber-300' : 'text-amber-400/70 hover:text-amber-300'
                }`
              }
            >
              신고함
            </NavLink>
          )}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {user ? (
            <>
              <NotificationBell />
              <Link
                to="/profile"
                className="rounded-lg px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800 hover:text-white"
              >
                {user.nickname}
              </Link>
              <button
                onClick={handleLogout}
                className="rounded-lg px-3 py-1.5 text-sm text-slate-400 hover:bg-slate-800 hover:text-white"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-lg px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800 hover:text-white"
              >
                로그인
              </Link>
              <Link
                to="/register"
                className="rounded-lg bg-sky-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-sky-400"
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
