import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import StatusBar from './StatusBar'

export default function Layout() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-6 pb-16 pt-10">
        <Outlet />
      </main>
      <StatusBar />
    </div>
  )
}
