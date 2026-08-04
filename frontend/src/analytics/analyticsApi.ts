import { apiRequest } from '../auth/authApi'
import type { AnalyticsOverview } from './types'

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
