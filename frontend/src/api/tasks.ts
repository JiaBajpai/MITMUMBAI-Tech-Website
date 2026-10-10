import { apiClient, jsonBody } from './client'
import type { Task, TaskRequest } from './types'

export const tasksApi = {
  list: (sessionId?: number, signal?: AbortSignal) => apiClient<Task[]>(`/tasks${sessionId ? `?sessionId=${sessionId}` : ''}`, { signal }),
  get: (id: number, signal?: AbortSignal) => apiClient<Task>(`/tasks/${id}`, { signal }),
  create: (request: TaskRequest) => apiClient<Task>('/tasks', { method: 'POST', body: jsonBody(request) }),
  update: (id: number, request: Partial<TaskRequest>) => apiClient<Task>(`/tasks/${id}`, { method: 'PATCH', body: jsonBody(request) }),
  complete: (id: number, evidence: { repoUrl: string; commitSha?: string; notes?: string }) => apiClient<Task>(`/tasks/${id}/complete`, { method: 'POST', body: jsonBody(evidence) }),
  verify: (id: number) => apiClient<Task>(`/tasks/${id}/verify`, { method: 'POST' }),
}
