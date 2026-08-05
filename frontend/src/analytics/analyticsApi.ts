import { apiDownload, apiRequest } from '../auth/authApi'
import type { AnalyticsOverview, UrlAnalytics } from './types'

function utcDate(daysAgo: number) {
  const date = new Date()
  date.setUTCDate(date.getUTCDate() - daysAgo)
  return date.toISOString().slice(0, 10)
}

export function getAnalyticsOverview(days: number) {
  const from = utcDate(days - 1)
  const to = utcDate(0)
  return apiRequest<AnalyticsOverview>(`/api/v1/analytics/overview?from=${from}&to=${to}`)
}

export function getUrlAnalytics(shortUrlId: number, days: number) {
  const from = utcDate(days - 1)
  const to = utcDate(0)
  return apiRequest<UrlAnalytics>(
    `/api/v1/urls/${shortUrlId}/analytics?from=${from}&to=${to}`,
  )
}

export function getAnalyticsCsv(days: number) {
  const from = utcDate(days - 1)
  const to = utcDate(0)
  return apiDownload(`/api/v1/analytics/overview/export?from=${from}&to=${to}`)
    .then((blob) => ({ blob, filename: `shortwave-analytics-${from}-to-${to}.csv` }))
}
