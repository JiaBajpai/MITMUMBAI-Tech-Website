import { apiClient, jsonBody } from './client'
import type { AdminAccount, CreateAccountRequest } from './types'

export const accountsApi = {
  list: (search: string, signal?: AbortSignal) => apiClient<AdminAccount[]>(`/admin/users${search ? `?q=${encodeURIComponent(search)}` : ''}`, { signal }),
  create: (request: CreateAccountRequest) => apiClient<AdminAccount>('/admin/users', { method: 'POST', body: jsonBody(request) }),
  update: (id: number, values: Partial<Pick<AdminAccount, 'email' | 'name' | 'program' | 'active'>>) => apiClient<AdminAccount>(`/admin/users/${id}`, { method: 'PATCH', body: jsonBody(values) }),
  setRoles: (id: number, roles: string[]) => apiClient<AdminAccount>(`/admin/users/${id}/roles`, { method: 'PUT', body: jsonBody({ roles }) }),
  assignDomain: (id: number, domainId: number) => apiClient<AdminAccount>(`/admin/users/${id}/domains/${domainId}`, { method: 'PUT' }),
  removeDomain: (id: number, domainId: number) => apiClient<AdminAccount>(`/admin/users/${id}/domains/${domainId}`, { method: 'DELETE' }),
  issuePasswordReset: (id: number) => apiClient<void>(`/admin/users/${id}/password-reset`, { method: 'POST' }),
}
