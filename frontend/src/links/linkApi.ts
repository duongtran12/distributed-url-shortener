import { apiRequest } from '../auth/authApi'
import type { CreateShortUrlInput, ShortUrl, ShortUrlPage } from './types'

export function getShortUrls(page = 0, size = 20) {
  return apiRequest<ShortUrlPage>(`/api/v1/urls?page=${page}&size=${size}`)
}

export function createShortUrl(input: CreateShortUrlInput) {
  return apiRequest<ShortUrl>('/api/v1/urls', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateShortUrlStatus(id: number, status: 'ACTIVE' | 'DISABLED') {
  return apiRequest<ShortUrl>(`/api/v1/urls/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function deleteShortUrl(id: number) {
  return apiRequest<void>(`/api/v1/urls/${id}`, { method: 'DELETE' })
}
