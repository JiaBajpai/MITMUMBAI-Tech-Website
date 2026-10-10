import { apiClient, jsonBody, type AuthResult, type AuthUser } from './client'

export const authApi = {
  login: (email: string, password: string) => apiClient<AuthResult>('/auth/login', { method: 'POST', auth: false, body: jsonBody({ email, password }) }),
  refresh: () => apiClient<AuthResult>('/auth/refresh', { method: 'POST', auth: false, retryAuth: false }),
  me: () => apiClient<AuthUser>('/auth/me'),
  logout: () => apiClient<void>('/auth/logout', { method: 'POST', auth: false, retryAuth: false }),
  setupPassword: (token: string, password: string) => apiClient<void>('/auth/password/setup', { method: 'POST', auth: false, retryAuth: false, body: jsonBody({ token, password }) }),
}
