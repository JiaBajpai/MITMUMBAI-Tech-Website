import { apiClient } from './client'
import type { DashboardData } from './types'

export const getDashboard = (signal?: AbortSignal) => apiClient<DashboardData>('/me/dashboard', { signal })
