import type { ReactNode } from 'react'

export function TableContainer({ children }: { children: ReactNode }) {
  return <div className="overflow-x-auto">{children}</div>
}
