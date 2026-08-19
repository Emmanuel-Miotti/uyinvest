export type Role = 'USER' | 'ADMIN'
export type AssetType = 'STOCK' | 'ETF' | 'BOND' | 'FUND' | 'CRYPTO' | 'CASH'
export type TransactionType = 'BUY' | 'SELL'

export interface User {
  id: string
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface Portfolio {
  id: string
  name: string
  description: string | null
  baseCurrency: string
  createdAt: string
  updatedAt: string
}

export interface PortfolioRequest {
  name: string
  description?: string | null
  baseCurrency: string
}

export interface Asset {
  id: string
  symbol: string
  name: string
  type: AssetType
  currency: string
  sector: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface AssetRequest {
  symbol: string
  name: string
  type: AssetType
  currency: string
  sector?: string | null
  active: boolean
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface Transaction {
  id: string
  asset: Asset
  type: TransactionType
  quantity: number
  price: number
  commission: number
  currency: string
  transactionDate: string
  createdAt: string
}

export interface TransactionRequest {
  assetId: string
  type: TransactionType
  quantity: number
  price: number
  commission?: number
  currency: string
  transactionDate: string
}

export interface Dividend {
  id: string
  asset: Asset
  amount: number
  currency: string
  paymentDate: string
  createdAt: string
}

export interface DividendRequest {
  assetId: string
  amount: number
  currency: string
  paymentDate: string
}

export interface DividendSummary {
  totalThisMonth: number
  totalThisYear: number
  totalHistorical: number
}

export interface Goal {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  currency: string
  targetDate: string | null
  progressPercentage: number
  createdAt: string
  updatedAt: string
}

export interface GoalRequest {
  name: string
  targetAmount: number
  currentAmount?: number
  currency: string
  targetDate?: string | null
}

export interface PortfolioSummary {
  totalInvested: number
  currentValue: number
  profitLoss: number
  profitLossPercentage: number
}

export interface Allocation {
  assetType: AssetType
  currentValue: number
  percentage: number
}

export interface PerformancePoint {
  date: string
  totalInvested: number
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
