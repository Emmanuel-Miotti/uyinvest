import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { portfoliosApi } from '@/api/portfolios'
import { transactionsApi } from '@/api/transactions'
import { assetsApi } from '@/api/assets'
import type { TransactionType } from '@/types'
import { PortfolioSelector } from '@/components/PortfolioSelector'
import { Card, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input, Label, Select } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { extractErrorMessage, formatCurrency, formatDate } from '@/lib/utils'

const emptyForm = {
  assetId: '',
  type: 'BUY' as TransactionType,
  quantity: '',
  price: '',
  commission: '0',
  currency: 'USD',
  transactionDate: new Date().toISOString().slice(0, 10),
}

export function TransactionsPage() {
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

  const transactionsQuery = useQuery({
    queryKey: ['transactions', portfolioId],
    queryFn: () => transactionsApi.list(portfolioId),
    enabled: !!portfolioId,
  })

  const createMutation = useMutation({
    mutationFn: () =>
      transactionsApi.create(portfolioId, {
        assetId: form.assetId,
        type: form.type,
        quantity: Number(form.quantity),
        price: Number(form.price),
        commission: Number(form.commission || 0),
        currency: form.currency,
        transactionDate: new Date(form.transactionDate).toISOString(),
      }),
    onSuccess: () => {
      toast.success('Transaction registered')
      queryClient.invalidateQueries({ queryKey: ['transactions', portfolioId] })
      queryClient.invalidateQueries({ queryKey: ['portfolio-summary', portfolioId] })
      queryClient.invalidateQueries({ queryKey: ['portfolio-allocation', portfolioId] })
      queryClient.invalidateQueries({ queryKey: ['portfolio-performance', portfolioId] })
      setFormOpen(false)
      setForm(emptyForm)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not register transaction')),
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
    return <EmptyState title="No portfolios yet" description="Create a portfolio before registering transactions." />
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-800">Transactions</h1>
        <div className="flex items-center gap-3">
          <PortfolioSelector portfolios={portfolios} value={portfolioId} onChange={setPortfolioId} />
          <Button onClick={openCreate} disabled={!assetsQuery.data?.content.length}>
            New transaction
          </Button>
        </div>
      </div>

      <Card>
        <CardContent>
          {transactionsQuery.isLoading && <Skeleton className="h-64" />}
          {transactionsQuery.isError && <ErrorState onRetry={() => transactionsQuery.refetch()} />}
          {transactionsQuery.data && transactionsQuery.data.length === 0 && (
            <EmptyState title="No transactions yet" description="Register a buy or sell to see it here." />
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
          )}
        </CardContent>
      </Card>

      <Modal open={isFormOpen} onClose={() => setFormOpen(false)} title="New transaction">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="tx-asset">Asset</Label>
            <Select id="tx-asset" required value={form.assetId} onChange={(e) => setForm((f) => ({ ...f, assetId: e.target.value }))}>
              {assetsQuery.data?.content.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.symbol} — {asset.name}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="tx-type">Type</Label>
            <Select id="tx-type" value={form.type} onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as TransactionType }))}>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="tx-quantity">Quantity</Label>
              <Input
                id="tx-quantity"
                type="number"
                step="any"
                min="0"
                required
                value={form.quantity}
                onChange={(e) => setForm((f) => ({ ...f, quantity: e.target.value }))}
              />
            </div>
            <div>
              <Label htmlFor="tx-price">Price</Label>
              <Input
                id="tx-price"
                type="number"
                step="any"
                min="0"
                required
                value={form.price}
                onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="tx-commission">Commission</Label>
              <Input
                id="tx-commission"
                type="number"
                step="any"
                min="0"
                value={form.commission}
                onChange={(e) => setForm((f) => ({ ...f, commission: e.target.value }))}
              />
            </div>
            <div>
              <Label htmlFor="tx-currency">Currency</Label>
              <Input
                id="tx-currency"
                required
                maxLength={3}
                value={form.currency}
                onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value.toUpperCase() }))}
              />
            </div>
          </div>
          <div>
            <Label htmlFor="tx-date">Date</Label>
            <Input
              id="tx-date"
              type="date"
              required
              max={new Date().toISOString().slice(0, 10)}
              value={form.transactionDate}
              onChange={(e) => setForm((f) => ({ ...f, transactionDate: e.target.value }))}
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
