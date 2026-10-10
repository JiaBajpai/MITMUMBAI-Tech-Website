import { apiClient, jsonBody } from './client'
import type { Attendance, Session } from './types'

export interface SessionFilters { domainId?: number; program?: string; type?: string; date?: string }
function query(values: SessionFilters) {
  const params = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)) })
  return params.size ? `?${params}` : ''
}

// Kept as a compatibility alias for the unused legacy public section; this still calls the real authenticated API.
export const getSessions = () => sessionsApi.list()

export const sessionsApi = {
  list: (filters: SessionFilters = {}, signal?: AbortSignal) => apiClient<Session[]>(`/sessions${query(filters)}`, { signal }),
  get: (id: number, signal?: AbortSignal) => apiClient<Session>(`/sessions/${id}`, { signal }),
  mine: (signal?: AbortSignal) => apiClient<Attendance[]>('/me/attendance', { signal }),
  attendance: (id: number, signal?: AbortSignal) => apiClient<Attendance[]>(`/sessions/${id}/attendance`, { signal }),
  markAttendance: (sessionId: number, userId: number, status: Attendance['status']) => apiClient<Attendance>(`/sessions/${sessionId}/attendance`, { method: 'POST', body: jsonBody({ userId, status }) }),
}
