import { apiRequest } from '../auth/authApi'
import type { CreateShortUrlInput, ShortUrl, ShortUrlPage, ShortUrlStatus, UpdateShortUrlInput } from './types'

export interface ShortUrlFilters {
	query?: string
	status?: ShortUrlStatus
}

export function getShortUrls(page = 0, size = 20, filters: ShortUrlFilters = {}) {
	const params = new URLSearchParams({ page: String(page), size: String(size) })
	if (filters.query) params.set('query', filters.query)
	if (filters.status) params.set('status', filters.status)
	return apiRequest<ShortUrlPage>(`/api/v1/urls?${params.toString()}`)
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

export function updateShortUrl(id: number, input: UpdateShortUrlInput) {
  return apiRequest<ShortUrl>(`/api/v1/urls/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function deleteShortUrl(id: number) {
  return apiRequest<void>(`/api/v1/urls/${id}`, { method: 'DELETE' })
}
