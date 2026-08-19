import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/portfolios', label: 'Portfolios' },
  { to: '/assets', label: 'Assets' },
  { to: '/transactions', label: 'Transactions' },
  { to: '/dividends', label: 'Dividends' },
  { to: '/goals', label: 'Goals' },
]

export function Sidebar() {
  return (
    <aside className="hidden w-56 shrink-0 border-r border-border-subtle bg-surface md:block">
      <div className="flex h-14 items-center px-5 text-lg font-semibold text-brand-700">UYInvest</div>
      <nav className="flex flex-col gap-0.5 px-3">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) =>
              cn(
                'rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100',
              )
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
