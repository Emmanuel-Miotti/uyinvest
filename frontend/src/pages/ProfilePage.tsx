import { useQuery } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { Card, CardContent } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Skeleton } from '@/components/ui/Skeleton'
import { ErrorState } from '@/components/ui/ErrorState'
import { formatDate } from '@/lib/utils'

export function ProfilePage() {
  const meQuery = useQuery({ queryKey: ['me'], queryFn: authApi.me })

  return (
    <div className="max-w-md space-y-6">
      <h1 className="text-xl font-semibold text-slate-800">Profile</h1>

      {meQuery.isLoading && <Skeleton className="h-40" />}
      {meQuery.isError && <ErrorState onRetry={() => meQuery.refetch()} />}

      {meQuery.data && (
        <Card>
          <CardContent className="space-y-3">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Name</p>
              <p className="text-sm text-slate-800">{meQuery.data.name}</p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Email</p>
              <p className="text-sm text-slate-800">{meQuery.data.email}</p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Role</p>
              <Badge tone={meQuery.data.role === 'ADMIN' ? 'brand' : 'neutral'}>{meQuery.data.role}</Badge>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Member since</p>
              <p className="text-sm text-slate-800">{formatDate(meQuery.data.createdAt)}</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
