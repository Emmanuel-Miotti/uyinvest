import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { portfoliosApi } from '@/api/portfolios'
import { dividendsApi } from '@/api/dividends'
import { assetsApi } from '@/api/assets'
import { PortfolioSelector } from '@/components/PortfolioSelector'
import { Card, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input, Label, Select } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { extractErrorMessage, formatCurrency, formatDate } from '@/lib/utils'

const emptyForm = { assetId: '', amount: '', currency: 'USD', paymentDate: new Date().toISOString().slice(0, 10) }

export function DividendsPage() {
  const [searchParams] = useSearchParams()
  const queryClient = useQueryClient()
  const [portfolioId, setPortfolioId] = useState(searchParams.get('portfolioId') ?? '')
  const [isFormOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(emptyForm)

  const portfoliosQuery = useQuery({ queryKey: ['portfolios'], queryFn: portfoliosApi.list })
  const assetsQuery = useQuery({ queryKey: ['assets', 'all'], queryFn: () => assetsApi.search({ size: 100 }) })

  useEffect(() => {
    if (!portfolioId && portfoliosQuery.data && portfoliosQuery.data.length > 0) {
      setPortfolioId(portfoliosQuery.data[0].id)
    }
  }, [portfoliosQuery.data, portfolioId])

  const dividendsQuery = useQuery({
    queryKey: ['dividends', portfolioId],
    queryFn: () => dividendsApi.list(portfolioId),
    enabled: !!portfolioId,
  })

  const summaryQuery = useQuery({
    queryKey: ['dividends-summary', portfolioId],
    queryFn: () => dividendsApi.summary(portfolioId),
    enabled: !!portfolioId,
  })

  const createMutation = useMutation({
    mutationFn: () =>
      dividendsApi.create(portfolioId, {
        assetId: form.assetId,
        amount: Number(form.amount),
        currency: form.currency,
        paymentDate: form.paymentDate,
      }),
    onSuccess: () => {
      toast.success('Dividend registered')
      queryClient.invalidateQueries({ queryKey: ['dividends', portfolioId] })
      queryClient.invalidateQueries({ queryKey: ['dividends-summary', portfolioId] })
      setFormOpen(false)
      setForm(emptyForm)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not register dividend')),
  })

  function openCreate() {
    const firstAsset = assetsQuery.data?.content[0]
    setForm({ ...emptyForm, assetId: firstAsset?.id ?? '', currency: firstAsset?.currency ?? 'USD' })
    setFormOpen(true)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    createMutation.mutate()
  }

  if (portfoliosQuery.isLoading) return <Skeleton className="h-64" />
  if (portfoliosQuery.isError) return <ErrorState onRetry={() => portfoliosQuery.refetch()} />

  const portfolios = portfoliosQuery.data ?? []

  if (portfolios.length === 0) {
    return <EmptyState title="No portfolios yet" description="Create a portfolio before registering dividends." />
  }

  const currency = portfolios.find((p) => p.id === portfolioId)?.baseCurrency ?? 'USD'

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-800">Dividends</h1>
        <div className="flex items-center gap-3">
          <PortfolioSelector portfolios={portfolios} value={portfolioId} onChange={setPortfolioId} />
          <Button onClick={openCreate} disabled={!assetsQuery.data?.content.length}>
            New dividend
          </Button>
        </div>
      </div>

      {summaryQuery.data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">This month</p>
              <p className="mt-2 text-xl font-semibold text-slate-800">{formatCurrency(summaryQuery.data.totalThisMonth, currency)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">This year</p>
              <p className="mt-2 text-xl font-semibold text-slate-800">{formatCurrency(summaryQuery.data.totalThisYear, currency)}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Historical</p>
              <p className="mt-2 text-xl font-semibold text-slate-800">{formatCurrency(summaryQuery.data.totalHistorical, currency)}</p>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardContent>
          {dividendsQuery.isLoading && <Skeleton className="h-64" />}
          {dividendsQuery.isError && <ErrorState onRetry={() => dividendsQuery.refetch()} />}
          {dividendsQuery.data && dividendsQuery.data.length === 0 && (
            <EmptyState title="No dividends yet" description="Register a dividend payment to see it here." />
          )}
          {dividendsQuery.data && dividendsQuery.data.length > 0 && (
            <table className="w-full text-sm">
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
          )}
        </CardContent>
      </Card>

      <Modal open={isFormOpen} onClose={() => setFormOpen(false)} title="New dividend">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="div-asset">Asset</Label>
            <Select id="div-asset" required value={form.assetId} onChange={(e) => setForm((f) => ({ ...f, assetId: e.target.value }))}>
              {assetsQuery.data?.content.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.symbol} — {asset.name}
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="div-amount">Amount</Label>
              <Input
                id="div-amount"
                type="number"
                step="any"
                min="0"
                required
                value={form.amount}
                onChange={(e) => setForm((f) => ({ ...f, amount: e.target.value }))}
              />
            </div>
            <div>
              <Label htmlFor="div-currency">Currency</Label>
              <Input
                id="div-currency"
                required
                maxLength={3}
                value={form.currency}
                onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value.toUpperCase() }))}
              />
            </div>
          </div>
          <div>
            <Label htmlFor="div-date">Payment date</Label>
            <Input
              id="div-date"
              type="date"
              required
              max={new Date().toISOString().slice(0, 10)}
              value={form.paymentDate}
              onChange={(e) => setForm((f) => ({ ...f, paymentDate: e.target.value }))}
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setFormOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={createMutation.isPending}>
              Save
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
