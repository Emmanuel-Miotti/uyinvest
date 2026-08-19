import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/Button'

interface TopbarProps {
  onMenuClick: () => void
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="flex h-14 items-center justify-between border-b border-border-subtle bg-surface px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          aria-label="Open navigation menu"
          className="-ml-1 rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M2.5 5h15M2.5 10h15M2.5 15h15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
        <div className="hidden text-sm text-slate-500 sm:block">Investment Portfolio Management</div>
      </div>
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
