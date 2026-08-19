import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { portfoliosApi } from '@/api/portfolios'
import type { Portfolio } from '@/types'
import { Card, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input, Label } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { extractErrorMessage } from '@/lib/utils'

const emptyForm = { name: '', description: '', baseCurrency: 'USD' }

export function PortfoliosListPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const portfoliosQuery = useQuery({ queryKey: ['portfolios'], queryFn: portfoliosApi.list })

  const [isFormOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Portfolio | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [deleteTarget, setDeleteTarget] = useState<Portfolio | null>(null)

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormOpen(true)
  }

  function openEdit(portfolio: Portfolio) {
    setEditing(portfolio)
    setForm({ name: portfolio.name, description: portfolio.description ?? '', baseCurrency: portfolio.baseCurrency })
    setFormOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: () =>
      editing ? portfoliosApi.update(editing.id, form) : portfoliosApi.create(form),
    onSuccess: () => {
      toast.success(editing ? 'Portfolio updated' : 'Portfolio created')
      queryClient.invalidateQueries({ queryKey: ['portfolios'] })
      setFormOpen(false)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not save portfolio')),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => portfoliosApi.remove(id),
    onSuccess: () => {
      toast.success('Portfolio deleted')
      queryClient.invalidateQueries({ queryKey: ['portfolios'] })
      setDeleteTarget(null)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not delete portfolio')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    saveMutation.mutate()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-800">Portfolios</h1>
        <Button onClick={openCreate}>New portfolio</Button>
      </div>

      {portfoliosQuery.isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
        </div>
      )}

      {portfoliosQuery.isError && <ErrorState onRetry={() => portfoliosQuery.refetch()} />}

      {portfoliosQuery.data && portfoliosQuery.data.length === 0 && (
        <EmptyState
          title="No portfolios yet"
          description="Create your first portfolio to start tracking investments."
          action={<Button onClick={openCreate}>New portfolio</Button>}
        />
      )}

      {portfoliosQuery.data && portfoliosQuery.data.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {portfoliosQuery.data.map((portfolio) => (
            <Card key={portfolio.id}>
              <CardContent>
                <div className="flex items-start justify-between">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-slate-800">{portfolio.name}</p>
                    <p className="text-xs text-slate-400">{portfolio.baseCurrency}</p>
                  </div>
                </div>
                {portfolio.description && (
                  <p className="mt-2 line-clamp-2 text-xs text-slate-500">{portfolio.description}</p>
                )}
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button size="sm" onClick={() => navigate(`/portfolios/${portfolio.id}`)}>
                    View
                  </Button>
                  <Button size="sm" variant="secondary" onClick={() => openEdit(portfolio)}>
                    Edit
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setDeleteTarget(portfolio)}>
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Modal open={isFormOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit portfolio' : 'New portfolio'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="portfolio-name">Name</Label>
            <Input
              id="portfolio-name"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </div>
          <div>
            <Label htmlFor="portfolio-description">Description</Label>
            <Input
              id="portfolio-description"
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <div>
            <Label htmlFor="portfolio-currency">Base currency</Label>
            <Input
              id="portfolio-currency"
              required
              maxLength={3}
              value={form.baseCurrency}
              onChange={(e) => setForm((f) => ({ ...f, baseCurrency: e.target.value.toUpperCase() }))}
              placeholder="USD"
            />
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

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete portfolio"
        description={`This will permanently delete "${deleteTarget?.name}" and all its transactions and dividends.`}
        confirmLabel="Delete"
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
