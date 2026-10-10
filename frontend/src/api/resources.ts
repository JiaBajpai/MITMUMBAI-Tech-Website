import { apiClient } from './client'
import type { Resource } from './types'

export interface ResourceFilters { domainId?: number; type?: string; difficulty?: string; topic?: string }
function query(filters: ResourceFilters) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)) })
  return params.size ? `?${params}` : ''
}

export const resourcesApi = {
  list: (filters: ResourceFilters = {}, signal?: AbortSignal) => apiClient<Resource[]>(`/resources${query(filters)}`, { signal }),
  get: (id: number, signal?: AbortSignal) => apiClient<Resource>(`/resources/${id}`, { signal }),
}
