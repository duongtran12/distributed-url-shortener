import { apiDownload, apiRequest } from '../auth/authApi'

export type ShortUrlAuditAction =
  | 'CREATED'
  | 'UPDATED'
  | 'STATUS_CHANGED'
  | 'PIN_CHANGED'
  | 'DUPLICATED'
  | 'BULK_TAG_CHANGED'
  | 'DELETED'

export interface ShortUrlAuditEvent {
  id: number
  shortUrlId: number | null
  shortCode: string
  action: ShortUrlAuditAction
  details: string
  createdAt: string
}

export interface ShortUrlAuditPage {
  content: ShortUrlAuditEvent[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export function getShortUrlAuditHistory(page = 0, size = 8, action?: ShortUrlAuditAction) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (action) params.set('action', action)
  return apiRequest<ShortUrlAuditPage>(`/api/v1/audit/short-urls?${params}`)
}

export function exportShortUrlAuditHistory(action?: ShortUrlAuditAction) {
  const params = new URLSearchParams()
  if (action) params.set('action', action)
  const query = params.size ? `?${params}` : ''
  return apiDownload(`/api/v1/audit/short-urls/export${query}`)
    .then((blob) => ({ blob, filename: `shortwave-audit-${new Date().toISOString().slice(0, 10)}.csv` }))
}
