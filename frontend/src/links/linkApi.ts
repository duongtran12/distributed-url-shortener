import { apiDownload, apiRequest } from '../auth/authApi'
import type { CreateShortUrlInput, ShortUrl, ShortUrlPage, ShortUrlStatus, UpdateShortUrlInput } from './types'

export type ShortUrlSort = 'NEWEST' | 'OLDEST' | 'MOST_CLICKED'
export type BulkShortUrlAction = 'ENABLE' | 'DISABLE' | 'DELETE'

export interface ShortUrlFilters {
	query?: string
	tag?: string
	status?: ShortUrlStatus
	sort?: ShortUrlSort
}

export function getShortUrls(page = 0, size = 20, filters: ShortUrlFilters = {}) {
	const params = new URLSearchParams({ page: String(page), size: String(size) })
	if (filters.query) params.set('query', filters.query)
	if (filters.tag) params.set('tag', filters.tag)
	if (filters.status) params.set('status', filters.status)
	if (filters.sort) params.set('sort', filters.sort)
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

export function updateShortUrlPin(id: number, pinned: boolean) {
  return apiRequest<ShortUrl>(`/api/v1/urls/${id}/pin`, {
    method: 'PATCH',
    body: JSON.stringify({ pinned }),
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

export function duplicateShortUrl(id: number) {
  return apiRequest<ShortUrl>(`/api/v1/urls/${id}/duplicate`, { method: 'POST' })
}

export function bulkUpdateShortUrls(ids: number[], action: BulkShortUrlAction) {
  return apiRequest<{ action: BulkShortUrlAction, affected: number }>('/api/v1/urls/bulk', {
    method: 'POST',
    body: JSON.stringify({ ids, action }),
  })
}

export function getShortUrlQrCode(id: number) {
  return apiDownload(`/api/v1/urls/${id}/qr`)
}
