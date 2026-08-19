import { Select } from '@/components/ui/Input'
import type { Portfolio } from '@/types'

interface PortfolioSelectorProps {
  portfolios: Portfolio[]
  value: string
  onChange: (portfolioId: string) => void
}

export function PortfolioSelector({ portfolios, value, onChange }: PortfolioSelectorProps) {
  return (
    <Select value={value} onChange={(e) => onChange(e.target.value)} className="max-w-xs">
      {portfolios.map((portfolio) => (
        <option key={portfolio.id} value={portfolio.id}>
          {portfolio.name} ({portfolio.baseCurrency})
        </option>
      ))}
    </Select>
  )
}
