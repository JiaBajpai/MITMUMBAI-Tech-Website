import { apiClient, jsonBody } from './client'
import type { GitHubContribution, GitHubRepository, Project, ProjectMembership, ProjectRequest } from './types'

export interface ProjectFilters { domainId?: number; status?: string; program?: string; q?: string }
function query(filters: ProjectFilters) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)) })
  return params.size ? `?${params}` : ''
}

export const projectsApi = {
  list: (filters: ProjectFilters = {}, signal?: AbortSignal) => apiClient<Project[]>(`/projects${query(filters)}`, { signal }),
  get: (id: number, signal?: AbortSignal) => apiClient<Project>(`/projects/${id}`, { signal }),
  members: (id: number, signal?: AbortSignal) => apiClient<ProjectMembership[]>(`/projects/${id}/members`, { signal }),
  create: (request: ProjectRequest) => apiClient<Project>('/projects', { method: 'POST', body: jsonBody(request) }),
  update: (id: number, request: Partial<ProjectRequest>) => apiClient<Project>(`/projects/${id}`, { method: 'PATCH', body: jsonBody(request) }),
  review: (id: number, decision: string, comment?: string) => apiClient<Project>(`/projects/${id}/review`, { method: 'POST', body: jsonBody({ decision, comment }) }),
  join: (id: number) => apiClient<ProjectMembership>(`/projects/${id}/join`, { method: 'POST' }),
  approve: (id: number, userId: number) => apiClient<ProjectMembership>(`/projects/${id}/members/${userId}/approve`, { method: 'POST' }),
  reject: (id: number, userId: number) => apiClient<ProjectMembership>(`/projects/${id}/members/${userId}/reject`, { method: 'POST' }),
  removeMember: (id: number, userId: number) => apiClient<void>(`/projects/${id}/members/${userId}`, { method: 'DELETE' }),
  leave: (id: number) => apiClient<void>(`/projects/${id}/leave`, { method: 'DELETE' }),
  linkedRepository: (id: number, signal?: AbortSignal) => apiClient<GitHubRepository>(`/projects/${id}/github/repository`, { signal }),
  accessibleRepositories: (signal?: AbortSignal) => apiClient<GitHubRepository[]>('/me/github/repositories', { signal }),
  linkRepository: (id: number, githubRepoId: number) => apiClient<GitHubRepository>(`/projects/${id}/github/repository`, { method: 'POST', body: jsonBody({ githubRepoId }) }),
  unlinkRepository: (id: number) => apiClient<void>(`/projects/${id}/github/repository`, { method: 'DELETE' }),
  contributions: (id: number, signal?: AbortSignal) => apiClient<GitHubContribution[]>(`/projects/${id}/github/contributions`, { signal }),
  verifyContribution: (projectId: number, contributionId: number) => apiClient<GitHubContribution>(`/projects/${projectId}/github/contributions/${contributionId}/verify`, { method: 'POST' }),
}
