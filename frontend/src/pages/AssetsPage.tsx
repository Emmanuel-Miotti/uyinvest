import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { assetsApi } from '@/api/assets'
import type { Asset, AssetType } from '@/types'
import { useAuthStore } from '@/stores/authStore'
import { Card, CardContent } from '@/components/ui/Card'
import { TableContainer } from '@/components/ui/Table'
import { Button } from '@/components/ui/Button'
import { Input, Label, Select } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { extractErrorMessage } from '@/lib/utils'

const ASSET_TYPES: AssetType[] = ['STOCK', 'ETF', 'BOND', 'FUND', 'CRYPTO', 'CASH']

const emptyForm = { symbol: '', name: '', type: 'STOCK' as AssetType, currency: 'USD', sector: '', active: true }

export function AssetsPage() {
  const user = useAuthStore((state) => state.user)
  const queryClient = useQueryClient()

  const [search, setSearch] = useState('')
  const [type, setType] = useState<AssetType | ''>('')
  const [page, setPage] = useState(0)
  const [isFormOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Asset | null>(null)
  const [form, setForm] = useState(emptyForm)

  const assetsQuery = useQuery({
    queryKey: ['assets', search, type, page],
    queryFn: () => assetsApi.search({ search: search || undefined, type: type || undefined, page, size: 10 }),
  })

  const saveMutation = useMutation({
    mutationFn: () => (editing ? assetsApi.update(editing.id, form) : assetsApi.create(form)),
    onSuccess: () => {
      toast.success(editing ? 'Asset updated' : 'Asset created')
      queryClient.invalidateQueries({ queryKey: ['assets'] })
      setFormOpen(false)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not save asset')),
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormOpen(true)
  }

  function openEdit(asset: Asset) {
    setEditing(asset)
    setForm({ symbol: asset.symbol, name: asset.name, type: asset.type, currency: asset.currency, sector: asset.sector ?? '', active: asset.active })
    setFormOpen(true)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    saveMutation.mutate()
  }

  const isAdmin = user?.role === 'ADMIN'

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-slate-800">Assets</h1>
        {isAdmin && <Button onClick={openCreate}>New asset</Button>}
      </div>

      <div className="flex flex-wrap gap-3">
        <Input
          placeholder="Search by symbol or name…"
          value={search}
          onChange={(e) => {
            setPage(0)
            setSearch(e.target.value)
          }}
          className="max-w-xs"
        />
        <Select
          value={type}
          onChange={(e) => {
            setPage(0)
            setType(e.target.value as AssetType | '')
          }}
          className="max-w-[160px]"
        >
          <option value="">All types</option>
          {ASSET_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </Select>
      </div>

      <Card>
        <CardContent>
          {assetsQuery.isLoading && <Skeleton className="h-64" />}
          {assetsQuery.isError && <ErrorState onRetry={() => assetsQuery.refetch()} />}
          {assetsQuery.data && assetsQuery.data.content.length === 0 && (
            <EmptyState title="No assets found" description="Try a different search or filter." />
          )}
          {assetsQuery.data && assetsQuery.data.content.length > 0 && (
            <>
              <TableContainer>
                <table className="w-full min-w-[560px] text-sm">
                  <thead>
                    <tr className="border-b border-border-subtle text-left text-xs uppercase text-slate-400">
                      <th className="pb-2 font-medium">Symbol</th>
                      <th className="pb-2 font-medium">Name</th>
                      <th className="pb-2 font-medium">Type</th>
                      <th className="pb-2 font-medium">Currency</th>
                      <th className="pb-2 font-medium">Status</th>
                      {isAdmin && <th className="pb-2 font-medium" />}
                    </tr>
                  </thead>
                  <tbody>
                    {assetsQuery.data.content.map((asset) => (
                      <tr key={asset.id} className="border-b border-border-subtle last:border-0">
                        <td className="py-2 font-medium text-slate-700">{asset.symbol}</td>
                        <td className="py-2 text-slate-600">{asset.name}</td>
                        <td className="py-2 text-slate-600">{asset.type}</td>
                        <td className="py-2 text-slate-600">{asset.currency}</td>
                        <td className="py-2">
                          <Badge tone={asset.active ? 'success' : 'neutral'}>{asset.active ? 'Active' : 'Inactive'}</Badge>
                        </td>
                        {isAdmin && (
                          <td className="py-2 text-right">
                            <Button size="sm" variant="secondary" onClick={() => openEdit(asset)}>
                              Edit
                            </Button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </TableContainer>

              <div className="mt-4 flex items-center justify-between text-xs text-slate-500">
                <span>
                  Page {assetsQuery.data.number + 1} of {Math.max(assetsQuery.data.totalPages, 1)}
                </span>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={assetsQuery.data.number + 1 >= assetsQuery.data.totalPages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Modal open={isFormOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit asset' : 'New asset'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="asset-symbol">Symbol</Label>
            <Input
              id="asset-symbol"
              required
              value={form.symbol}
              onChange={(e) => setForm((f) => ({ ...f, symbol: e.target.value.toUpperCase() }))}
              placeholder="AAPL"
            />
          </div>
          <div>
            <Label htmlFor="asset-name">Name</Label>
            <Input id="asset-name" required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </div>
          <div>
            <Label htmlFor="asset-type">Type</Label>
            <Select id="asset-type" value={form.type} onChange={(e) => setForm((f) => ({ ...f, type: e.target.value as AssetType }))}>
              {ASSET_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="asset-currency">Currency</Label>
            <Input
              id="asset-currency"
              required
              maxLength={3}
              value={form.currency}
              onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value.toUpperCase() }))}
            />
          </div>
          <div>
            <Label htmlFor="asset-sector">Sector</Label>
            <Input id="asset-sector" value={form.sector} onChange={(e) => setForm((f) => ({ ...f, sector: e.target.value }))} />
          </div>
          <div className="flex items-center gap-2">
            <input
              id="asset-active"
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
              className="h-4 w-4 rounded border-slate-300"
            />
            <Label htmlFor="asset-active" className="mb-0">
              Active (tradeable)
            </Label>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setFormOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={saveMutation.isPending}>
              Save
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
