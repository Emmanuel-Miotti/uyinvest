import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

type Tone = 'neutral' | 'success' | 'danger' | 'brand'

const toneClasses: Record<Tone, string> = {
  neutral: 'bg-slate-100 text-slate-700',
  success: 'bg-success-100 text-success-500',
  danger: 'bg-danger-100 text-danger-500',
  brand: 'bg-brand-100 text-brand-700',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium', toneClasses[tone])}>
      {children}
    </span>
  )
}
