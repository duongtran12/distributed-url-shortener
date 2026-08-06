export type ShortUrlStatus = 'ACTIVE' | 'DISABLED' | 'BLOCKED'

export interface ShortUrl {
  id: number
  shortCode: string
  shortUrl: string
  originalUrl: string
  title: string | null
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
  title?: string
  customAlias?: string
  expiresAt?: string
}

export interface UpdateShortUrlInput {
  originalUrl: string
  title: string | null
  expiresAt: string | null
}
