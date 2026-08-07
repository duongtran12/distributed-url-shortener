import { apiDownload, apiRequest } from '../auth/authApi'
import type { CreateShortUrlInput, ShortUrl, ShortUrlPage, ShortUrlStatus, UpdateShortUrlInput } from './types'

export type ShortUrlSort = 'NEWEST' | 'OLDEST' | 'MOST_CLICKED'
export type BulkShortUrlAction = 'ENABLE' | 'DISABLE' | 'DELETE' | 'SET_TAG' | 'CLEAR_TAG'

export interface ShortUrlFilters {
	query?: string
	tag?: string
	status?: ShortUrlStatus
	pinned?: boolean
	sort?: ShortUrlSort
}

function filterParams(filters: ShortUrlFilters) {
	const params = new URLSearchParams()
	if (filters.query) params.set('query', filters.query)
	if (filters.tag) params.set('tag', filters.tag)
	if (filters.status) params.set('status', filters.status)
	if (filters.pinned !== undefined) params.set('pinned', String(filters.pinned))
	if (filters.sort) params.set('sort', filters.sort)
	return params
}

export function getShortUrls(page = 0, size = 20, filters: ShortUrlFilters = {}) {
	const params = filterParams(filters)
	params.set('page', String(page))
	params.set('size', String(size))
	return apiRequest<ShortUrlPage>(`/api/v1/urls?${params.toString()}`)
}

export function exportShortUrls(filters: ShortUrlFilters = {}) {
	const params = filterParams(filters)
	return apiDownload(`/api/v1/urls/export?${params.toString()}`)
		.then((blob) => ({ blob, filename: `shortwave-links-${new Date().toISOString().slice(0, 10)}.csv` }))
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

export function bulkUpdateShortUrls(ids: number[], action: BulkShortUrlAction, tag?: string) {
  return apiRequest<{ action: BulkShortUrlAction, affected: number }>('/api/v1/urls/bulk', {
    method: 'POST',
    body: JSON.stringify({ ids, action, ...(tag ? { tag } : {}) }),
  })
}

export function getShortUrlQrCode(id: number) {
  return apiDownload(`/api/v1/urls/${id}/qr`)
}
