import { apiClient, jsonBody } from './client'
import type { UserProfile } from './types'

export const profileApi = {
  get: (signal?: AbortSignal) => apiClient<UserProfile>('/me/profile', { signal }),
  update: (values: Pick<UserProfile, 'bio' | 'avatarUrl' | 'githubUrl' | 'linkedinUrl' | 'phone'>) => apiClient<UserProfile>('/me/profile', { method: 'PATCH', body: jsonBody(values) }),
}
