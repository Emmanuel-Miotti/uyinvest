import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { goalsApi } from '@/api/goals'
import type { Goal } from '@/types'
import { Card, CardContent } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input, Label } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { cn, extractErrorMessage, formatCurrency } from '@/lib/utils'

const emptyForm = { name: '', targetAmount: '', currentAmount: '0', currency: 'USD', targetDate: '' }

export function GoalsPage() {
  const queryClient = useQueryClient()
  const goalsQuery = useQuery({ queryKey: ['goals'], queryFn: goalsApi.list })

  const [isFormOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Goal | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [deleteTarget, setDeleteTarget] = useState<Goal | null>(null)

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setFormOpen(true)
  }

  function openEdit(goal: Goal) {
    setEditing(goal)
    setForm({
      name: goal.name,
      targetAmount: String(goal.targetAmount),
      currentAmount: String(goal.currentAmount),
      currency: goal.currency,
      targetDate: goal.targetDate ?? '',
    })
    setFormOpen(true)
  }

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name,
        targetAmount: Number(form.targetAmount),
        currentAmount: Number(form.currentAmount || 0),
        currency: form.currency,
        targetDate: form.targetDate || null,
      }
      return editing ? goalsApi.update(editing.id, payload) : goalsApi.create(payload)
    },
    onSuccess: () => {
      toast.success(editing ? 'Goal updated' : 'Goal created')
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      setFormOpen(false)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not save goal')),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => goalsApi.remove(id),
    onSuccess: () => {
      toast.success('Goal deleted')
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      setDeleteTarget(null)
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not delete goal')),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    saveMutation.mutate()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-800">Financial goals</h1>
        <Button onClick={openCreate}>New goal</Button>
      </div>

      {goalsQuery.isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-36" />
          <Skeleton className="h-36" />
          <Skeleton className="h-36" />
        </div>
      )}

      {goalsQuery.isError && <ErrorState onRetry={() => goalsQuery.refetch()} />}

      {goalsQuery.data && goalsQuery.data.length === 0 && (
        <EmptyState title="No goals yet" description="Set a savings goal to track your progress." action={<Button onClick={openCreate}>New goal</Button>} />
      )}

      {goalsQuery.data && goalsQuery.data.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {goalsQuery.data.map((goal) => (
            <Card key={goal.id}>
              <CardContent>
                <p className="text-sm font-semibold text-slate-800">{goal.name}</p>
                <p className="mt-1 text-xs text-slate-400">
                  {formatCurrency(goal.currentAmount, goal.currency)} of {formatCurrency(goal.targetAmount, goal.currency)}
                </p>
                <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={cn('h-full rounded-full', goal.progressPercentage >= 100 ? 'bg-success-500' : 'bg-brand-500')}
                    style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }}
                  />
                </div>
                <p className="mt-1 text-xs font-medium text-slate-500">{goal.progressPercentage.toFixed(2)}%</p>
                <div className="mt-4 flex gap-2">
                  <Button size="sm" variant="secondary" onClick={() => openEdit(goal)}>
                    Edit
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setDeleteTarget(goal)}>
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Modal open={isFormOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit goal' : 'New goal'}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="goal-name">Name</Label>
            <Input id="goal-name" required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="goal-target">Target amount</Label>
              <Input
                id="goal-target"
                type="number"
                step="any"
                min="0"
                required
                value={form.targetAmount}
                onChange={(e) => setForm((f) => ({ ...f, targetAmount: e.target.value }))}
              />
            </div>
            <div>
              <Label htmlFor="goal-current">Current amount</Label>
              <Input
                id="goal-current"
                type="number"
                step="any"
                min="0"
                value={form.currentAmount}
                onChange={(e) => setForm((f) => ({ ...f, currentAmount: e.target.value }))}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="goal-currency">Currency</Label>
              <Input
                id="goal-currency"
                required
                maxLength={3}
                value={form.currency}
                onChange={(e) => setForm((f) => ({ ...f, currency: e.target.value.toUpperCase() }))}
              />
            </div>
            <div>
              <Label htmlFor="goal-date">Target date</Label>
              <Input
                id="goal-date"
                type="date"
                value={form.targetDate}
                onChange={(e) => setForm((f) => ({ ...f, targetDate: e.target.value }))}
              />
            </div>
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
        title="Delete goal"
        description={`This will permanently delete "${deleteTarget?.name}".`}
        confirmLabel="Delete"
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
