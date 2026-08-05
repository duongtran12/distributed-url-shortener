import type { ApiErrorPayload, UserProfile } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'shortwave.access-token'
export const SESSION_EXPIRED_EVENT = 'shortwave:session-expired'

interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
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
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

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

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
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

export async function register(displayName: string, email: string, password: string): Promise<UserProfile> {
  await apiRequest<UserProfile>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ displayName, email, password }),
  })
  return login(email, password)
}

export function getCurrentUser(): Promise<UserProfile> {
  return apiRequest<UserProfile>('/api/v1/users/me')
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiRequest<void>('/api/v1/users/me/password', {
    method: 'PATCH',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}

export function getAccessToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function clearAccessToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}
