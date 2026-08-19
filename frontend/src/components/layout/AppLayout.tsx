import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { Sidebar } from '@/components/layout/Sidebar'
import { Topbar } from '@/components/layout/Topbar'

export function AppLayout() {
  const token = useAuthStore((state) => state.token)
  const setAuth = useAuthStore((state) => state.setAuth)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  const meQuery = useQuery({ queryKey: ['me'], queryFn: authApi.me, enabled: !!token })

  useEffect(() => {
    if (meQuery.data && token) {
      setAuth(token, meQuery.data)
    }
  }, [meQuery.data, token, setAuth])

  return (
    <div className="flex min-h-screen bg-surface-muted">
      <Sidebar open={mobileNavOpen} onNavigate={() => setMobileNavOpen(false)} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onMenuClick={() => setMobileNavOpen(true)} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
