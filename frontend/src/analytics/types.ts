export interface DailyClickCount {
  date: string
  clicks: number
}

export interface TopUrlAnalytics {
  shortUrlId: number
  shortCode: string
  originalUrl: string
  clicks: number
  uniqueVisitors: number
}

export interface AnalyticsOverview {
  totalUrls: number
  activeUrls: number
  lifetimeClicks: number
  periodClicks: number
  periodUniqueVisitors: number
  from: string
  to: string
  dailyClicks: DailyClickCount[]
  topUrls: TopUrlAnalytics[]
}

export interface CategoryClickCount {
  category: string
  clicks: number
}

export interface UrlAnalytics {
  shortUrlId: number
  shortCode: string
  lifetimeClicks: number
  lifetimeUniqueVisitors: number
  periodClicks: number
  periodUniqueVisitors: number
  from: string
  to: string
  dailyClicks: DailyClickCount[]
  browsers: CategoryClickCount[]
  operatingSystems: CategoryClickCount[]
  devices: CategoryClickCount[]
  referrers: CategoryClickCount[]
}
