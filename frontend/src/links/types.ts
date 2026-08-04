export type ShortUrlStatus = 'ACTIVE' | 'DISABLED' | 'BLOCKED'

export interface ShortUrl {
  id: number
  shortCode: string
  shortUrl: string
  originalUrl: string
  status: ShortUrlStatus
  customAlias: boolean
  expiresAt: string | null
  clickCount: number
  createdAt: string
}

export interface ShortUrlPage {
  content: ShortUrl[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateShortUrlInput {
  originalUrl: string
  customAlias?: string
  expiresAt?: string
}
