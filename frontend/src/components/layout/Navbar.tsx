import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore, useIsAdmin } from '../../store/authStore'
import NotificationBell from './NotificationBell'
import Avatar from '../common/Avatar'

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
    <header className="sticky top-0 z-20 border-b border-slate-800 bg-[#0a0d13]">
      {/* 좁은 화면: 로고와 계정 메뉴가 첫 줄, 메뉴는 둘째 줄에 가로로 밀어 보는 한 줄 */}
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-7 px-4 sm:h-14 sm:flex-nowrap sm:px-6">
        <Link to="/" className="flex h-12 items-center font-mono text-base font-bold text-white sm:h-auto">
          codeXray
        </Link>

        <nav className="order-last -mx-4 flex w-[calc(100%+2rem)] items-center gap-0.5 overflow-x-auto border-t border-slate-800 px-2 py-1 font-mono text-sm sm:order-none sm:mx-0 sm:w-auto sm:overflow-visible sm:border-0 sm:p-0">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `shrink-0 whitespace-nowrap rounded-md px-3 py-1.5 transition ${
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
                `shrink-0 whitespace-nowrap rounded-md px-3 py-1.5 transition ${
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
                title="프로필"
                className="flex items-center gap-2 rounded-md px-2 py-1 text-sm text-slate-300 transition hover:bg-white/5 hover:text-white"
              >
                <Avatar name={user.nickname} size={22} />
                <span className="max-w-[6rem] truncate sm:max-w-[9rem]">{user.nickname}</span>
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
                className="whitespace-nowrap rounded-md px-3 py-1.5 text-sm text-slate-300 transition hover:bg-white/5 hover:text-white"
              >
                로그인
              </Link>
              <Link
                to="/register"
                className="whitespace-nowrap rounded-md border border-teal-400/40 px-3 py-1 text-sm text-teal-300 transition hover:bg-teal-400/10"
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
