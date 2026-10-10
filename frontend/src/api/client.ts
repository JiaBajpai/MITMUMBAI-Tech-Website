export interface ApiEnvelope<T> {
  timestamp?: string
  status?: number
  success?: boolean
  data: T
  error?: string | null
  message?: string | null
  path?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors?: Record<string, string>

  constructor(message: string, status: number, code?: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

export class NetworkError extends Error {
  constructor(message = 'Unable to reach the server. Check your connection and try again.') {
    super(message)
    this.name = 'NetworkError'
  }
}

export interface AuthTokens {
  accessToken: string
}

const defaultApiBaseUrl = typeof window !== 'undefined' && window.location.hostname === '127.0.0.1'
  ? 'http://127.0.0.1:8080/api/v1'
  : 'http://localhost:8080/api/v1'
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl).replace(/\/$/, '')
let authTokens: AuthTokens | null = null
let onAuthExpired: (() => void) | undefined
let onTokensRefreshed: ((tokens: AuthTokens, user?: AuthUser) => void) | undefined
let refreshRequest: Promise<boolean> | null = null

export interface AuthUser {
  id: number
  name: string
  email: string
  program: string
  roles: string[]
}

export interface AuthResult extends AuthTokens { user: AuthUser }

export function setAuthTokens(tokens: AuthTokens | null) {
  authTokens = tokens
}

export function getAuthTokens() {
  return authTokens
}

export function configureAuthCallbacks(callbacks: {
  onExpired: () => void
  onRefreshed: (tokens: AuthTokens, user?: AuthUser) => void
}) {
  onAuthExpired = callbacks.onExpired
  onTokensRefreshed = callbacks.onRefreshed
}

function isEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  return typeof value === 'object' && value !== null && 'data' in value && 'success' in value
}

async function readBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    const text = await response.text().catch(() => '')
    return text ? { message: text } : undefined
  }
  return response.json().catch(() => undefined)
}

function messageFrom(body: unknown, fallback: string): { message: string; code?: string; fieldErrors?: Record<string, string> } {
  if (typeof body !== 'object' || body === null) return { message: fallback }
  const record = body as Record<string, unknown>
  const message = typeof record.message === 'string' ? record.message : typeof record.error === 'string' ? record.error : fallback
  const nested = typeof record.data === 'object' && record.data !== null ? record.data as Record<string, unknown> : record
  const errors = record.fieldErrors ?? record.errors ?? nested.fieldErrors ?? nested.errors ?? (typeof record.data === 'object' && record.data !== null ? record.data : undefined)
  const fieldErrors = typeof errors === 'object' && errors !== null ? Object.fromEntries(Object.entries(errors).filter((entry): entry is [string, string] => typeof entry[1] === 'string')) : undefined
  const detail = fieldErrors && Object.entries(fieldErrors).map(([field, value]) => `${field}: ${value}`).join('; ')
  return { message: detail ? `${message} (${detail})` : message, code: typeof record.error === 'string' ? record.error : undefined, fieldErrors }
}

async function refreshAuth(): Promise<boolean> {
  if (refreshRequest) return refreshRequest
  if (!authTokens && !onTokensRefreshed) return false

  refreshRequest = (async () => {
    try {
      const response = await fetch(`${apiBaseUrl}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      })
      const body = await readBody(response)
      if (!response.ok) return false
      const result = isEnvelope(body) ? body.data as AuthResult : body as AuthResult
      if (!result?.accessToken) return false
      const newTokens = { accessToken: result.accessToken }
      authTokens = newTokens
      onTokensRefreshed?.(newTokens, result.user)
      return true
    } catch {
      return false
    } finally {
      refreshRequest = null
    }
  })()
  return refreshRequest
}

export type ApiRequestOptions = RequestInit & { auth?: boolean; retryAuth?: boolean }

export async function apiClient<T>(endpoint: string, options: ApiRequestOptions = {}): Promise<T> {
  const { auth = true, retryAuth = true, ...requestOptions } = options
  const url = `${apiBaseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`
  const send = async (token?: string) => {
    try {
      return await fetch(url, {
        ...requestOptions,
        credentials: 'include',
        headers: {
          ...(requestOptions.body && !(requestOptions.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
          ...requestOptions.headers,
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      })
    } catch {
      throw new NetworkError()
    }
  }

  let response = await send(auth ? authTokens?.accessToken : undefined)
  if (auth && retryAuth && response.status === 401 && !endpoint.startsWith('/auth/')) {
    if (await refreshAuth()) response = await send(authTokens?.accessToken)
    else {
      authTokens = null
      onAuthExpired?.()
    }
  }

  const body = await readBody(response)
  if (!response.ok) {
    const info = messageFrom(body, `Request failed (${response.status})`)
    throw new ApiError(info.message, response.status, info.code, info.fieldErrors)
  }
  return (isEnvelope(body) ? body.data : body) as T
}

export function jsonBody(value: unknown) {
  return JSON.stringify(value)
}
