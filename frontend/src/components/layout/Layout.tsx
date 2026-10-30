import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import Spinner from '../common/Spinner'
import Navbar from './Navbar'
import StatusBar from './StatusBar'

export default function Layout() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-6 pb-16 pt-10">
        {/* 화면 코드를 받는 동안 메뉴와 상태줄은 그대로 두고 본문 자리만 로딩 표시 */}
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </main>
      <StatusBar />
    </div>
  )
}
