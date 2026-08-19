import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { portfoliosApi } from '@/api/portfolios'
import { transactionsApi } from '@/api/transactions'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { PortfolioSelector } from '@/components/PortfolioSelector'
import { cn, formatCurrency, formatDate, formatPercentage } from '@/lib/utils'

const ALLOCATION_COLORS = ['#3466ff', '#598cff', '#8eb4ff', '#16a34a', '#f59e0b', '#dc2626']

export function DashboardPage() {
  const navigate = useNavigate()
  const [portfolioId, setPortfolioId] = useState<string>('')

  const portfoliosQuery = useQuery({ queryKey: ['portfolios'], queryFn: portfoliosApi.list })

  useEffect(() => {
    if (!portfolioId && portfoliosQuery.data && portfoliosQuery.data.length > 0) {
      setPortfolioId(portfoliosQuery.data[0].id)
    }
  }, [portfoliosQuery.data, portfolioId])

  const summaryQuery = useQuery({
    queryKey: ['portfolio-summary', portfolioId],
    queryFn: () => portfoliosApi.summary(portfolioId),
    enabled: !!portfolioId,
  })

  const allocationQuery = useQuery({
    queryKey: ['portfolio-allocation', portfolioId],
    queryFn: () => portfoliosApi.allocation(portfolioId),
    enabled: !!portfolioId,
  })

  const performanceQuery = useQuery({
    queryKey: ['portfolio-performance', portfolioId],
    queryFn: () => portfoliosApi.performance(portfolioId),
    enabled: !!portfolioId,
  })

  const transactionsQuery = useQuery({
    queryKey: ['transactions', portfolioId],
    queryFn: () => transactionsApi.list(portfolioId),
    enabled: !!portfolioId,
  })

  if (portfoliosQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-9 w-64" />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
      </div>
    )
  }

  if (portfoliosQuery.isError) {
    return <ErrorState onRetry={() => portfoliosQuery.refetch()} />
  }

  const portfolios = portfoliosQuery.data ?? []

  if (portfolios.length === 0) {
    return (
      <EmptyState
        title="No portfolios yet"
        description="Create your first portfolio to start tracking your investments."
        action={<Button onClick={() => navigate('/portfolios')}>Create portfolio</Button>}
      />
    )
  }

  const currency = portfolios.find((p) => p.id === portfolioId)?.baseCurrency ?? 'USD'

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-800">Dashboard</h1>
        <PortfolioSelector portfolios={portfolios} value={portfolioId} onChange={setPortfolioId} />
      </div>

      {summaryQuery.isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
      )}

      {summaryQuery.isError && <ErrorState onRetry={() => summaryQuery.refetch()} />}

      {summaryQuery.data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Portfolio value</p>
              <p className="mt-2 text-2xl font-semibold text-slate-800">
                {formatCurrency(summaryQuery.data.currentValue, currency)}
              </p>
              <p className="mt-1 text-xs text-slate-400">Invested: {formatCurrency(summaryQuery.data.totalInvested, currency)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Profit / Loss</p>
              <p
                className={cn(
                  'mt-2 text-2xl font-semibold',
                  summaryQuery.data.profitLoss >= 0 ? 'text-success-500' : 'text-danger-500',
                )}
              >
                {formatCurrency(summaryQuery.data.profitLoss, currency)}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Return</p>
              <p
                className={cn(
                  'mt-2 text-2xl font-semibold',
                  summaryQuery.data.profitLossPercentage >= 0 ? 'text-success-500' : 'text-danger-500',
                )}
              >
                {formatPercentage(summaryQuery.data.profitLossPercentage)}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Allocation by asset type</CardTitle>
          </CardHeader>
          <CardContent>
            {allocationQuery.isLoading && <Skeleton className="h-64" />}
            {allocationQuery.isError && <ErrorState onRetry={() => allocationQuery.refetch()} />}
            {allocationQuery.data && allocationQuery.data.length === 0 && (
              <EmptyState title="No open positions" description="Buy an asset to see your allocation." />
            )}
            {allocationQuery.data && allocationQuery.data.length > 0 && (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={allocationQuery.data}
                    dataKey="currentValue"
                    nameKey="assetType"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={allocationQuery.data.length > 1 ? 2 : 0}
                    isAnimationActive={false}
                  >
                    {allocationQuery.data.map((entry, index) => (
                      <Cell key={entry.assetType} fill={ALLOCATION_COLORS[index % ALLOCATION_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => formatCurrency(Number(value), currency)} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Invested capital over time</CardTitle>
          </CardHeader>
          <CardContent>
            {performanceQuery.isLoading && <Skeleton className="h-64" />}
            {performanceQuery.isError && <ErrorState onRetry={() => performanceQuery.refetch()} />}
            {performanceQuery.data && performanceQuery.data.length === 0 && (
              <EmptyState title="No transactions yet" description="Your investment timeline will appear here." />
            )}
            {performanceQuery.data && performanceQuery.data.length > 0 && (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={performanceQuery.data}>
                  <XAxis dataKey="date" tickFormatter={(v) => formatDate(String(v))} tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} width={70} tickFormatter={(v) => formatCurrency(Number(v), currency)} />
                  <Tooltip
                    labelFormatter={(label) => formatDate(String(label))}
                    formatter={(value) => formatCurrency(Number(value), currency)}
                  />
                  <Line type="monotone" dataKey="totalInvested" stroke="#3466ff" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent transactions</CardTitle>
        </CardHeader>
        <CardContent>
          {transactionsQuery.isLoading && <Skeleton className="h-40" />}
          {transactionsQuery.isError && <ErrorState onRetry={() => transactionsQuery.refetch()} />}
          {transactionsQuery.data && transactionsQuery.data.length === 0 && (
            <EmptyState title="No transactions yet" description="Buy or sell an asset to see it here." />
          )}
          {transactionsQuery.data && transactionsQuery.data.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-subtle text-left text-xs uppercase text-slate-400">
                  <th className="pb-2 font-medium">Date</th>
                  <th className="pb-2 font-medium">Asset</th>
                  <th className="pb-2 font-medium">Type</th>
                  <th className="pb-2 font-medium text-right">Quantity</th>
                  <th className="pb-2 font-medium text-right">Price</th>
                </tr>
              </thead>
              <tbody>
                {transactionsQuery.data.slice(0, 5).map((tx) => (
                  <tr key={tx.id} className="border-b border-border-subtle last:border-0">
                    <td className="py-2 text-slate-500">{formatDate(tx.transactionDate)}</td>
                    <td className="py-2 font-medium text-slate-700">{tx.asset.symbol}</td>
                    <td className="py-2">
                      <Badge tone={tx.type === 'BUY' ? 'success' : 'danger'}>{tx.type}</Badge>
                    </td>
                    <td className="py-2 text-right text-slate-600">{tx.quantity}</td>
                    <td className="py-2 text-right text-slate-600">{formatCurrency(tx.price, tx.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
