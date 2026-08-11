import type { ApiErrorPayload, UserProfile } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'shortwave.access-token'
export const SESSION_EXPIRED_EVENT = 'shortwave:session-expired'
let refreshPromise: Promise<string> | null = null

interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface ActiveSession {
  id: number
  userAgent: string
  createdAt: string
  lastUsedAt: string
  expiresAt: string
  current: boolean
}

export class ApiClientError extends Error {
  readonly code?: string
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(payload: ApiErrorPayload, status: number) {
    super(payload.message || `Request failed with status ${status}`)
    this.name = 'ApiClientError'
    this.code = payload.code
    this.status = status
    this.fieldErrors = Object.fromEntries(
      (payload.fieldErrors || []).map((error) => [error.field.split('.').at(-1) || error.field, error.message]),
    )
  }
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await authorizedRequest(path, options)

  if (response.status === 204 || !response.headers.get('content-type')?.includes('application/json')) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function apiDownload(path: string): Promise<Blob> {
  const response = await authorizedRequest(path)
  return response.blob()
}

async function authorizedRequest(path: string, options: RequestInit = {}): Promise<Response> {
  const token = getAccessToken()
  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401 && token && path !== '/api/v1/auth/refresh') {
    try {
      const refreshedToken = await refreshAccessToken()
      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        credentials: 'include',
        headers: {
          ...(options.body ? { 'Content-Type': 'application/json' } : {}),
          Authorization: `Bearer ${refreshedToken}`,
          ...options.headers,
        },
      })
    } catch { /* The original 401 is handled below. */ }
  }

  if (!response.ok) {
    let payload: ApiErrorPayload
    try {
      payload = await response.json() as ApiErrorPayload
    } catch {
      payload = { message: 'The server returned an unreadable response' }
    }
    if (response.status === 401 && token) {
      clearAccessToken()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      payload = {
        ...payload,
        message: 'Your session has expired. Please sign in again.',
      }
    }
    throw new ApiClientError(payload, response.status)
  }

  return response
}

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Session refresh failed')
        const session = await response.json() as LoginResponse
        sessionStorage.setItem(TOKEN_KEY, session.accessToken)
        return session.accessToken
      })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

export async function login(email: string, password: string): Promise<UserProfile> {
  const result = await apiRequest<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  sessionStorage.setItem(TOKEN_KEY, result.accessToken)
  try {
    return await getCurrentUser()
  } catch (error) {
    clearAccessToken()
    throw error
  }
}

export async function register(displayName: string, email: string, password: string): Promise<void> {
  await apiRequest<UserProfile>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ displayName, email, password }),
  })
}

export function requestEmailVerification(email: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/email-verification/request', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function confirmEmailVerification(token: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/email-verification/confirm', {
    method: 'POST',
    body: JSON.stringify({ token }),
  })
}

export function getCurrentUser(): Promise<UserProfile> {
  return apiRequest<UserProfile>('/api/v1/users/me')
}

export async function restoreSession(): Promise<UserProfile | null> {
  if (!getAccessToken()) {
    try {
      await refreshAccessToken()
    } catch {
      clearAccessToken()
      return null
    }
  }
  try {
    return await getCurrentUser()
  } catch {
    clearAccessToken()
    return null
  }
}

export async function logout(): Promise<void> {
  clearAccessToken()
  try {
    await fetch(`${API_BASE_URL}/api/v1/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    })
  } catch {
    // Local logout still succeeds when the backend is unavailable.
  }
}

export function getActiveSessions(): Promise<ActiveSession[]> {
  return apiRequest<ActiveSession[]>('/api/v1/auth/sessions')
}

export async function revokeSession(id: number, current: boolean): Promise<void> {
  await apiRequest<void>(`/api/v1/auth/sessions/${id}`, { method: 'DELETE' })
  if (current) clearAccessToken()
}

export function revokeOtherSessions(): Promise<{ revoked: number }> {
  return apiRequest<{ revoked: number }>('/api/v1/auth/sessions/others', { method: 'DELETE' })
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiRequest<void>('/api/v1/users/me/password', {
    method: 'PATCH',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}

export function updateProfile(displayName: string): Promise<UserProfile> {
  return apiRequest<UserProfile>('/api/v1/users/me', {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  })
}

export async function deleteAccount(currentPassword: string): Promise<void> {
  await apiRequest<void>('/api/v1/users/me', {
    method: 'DELETE',
    body: JSON.stringify({ currentPassword }),
  })
  clearAccessToken()
  try {
    await fetch(`${API_BASE_URL}/api/v1/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    })
  } catch {
    // The account is already deleted; cookie cleanup can safely fail offline.
  }
}

export function requestPasswordReset(email: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function confirmPasswordReset(token: string, newPassword: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/password-reset/confirm', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  })
}

export function getAccessToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function clearAccessToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}
