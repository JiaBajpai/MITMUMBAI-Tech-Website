import { apiClient } from './client'
import type { GitHubAuthStart, GitHubStatus } from './types'

export const githubApi = {
  status: (signal?: AbortSignal) => apiClient<GitHubStatus>('/me/github/status', { signal }),
  connect: () => apiClient<GitHubAuthStart>('/me/github/connect', { method: 'POST' }),
  disconnect: () => apiClient<void>('/me/github/connect', { method: 'DELETE' }),
}
