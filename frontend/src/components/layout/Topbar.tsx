import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/Button'

export function Topbar() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="flex h-14 items-center justify-between border-b border-border-subtle bg-surface px-6">
      <div className="text-sm text-slate-500">Investment Portfolio Management</div>
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate('/profile')}
          className="text-sm font-medium text-slate-700 hover:text-brand-700"
        >
          {user?.name ?? 'Profile'}
        </button>
        <Button variant="secondary" size="sm" onClick={handleLogout}>
          Log out
        </Button>
      </div>
    </header>
  )
}
