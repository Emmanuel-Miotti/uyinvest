import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { portfoliosApi } from '@/api/portfolios'
import { transactionsApi } from '@/api/transactions'
import { dividendsApi } from '@/api/dividends'
import { Card, CardContent, CardHeader } from '@/components/ui/Card'
import { TableContainer } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { cn, formatCurrency, formatDate, formatPercentage } from '@/lib/utils'

type Tab = 'transactions' | 'dividends'

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('transactions')

  const portfolioQuery = useQuery({
    queryKey: ['portfolio', id],
    queryFn: () => portfoliosApi.get(id!),
    enabled: !!id,
  })

  const summaryQuery = useQuery({
    queryKey: ['portfolio-summary', id],
    queryFn: () => portfoliosApi.summary(id!),
    enabled: !!id,
  })

  const transactionsQuery = useQuery({
    queryKey: ['transactions', id],
    queryFn: () => transactionsApi.list(id!),
    enabled: !!id && tab === 'transactions',
  })

  const dividendsQuery = useQuery({
    queryKey: ['dividends', id],
    queryFn: () => dividendsApi.list(id!),
    enabled: !!id && tab === 'dividends',
  })

  if (portfolioQuery.isLoading) {
    return <Skeleton className="h-40" />
  }

  if (portfolioQuery.isError || !portfolioQuery.data) {
    return <ErrorState message="Portfolio not found." onRetry={() => navigate('/portfolios')} />
  }

  const portfolio = portfolioQuery.data
  const currency = portfolio.baseCurrency

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <button onClick={() => navigate('/portfolios')} className="text-xs text-brand-700 hover:underline">
            ← Back to portfolios
          </button>
          <h1 className="text-xl font-semibold text-slate-800">{portfolio.name}</h1>
        </div>
        <Button
          onClick={() =>
            navigate(tab === 'transactions' ? `/transactions?portfolioId=${portfolio.id}` : `/dividends?portfolioId=${portfolio.id}`)
          }
        >
          {tab === 'transactions' ? 'New transaction' : 'New dividend'}
        </Button>
      </div>

      {summaryQuery.data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Current value</p>
              <p className="mt-2 text-xl font-semibold text-slate-800">{formatCurrency(summaryQuery.data.currentValue, currency)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Profit / Loss</p>
              <p className={cn('mt-2 text-xl font-semibold', summaryQuery.data.profitLoss >= 0 ? 'text-success-500' : 'text-danger-500')}>
                {formatCurrency(summaryQuery.data.profitLoss, currency)}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Return</p>
              <p
                className={cn(
                  'mt-2 text-xl font-semibold',
                  summaryQuery.data.profitLossPercentage >= 0 ? 'text-success-500' : 'text-danger-500',
                )}
              >
                {formatPercentage(summaryQuery.data.profitLossPercentage)}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader className="border-b-0 pb-0">
          <div className="flex gap-1">
            <button
              onClick={() => setTab('transactions')}
              className={cn('rounded-t-md px-3 py-2 text-sm font-medium', tab === 'transactions' ? 'border-b-2 border-brand-600 text-brand-700' : 'text-slate-500')}
            >
              Transactions
            </button>
            <button
              onClick={() => setTab('dividends')}
              className={cn('rounded-t-md px-3 py-2 text-sm font-medium', tab === 'dividends' ? 'border-b-2 border-brand-600 text-brand-700' : 'text-slate-500')}
            >
              Dividends
            </button>
          </div>
        </CardHeader>
        <CardContent>
          {tab === 'transactions' && (
            <>
              {transactionsQuery.isLoading && <Skeleton className="h-32" />}
              {transactionsQuery.isError && <ErrorState onRetry={() => transactionsQuery.refetch()} />}
              {transactionsQuery.data && transactionsQuery.data.length === 0 && (
                <EmptyState title="No transactions yet" description="Register a buy or sell to see it here." />
              )}
              {transactionsQuery.data && transactionsQuery.data.length > 0 && (
                <TableContainer>
                  <table className="w-full min-w-[620px] text-sm">
                    <thead>
                      <tr className="border-b border-border-subtle text-left text-xs uppercase text-slate-400">
                        <th className="pb-2 font-medium">Date</th>
                        <th className="pb-2 font-medium">Asset</th>
                        <th className="pb-2 font-medium">Type</th>
                        <th className="pb-2 font-medium text-right">Quantity</th>
                        <th className="pb-2 font-medium text-right">Price</th>
                        <th className="pb-2 font-medium text-right">Commission</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactionsQuery.data.map((tx) => (
                        <tr key={tx.id} className="border-b border-border-subtle last:border-0">
                          <td className="py-2 text-slate-500">{formatDate(tx.transactionDate)}</td>
                          <td className="py-2 font-medium text-slate-700">{tx.asset.symbol}</td>
                          <td className="py-2">
                            <Badge tone={tx.type === 'BUY' ? 'success' : 'danger'}>{tx.type}</Badge>
                          </td>
                          <td className="py-2 text-right text-slate-600">{tx.quantity}</td>
                          <td className="py-2 text-right text-slate-600">{formatCurrency(tx.price, tx.currency)}</td>
                          <td className="py-2 text-right text-slate-600">{formatCurrency(tx.commission, tx.currency)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </TableContainer>
              )}
            </>
          )}

          {tab === 'dividends' && (
            <>
              {dividendsQuery.isLoading && <Skeleton className="h-32" />}
              {dividendsQuery.isError && <ErrorState onRetry={() => dividendsQuery.refetch()} />}
              {dividendsQuery.data && dividendsQuery.data.length === 0 && (
                <EmptyState title="No dividends yet" description="Register a dividend payment to see it here." />
              )}
              {dividendsQuery.data && dividendsQuery.data.length > 0 && (
                <TableContainer>
                  <table className="w-full min-w-[420px] text-sm">
                    <thead>
                      <tr className="border-b border-border-subtle text-left text-xs uppercase text-slate-400">
                        <th className="pb-2 font-medium">Payment date</th>
                        <th className="pb-2 font-medium">Asset</th>
                        <th className="pb-2 font-medium text-right">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dividendsQuery.data.map((dividend) => (
                        <tr key={dividend.id} className="border-b border-border-subtle last:border-0">
                          <td className="py-2 text-slate-500">{formatDate(dividend.paymentDate)}</td>
                          <td className="py-2 font-medium text-slate-700">{dividend.asset.symbol}</td>
                          <td className="py-2 text-right text-slate-600">{formatCurrency(dividend.amount, dividend.currency)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </TableContainer>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
