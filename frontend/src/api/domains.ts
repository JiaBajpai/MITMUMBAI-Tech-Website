import { apiClient } from './client'
import type { Domain } from './types'

export const domainsApi = {
  list: (signal?: AbortSignal) => apiClient<Domain[]>('/domains', { signal }),
  get: (id: number, signal?: AbortSignal) => apiClient<Domain>(`/domains/${id}`, { signal }),
}
